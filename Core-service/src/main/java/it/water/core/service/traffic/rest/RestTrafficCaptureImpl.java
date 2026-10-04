/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.core.service.traffic.rest;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.RestTrafficCapture;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.core.service.traffic.TrafficCorrelationContext;
import it.water.core.service.traffic.WaterTrafficCallRecord;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author Aristide Cittadino.
 * Runtime-agnostic implementation of {@link RestTrafficCapture} (F3): everything the CXF and the
 * Spring adapters would otherwise have to duplicate lives here - enablement, correlation scope,
 * path normalization, path exclusions, outcome derivation and record building.
 * <p>
 * <b>ADR-11 short-circuit.</b> The {@link TrafficReporter} is resolved OPTIONALLY from the registry;
 * with no reporter installed, or with reporting disabled, {@link #isEnabled()} is false and adapters
 * skip their own work too.
 * <p>
 * <b>Outcome.</b> Unlike the S2S interceptors (see R1: the after-hook never runs on the exception
 * path), a REST adapter always gets a chance to report - CXF through its response filter, Spring
 * through {@code afterCompletion}, which runs on the error path as well. REST records therefore
 * carry a real {@link Outcome}: ERROR when the call threw or answered with a 4xx/5xx status.
 * <p>
 * <b>ADR-4 privacy.</b> No payload is ever captured and no query string is retained (the path is
 * normalized to its route-ish form only). Error-message truncation, identity and tenant enrichment
 * are NOT done here: they are cross-cutting policies applied to every source alike by
 * {@code TrafficRecordPolicy}, behind the reporter.
 */
@FrameworkComponent(services = {RestTrafficCapture.class})
public class RestTrafficCaptureImpl implements RestTrafficCapture {

    private static final Logger log = LoggerFactory.getLogger(RestTrafficCaptureImpl.class);

    private static final String PROP_REST_ENABLED = "water.traffic.rest.enabled";
    private static final String PROP_REST_EXCLUDE = "water.traffic.rest.exclude";
    private static final String WATER_BASE_CONTEXT = "/water";
    private static final int HTTP_ERROR_STATUS_THRESHOLD = 400;

    /**
     * Injected ONCE at startup: this component is not a Water {@code Service}, so it is registered
     * un-proxied and the lazy per-invocation field injection never runs on it - a plain
     * {@code @Inject} would silently stay null.
     */
    @Inject(injectOnceAtStartup = true)
    @Setter
    @Getter
    private ComponentRegistry componentsRegistry;

    @Override
    public boolean isEnabled() {
        try {
            TrafficReporter reporter = resolveReporter();
            if (reporter == null || !reporter.isEnabled()) {
                return false;
            }
            ApplicationProperties props = resolveApplicationProperties();
            return props == null || Boolean.parseBoolean(props.getPropertyOrDefault(PROP_REST_ENABLED, "true"));
        } catch (Exception e) {
            log.warn("REST traffic capture enablement check failed, disabling capture: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void requestStarted() {
        try {
            TrafficCorrelationContext.open();
        } catch (Exception e) {
            log.warn("Could not open the REST correlation scope: {}", e.getMessage());
        }
    }

    @Override
    public void captureRestCall(String httpMethod, String path, Integer statusCode, String clientIp,
                                long durationMillis, Throwable error) {
        try {
            TrafficReporter reporter = resolveReporter();
            if (reporter == null || !reporter.isEnabled()) {
                return;
            }
            ApplicationProperties props = resolveApplicationProperties();
            if (!restCaptureEnabled(props)) {
                return;
            }
            String normalizedPath = normalizePath(path);
            if (isExcluded(props, normalizedPath)) {
                return;
            }
            reporter.reportCall(buildRecord(httpMethod, normalizedPath, statusCode, clientIp, durationMillis, error));
        } catch (Exception e) {
            // telemetry must never break the request being served
            log.warn("REST traffic capture failed, dropping record: {}", e.getMessage(), e);
        } finally {
            // the scope MUST die with the request: this thread goes back to a pool and must not
            // correlate the next, unrelated request with this one
            TrafficCorrelationContext.close();
        }
    }

    private WaterTrafficCallRecord buildRecord(String httpMethod, String path, Integer statusCode,
                                               String clientIp, long durationMillis, Throwable error) {
        boolean failed = error != null || (statusCode != null && statusCode >= HTTP_ERROR_STATUS_THRESHOLD);
        return WaterTrafficCallRecord.builder()
                .recordId(UUID.randomUUID().toString())
                .recordType(RecordType.REST)
                .timestamp(Instant.now())
                .outcome(failed ? Outcome.ERROR : Outcome.SUCCESS)
                .durationMillis(durationMillis)
                .schemaVersion(1)
                .operation(operationOf(httpMethod, path))
                .httpMethod(httpMethod)
                .path(path)
                .statusCode(statusCode)
                .clientIp(clientIp)
                .errorType(error != null ? error.getClass().getName() : null)
                .errorMessage(error != null ? error.getMessage() : null)
                .build();
    }

    /**
     * Human-readable operation label, the REST counterpart of the S2S method name: {@code GET /roles}.
     */
    private String operationOf(String httpMethod, String path) {
        if (httpMethod == null) {
            return path;
        }
        return path != null ? httpMethod + " " + path : httpMethod;
    }

    /**
     * Strips the {@code /water} base context (CXF prepends it to every resource, and the Spring
     * request URI carries it too) and any query string, and guarantees a leading slash - so that the
     * same endpoint yields the same path under either runtime.
     */
    String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.equals(WATER_BASE_CONTEXT)) {
            return "/";
        }
        if (normalized.startsWith(WATER_BASE_CONTEXT + "/")) {
            normalized = normalized.substring(WATER_BASE_CONTEXT.length());
        }
        // collapse a trailing slash, so /roles and /roles/ are the same endpoint
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean restCaptureEnabled(ApplicationProperties props) {
        return props == null || Boolean.parseBoolean(props.getPropertyOrDefault(PROP_REST_ENABLED, "true"));
    }

    /**
     * Path exclusions are PREFIX matches on the normalized path, so {@code /health} also excludes
     * {@code /health/db}. Meant for high-frequency, low-value endpoints (health, metrics, probes).
     */
    private boolean isExcluded(ApplicationProperties props, String normalizedPath) {
        if (normalizedPath == null) {
            return false;
        }
        for (String excluded : exclusions(props)) {
            if (normalizedPath.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    private List<String> exclusions(ApplicationProperties props) {
        if (props == null) {
            return Collections.emptyList();
        }
        String raw = props.getPropertyOrDefault(PROP_REST_EXCLUDE, "");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Resolves the {@link TrafficReporter} optionally from the registry (ADR-11 short-circuit).
     */
    private TrafficReporter resolveReporter() {
        if (getComponentsRegistry() == null) {
            return null;
        }
        try {
            List<TrafficReporter> reporters = getComponentsRegistry().findComponents(TrafficReporter.class, null);
            if (reporters == null || reporters.isEmpty()) {
                return null;
            }
            return reporters.get(0);
        } catch (NoComponentRegistryFoundException e) {
            log.debug("No TrafficReporter registered, REST capture disabled");
            return null;
        }
    }

    private ApplicationProperties resolveApplicationProperties() {
        if (getComponentsRegistry() == null) {
            return null;
        }
        try {
            return getComponentsRegistry().findComponent(ApplicationProperties.class, null);
        } catch (NoComponentRegistryFoundException e) {
            log.debug("No ApplicationProperties registered, falling back to REST capture defaults");
            return null;
        }
    }
}
