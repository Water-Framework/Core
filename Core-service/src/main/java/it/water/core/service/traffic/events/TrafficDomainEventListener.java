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

package it.water.core.service.traffic.events;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.entity.events.PostCrudDetailedEvent;
import it.water.core.api.entity.events.PostCrudEvent;
import it.water.core.api.entity.events.PostRemoveEvent;
import it.water.core.api.entity.events.PostSaveEvent;
import it.water.core.api.entity.events.PostUpdateDetailedEvent;
import it.water.core.api.entity.events.PostUpdateEvent;
import it.water.core.api.entity.events.PreCrudDetailedEvent;
import it.water.core.api.entity.events.PreCrudEvent;
import it.water.core.api.entity.events.PreRemoveEvent;
import it.water.core.api.entity.events.PreSaveEvent;
import it.water.core.api.entity.events.PreUpdateDetailedEvent;
import it.water.core.api.entity.events.PreUpdateEvent;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.Resource;
import it.water.core.api.model.events.ApplicationEventListener;
import it.water.core.api.model.events.Event;
import it.water.core.api.model.events.PostDetailedEvent;
import it.water.core.api.model.events.PostEvent;
import it.water.core.api.model.events.PreDetailedEvent;
import it.water.core.api.model.events.PreEvent;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.events.TrafficEvent;
import it.water.core.api.traffic.model.ChangeOperation;
import it.water.core.api.traffic.model.ChangePhase;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.api.traffic.model.TrafficRecord;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import it.water.core.service.traffic.WaterTrafficDomainEventRecord;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @Author Aristide Cittadino.
 * Domain-event source of the traffic pipeline (F2): an {@link ApplicationEventListener} that
 * observes the Water event bus, normalizes every observed {@link Event} into a
 * {@link WaterTrafficDomainEventRecord} and hands it to the {@link TrafficReporter}.
 * <p>
 * <b>Loop guard (mandatory).</b> The in-memory egress dispatches each published traffic event to
 * EVERY registered {@code ApplicationEventListener}, this one included. Without a guard each
 * traffic record would be re-observed and re-reported, feeding the pipeline with its own output
 * forever. Any resource that is already a {@link TrafficRecord}, or any event that is already a
 * {@link TrafficEvent}, is therefore ignored up-front.
 * <p>
 * <b>ADR-11 short-circuit.</b> The reporter is resolved OPTIONALLY from the registry: when it is
 * absent or disabled nothing is built and nothing is reported.
 * <p>
 * <b>ADR-2 opt-in whitelist.</b> Capture is additionally gated by
 * {@code water.traffic.events.enabled} and by {@code water.traffic.events.mode}: in the default
 * {@code opt-in} mode only the {@code resourceType}s listed in
 * {@code water.traffic.events.whitelist} are captured (an empty whitelist captures nothing);
 * {@code opt-out} captures every observed event.
 * <p>
 * <b>ADR-4 privacy.</b> Only lightweight references ({@code resourceType#id}) are put on the
 * record for detailed events - never the resource payload. The resource itself travels as the
 * in-process event payload only, which never crosses a broker.
 */
@FrameworkComponent(services = {ApplicationEventListener.class})
public class TrafficDomainEventListener implements ApplicationEventListener<Resource> {

    private static final Logger log = LoggerFactory.getLogger(TrafficDomainEventListener.class);

    private static final String PROP_EVENTS_ENABLED = "water.traffic.events.enabled";
    private static final String PROP_EVENTS_MODE = "water.traffic.events.mode";
    private static final String PROP_EVENTS_WHITELIST = "water.traffic.events.whitelist";
    private static final String MODE_OPT_OUT = "opt-out";

    /**
     * Injected ONCE at startup: this listener is a framework component but NOT a Water
     * {@code Service}, so it is registered un-proxied and the lazy, per-invocation field
     * injection never runs on it - a plain {@code @Inject} would silently stay null. Same
     * rationale documented on {@code TrafficS2SInterceptor}.
     */
    @Inject(injectOnceAtStartup = true)
    @Setter
    @Getter
    private ComponentRegistry componentsRegistry;

    /**
     * Resolved LAZILY (on first use) and then kept, because the property gates now run BEFORE any
     * registry lookup: resolving this runtime-wide singleton from the registry on every captured
     * event would put back exactly the cost the gate ordering removes.
     * <p>
     * Deliberately NOT an {@code @Inject(injectOnceAtStartup = true)} field: in OSGi this listener
     * can be activated before {@code ApplicationProperties} is registered, and a startup injection
     * would then fail the activator and bring the whole container down.
     */
    @Setter
    @Getter
    private volatile ApplicationProperties applicationProperties;

    @Override
    public void consumerEvent(Resource resource, Event event) {
        // a plain event carries a single resource image: it is already fully described by
        // resourceType + resourceId, so both refs stay null (they exist to disambiguate the two
        // images of a DETAILED event, and labelling a PRE image as "after" would be misleading)
        capture(resource, null, null, event);
    }

    @Override
    public void consumerDetailedEvent(Resource beforeResource, Resource afterResource, Event event) {
        // the "current" resource is the after image when present, the before image otherwise
        Resource subject = afterResource != null ? afterResource : beforeResource;
        capture(subject, beforeResource, afterResource, event);
    }

    /**
     * Single capture path shared by the plain and the detailed callbacks. Fail-safe by contract:
     * telemetry must never break the emitter, so any unexpected error is swallowed with a warning.
     */
    private void capture(Resource subject, Resource beforeResource, Resource afterResource, Event event) {
        try {
            // loop guard FIRST: cheapest check, and the only thing standing between this listener
            // and an endless self-feeding loop through the in-memory publisher
            if (subject instanceof TrafficRecord || event instanceof TrafficEvent) {
                return;
            }
            // property gates BEFORE any registry lookup: this listener sits on the CRUD hot path
            // (every persist/update/remove of every entity walks through it), and a registry
            // lookup is orders of magnitude more expensive than a property read - in Spring it
            // means getBeansOfType + a sort over the matching bean definitions, uncached. The
            // gates stay per-capture reads so the configuration remains re-tunable at runtime.
            ApplicationProperties props = resolveApplicationProperties();
            if (!eventCaptureEnabled(props)) {
                return;
            }
            String resourceType = subject != null ? subject.getClass().getName() : null;
            if (!isCaptured(props, resourceType)) {
                return;
            }
            TrafficReporter reporter = resolveReporter();
            if (reporter == null || !reporter.isEnabled()) {
                return;
            }
            reporter.reportDomainEvent(buildRecord(subject, beforeResource, afterResource, event, resourceType), subject);
        } catch (Exception e) {
            // telemetry must never break the event emitter
            log.warn("Traffic domain event capture failed, dropping record: {}", e.getMessage(), e);
        }
    }

    private WaterTrafficDomainEventRecord buildRecord(Resource subject, Resource beforeResource, Resource afterResource,
                                                      Event event, String resourceType) {
        return WaterTrafficDomainEventRecord.builder()
                .recordId(UUID.randomUUID().toString())
                .recordType(recordTypeOf(event))
                .timestamp(Instant.now())
                .outcome(Outcome.SUCCESS)
                // a domain event is a fact already happened: it has no duration
                .durationMillis(0L)
                .schemaVersion(1)
                .eventClass(eventClassOf(event))
                .resourceType(resourceType)
                .resourceId(resourceIdOf(subject))
                .changePhase(changePhaseOf(event))
                .changeOperation(changeOperationOf(event))
                .beforeRef(resourceRef(beforeResource))
                .afterRef(resourceRef(afterResource))
                .build();
    }

    /**
     * Resolves the {@link TrafficReporter} optionally from the registry (ADR-11 short-circuit):
     * returns {@code null} when the registry is unavailable or no reporter is registered.
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
            log.debug("No TrafficReporter registered, domain event capture disabled");
            return null;
        }
    }

    /**
     * The property VALUES are read per capture (never cached) because the whitelist is meant to be
     * re-tunable at runtime, but the {@link ApplicationProperties} COMPONENT is resolved once at
     * startup: it is a singleton for the whole runtime, so paying a registry lookup for it on every
     * CRUD operation would buy nothing. The registry fallback covers the runtimes (and the tests)
     * where the field was not injected.
     */
    private ApplicationProperties resolveApplicationProperties() {
        ApplicationProperties resolved = applicationProperties;
        if (resolved != null) {
            return resolved;
        }
        if (getComponentsRegistry() == null) {
            return null;
        }
        try {
            resolved = getComponentsRegistry().findComponent(ApplicationProperties.class, null);
        } catch (Exception e) {
            log.debug("No ApplicationProperties registered, falling back to domain event capture defaults");
            return null;
        }
        // cached only on success, so a listener activated before the properties component simply
        // retries on the next event instead of staying blind forever
        applicationProperties = resolved;
        return resolved;
    }

    private boolean eventCaptureEnabled(ApplicationProperties props) {
        if (props == null) {
            return true;
        }
        return Boolean.parseBoolean(props.getPropertyOrDefault(PROP_EVENTS_ENABLED, "true"));
    }

    /**
     * ADR-2: {@code opt-in} (default) captures only white-listed resource types, {@code opt-out}
     * captures everything. A null resourceType (event without a resource) can never match a
     * whitelist entry, so it is captured in {@code opt-out} mode only.
     */
    private boolean isCaptured(ApplicationProperties props, String resourceType) {
        String mode = props != null ? props.getPropertyOrDefault(PROP_EVENTS_MODE, "opt-in") : "opt-in";
        if (MODE_OPT_OUT.equalsIgnoreCase(safeTrim(mode))) {
            return true;
        }
        if (resourceType == null) {
            return false;
        }
        Set<String> whitelist = whitelist(props);
        if (whitelist.isEmpty()) {
            return false;
        }
        return whitelist.contains(resourceType) || whitelist.contains(simpleName(resourceType));
    }

    /**
     * Whitelist entries accept both the fully qualified and the simple resource class name.
     */
    private Set<String> whitelist(ApplicationProperties props) {
        if (props == null) {
            return Collections.emptySet();
        }
        String raw = props.getPropertyOrDefault(PROP_EVENTS_WHITELIST, "");
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * CRUD events are the only observable signal of the persistence layer (which Water cannot
     * intercept directly), hence they are classified as {@link RecordType#PERSISTENCE}; every
     * other domain event is a {@link RecordType#DOMAIN_EVENT}.
     */
    private RecordType recordTypeOf(Event event) {
        boolean crud = event instanceof PreCrudEvent
                || event instanceof PostCrudEvent
                || event instanceof PreCrudDetailedEvent
                || event instanceof PostCrudDetailedEvent;
        return crud ? RecordType.PERSISTENCE : RecordType.DOMAIN_EVENT;
    }

    private ChangePhase changePhaseOf(Event event) {
        if (event instanceof PreEvent || event instanceof PreDetailedEvent) {
            return ChangePhase.PRE;
        }
        if (event instanceof PostEvent || event instanceof PostDetailedEvent) {
            return ChangePhase.POST;
        }
        return null;
    }

    private ChangeOperation changeOperationOf(Event event) {
        if (event instanceof PreSaveEvent || event instanceof PostSaveEvent) {
            return ChangeOperation.SAVE;
        }
        if (event instanceof PreUpdateEvent || event instanceof PostUpdateEvent
                || event instanceof PreUpdateDetailedEvent || event instanceof PostUpdateDetailedEvent) {
            return ChangeOperation.UPDATE;
        }
        if (event instanceof PreRemoveEvent || event instanceof PostRemoveEvent) {
            return ChangeOperation.REMOVE;
        }
        return ChangeOperation.GENERIC;
    }

    /**
     * Prefers the most specific Water {@link Event} SUB-interface implemented by the event
     * instance: the concrete class may well be an anonymous class or a dynamic proxy, whose name
     * carries no information for a consumer downstream. {@code Event} itself is skipped because it
     * is a bare marker - when it is the only interface implemented, the concrete class name is the
     * more informative of the two and is used instead.
     */
    private String eventClassOf(Event event) {
        if (event == null) {
            return null;
        }
        return Arrays.stream(event.getClass().getInterfaces())
                .filter(iface -> Event.class.isAssignableFrom(iface) && !Event.class.equals(iface))
                .map(Class::getName)
                .findFirst()
                .orElseGet(() -> event.getClass().getName());
    }

    private String resourceIdOf(Resource resource) {
        if (resource instanceof BaseEntity) {
            return String.valueOf(((BaseEntity) resource).getId());
        }
        return null;
    }

    /**
     * ADR-4: a REFERENCE to the resource state, never the state itself.
     */
    private String resourceRef(Resource resource) {
        if (resource == null) {
            return null;
        }
        String id = resourceIdOf(resource);
        return id != null ? resource.getClass().getName() + "#" + id : resource.getClass().getName();
    }

    private String simpleName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    private String safeTrim(String value) {
        return value != null ? value.trim() : null;
    }
}
