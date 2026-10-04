
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

package it.water.core.service.traffic.interceptors;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.interceptors.GlobalAfterMethodInterceptor;
import it.water.core.api.interceptors.GlobalBeforeMethodInterceptor;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseApi;
import it.water.core.api.service.BaseSystemApi;
import it.water.core.api.service.Service;
import it.water.core.api.service.rest.RestApi;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.annotations.NoTraffic;
import it.water.core.api.traffic.annotations.ReportTraffic;
import it.water.core.api.traffic.model.Outcome;
import it.water.core.api.traffic.model.RecordType;
import it.water.core.interceptors.WaterAbstractInterceptor;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.service.traffic.WaterTrafficCallRecord;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author Aristide Cittadino.
 * Service-to-Service (S2S) traffic capture: once {@code water.traffic.s2s.enabled} is on, every
 * method declared by one of Water's four architectural layers - {@link RestApi}, {@link BaseApi},
 * {@link BaseSystemApi}, {@link BaseRepository} - is reported, and nothing else is.
 * <p>
 * <b>Opt-out inside a fixed scope.</b> The earlier capture was opt-in, driven by {@link ReportTraffic}
 * on each method: it traced nothing until someone remembered to annotate, which is the opposite of
 * what observability is for. Capture is now automatic, but only across those four boundaries, because
 * they are exactly the ones a request crosses (REST &rarr; Api &rarr; SystemApi &rarr; repository).
 * The result describes the FLOW of a request instead of logging every call in the process. Anything
 * else - helper interfaces, plain {@code Service}s, framework plumbing, the traffic pipeline itself -
 * is out of scope by construction, which is why this class needs neither an infrastructure exclusion
 * list nor a re-entrancy guard. Finer exclusions inside the scope: {@link NoTraffic} at the source,
 * {@code water.traffic.s2s.exclude} for a deployment.
 * <p>
 * <b>Call tree.</b> The before-hook assigns the record's identity on entry and reads the currently
 * open span as its parent, so nested calls form a tree ({@code parentId} / {@code depth}) instead of a
 * flat set of siblings sharing a {@code correlationId}.
 * <p>
 * <b>R1 (SUCCESS-only).</b> As with the annotation-driven interceptors it replaces, the after-hook
 * does not run on the exception path in any runtime, so records carry {@link Outcome#SUCCESS} and
 * failures stay uncaptured until the proxy chain grows a {@code finally} hook.
 * <p>
 * <b>Fail-safe.</b> Nothing here ever propagates to the caller: a telemetry failure must not break
 * business code.
 */
@FrameworkComponent(services = {GlobalBeforeMethodInterceptor.class, GlobalAfterMethodInterceptor.class})
public class TrafficS2SInterceptor extends WaterAbstractInterceptor<Service>
        implements GlobalBeforeMethodInterceptor, GlobalAfterMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TrafficS2SInterceptor.class);

    private static final String PROP_S2S_ENABLED = "water.traffic.s2s.enabled";
    private static final String PROP_S2S_EXCLUDE = "water.traffic.s2s.exclude";

    /**
     * "Is this (class, method) pair part of one of the four traced layers?" - the answer never changes
     * for a given pair, and it is consulted on every intercepted call.
     */
    private static final Map<LayerKey, Boolean> TRACED_LAYER_CACHE = new ConcurrentHashMap<>();

    private static final String WATER_PACKAGE_PREFIX = "it.water.";

    /**
     * Derived module id per component class - a pure function of the class, computed once.
     */
    private static final Map<Class<?>, String> MODULE_ID_CACHE = new ConcurrentHashMap<>();

    /**
     * Injected ONCE at startup: this interceptor is a framework component but NOT a Water
     * {@code Service}, so it is registered un-proxied and the lazy, per-invocation field injection
     * never runs on it - a plain {@code @Inject} would silently stay null and disable the capture.
     */
    @Inject(injectOnceAtStartup = true)
    @Setter
    @Getter
    private ComponentRegistry componentsRegistry;

    /**
     * Resolved lazily and cached on success, never injected at startup: in OSGi
     * {@code ApplicationProperties} is registered AFTER the framework components, so a startup
     * injection would fail the activator and bring the container down.
     */
    @Setter
    @Getter
    private volatile ApplicationProperties applicationProperties;

    @Override
    public <S extends Service> void interceptMethod(S destination, Method m, Object[] args) {
        if (!captureEnabled()) {
            return;
        }
        try {
            Class<?> concreteClass = computeServiceClass(destination);
            if (!isTracedLayer(concreteClass, m) || isExcluded(concreteClass, m)) {
                return;
            }
            TrafficReporter reporter = resolveReporter();
            if (reporter == null || !reporter.isEnabled()) {
                return;
            }
            TrafficCallContext.Span parent = TrafficCallContext.peek();
            TrafficCallContext.push(new TrafficCallContext.Span(
                    System.nanoTime(),
                    // an inner call inherits the correlation of the call it belongs to
                    parent != null ? parent.correlationId() : UUID.randomUUID().toString(),
                    operationOf(concreteClass, m),
                    concreteClass.getName(),
                    UUID.randomUUID().toString(),
                    parent != null ? parent.recordId() : null,
                    TrafficCallContext.depth()));
        } catch (Exception e) {
            log.warn("S2S traffic capture failed on entry, skipping span: {}", e.getMessage(), e);
        }
    }

    @Override
    public <S extends Service> void interceptMethod(S destination, Method m, Object[] args, Object returnResult) {
        if (!captureEnabled()) {
            return;
        }
        try {
            Class<?> concreteClass = computeServiceClass(destination);
            if (!isTracedLayer(concreteClass, m) || isExcluded(concreteClass, m)) {
                // symmetric with the before-hook: no span was pushed, so there is none to pop
                return;
            }
            TrafficReporter reporter = resolveReporter();
            if (reporter == null || !reporter.isEnabled()) {
                return;
            }
            TrafficCallContext.Span span = TrafficCallContext.pop();
            if (span == null) {
                // defensive: capture was switched on between entry and exit of this call
                return;
            }
            // no re-entrancy guard is needed around this call: reporting goes through
            // TrafficReporter/TrafficPublisher, which are plain Services and therefore not one of the
            // four traced layers - the capture cannot observe itself by construction
            reporter.reportCall(recordOf(span, concreteClass, Outcome.SUCCESS, null));
        } catch (Exception e) {
            log.warn("S2S traffic capture failed on exit, dropping record: {}", e.getMessage(), e);
        }
    }

    /**
     * Failure path (R1 closed). Reports the call with {@link Outcome#ERROR} and the exception type, and
     * - just as importantly - pops the span the entry hook opened.
     * <p>
     * Before this hook existed the after-hook simply never ran on an exception, so a failed call was
     * invisible (the calls most worth investigating) and its span stayed on the thread's deque, making
     * the NEXT successful call adopt a stale parent. That leak was mitigated by capping the deque
     * ({@code MAX_DEPTH}); it is now prevented instead.
     */
    @Override
    public <S extends Service> void interceptError(S destination, Method m, Object[] args, Throwable error) {
        if (!captureEnabled()) {
            return;
        }
        try {
            Class<?> concreteClass = computeServiceClass(destination);
            if (!isTracedLayer(concreteClass, m) || isExcluded(concreteClass, m)) {
                return;
            }
            TrafficReporter reporter = resolveReporter();
            if (reporter == null || !reporter.isEnabled()) {
                return;
            }
            TrafficCallContext.Span span = TrafficCallContext.pop();
            if (span == null) {
                return;
            }
            reporter.reportCall(recordOf(span, concreteClass, Outcome.ERROR, error));
        } catch (Exception e) {
            log.warn("S2S traffic capture failed on the error path, dropping record: {}", e.getMessage(), e);
        }
    }

    /**
     * Builds the record from the span closed on exit. {@code errorMessage} is passed through as-is: the
     * truncation required by ADR-4 belongs to the policy, which every record crosses.
     */
    private WaterTrafficCallRecord recordOf(TrafficCallContext.Span span, Class<?> concreteClass, Outcome outcome, Throwable error) {
        return WaterTrafficCallRecord.builder()
                .recordId(span.recordId())
                .recordType(isSystemApi(concreteClass) ? RecordType.SYSTEM_API : RecordType.API)
                .timestamp(Instant.now())
                .outcome(outcome)
                .errorType(error != null ? error.getClass().getName() : null)
                .errorMessage(error != null ? error.getMessage() : null)
                .durationMillis((System.nanoTime() - span.startNanos()) / 1_000_000L)
                .operation(span.operation())
                .destination(span.destination())
                .moduleId(moduleIdOf(concreteClass))
                .correlationId(span.correlationId())
                .parentId(span.parentId())
                .depth(span.depth())
                .schemaVersion(1)
                .build();
    }

    /**
     * Module the captured component belongs to, derived from its package: everything up to (and
     * including) the segment after {@code it.water}, e.g. {@code it.water.user.service.UserServiceImpl}
     * &rarr; {@code it.water.user}. Cheap, needs no registry, and gives records a "who owns this" axis
     * that was previously always null. Falls back to the package as a whole for components outside the
     * {@code it.water} namespace, and to {@code null} for the default package.
     */
    private String moduleIdOf(Class<?> concreteClass) {
        return MODULE_ID_CACHE.computeIfAbsent(concreteClass, clazz -> {
            Package pkg = clazz.getPackage();
            if (pkg == null) {
                return "";
            }
            String name = pkg.getName();
            if (!name.startsWith(WATER_PACKAGE_PREFIX)) {
                return name;
            }
            int moduleStart = WATER_PACKAGE_PREFIX.length();
            int moduleEnd = name.indexOf('.', moduleStart);
            return moduleEnd < 0 ? name : name.substring(0, moduleEnd);
        });
    }

    /**
     * Master gate, read on every intercepted call so it stays switchable at runtime. Kept as the
     * FIRST check because it is the only one cheap enough to sit on this path unconditionally: a
     * property read against an already-resolved component, no registry lookup, no ThreadLocal.
     */
    private boolean captureEnabled() {
        ApplicationProperties props = resolveApplicationProperties();
        return props != null && Boolean.parseBoolean(props.getPropertyOrDefault(PROP_S2S_ENABLED, "false"));
    }

    /**
     * The scope of the capture: a call is traffic only when the invoked method is declared by one of
     * the FOUR architectural layers of a Water application - {@link RestApi}, {@link BaseApi},
     * {@link BaseSystemApi}, {@link BaseRepository}. Everything else is ignored.
     * <p>
     * That is what makes the opt-out capture meaningful instead of overwhelming: those four
     * interfaces are exactly the boundaries a request crosses (REST &rarr; Api &rarr; SystemApi &rarr;
     * repository), so the telemetry describes the FLOW of a request and nothing else. A component's
     * own helper interfaces, framework plumbing and any plain {@code Service} stay out by
     * construction - no exclusion list needed for them.
     * <p>
     * It is also what removed two mechanisms an earlier iteration needed: interceptors
     * ({@code AbstractPermissionInterceptor implements Service}) used to be captured while the chain
     * called {@code getAnnotation()} on them, and the traffic pipeline used to capture its own
     * reporting - both are simply not one of the four layers, so neither the infrastructure
     * exclusion list nor the re-entrancy guard exist any more.
     * <p>
     * The check is on the METHOD, not just on the component: a class may implement one of these
     * interfaces and expose other methods of its own, and those are not part of the flow.
     */
    private boolean isTracedLayer(Class<?> concreteClass, Method m) {
        return TRACED_LAYER_CACHE.computeIfAbsent(
                new LayerKey(concreteClass, m),
                key -> declaredByTracedLayer(key.owner, m));
    }

    /**
     * Under a JDK proxy the incoming method already belongs to an interface, so its declaring class
     * settles it; under Spring the method is the concrete one, so the implemented interfaces are
     * walked. Either way the answer is cached, since it never changes for a (class, method) pair.
     */
    private boolean declaredByTracedLayer(Class<?> concreteClass, Method m) {
        if (m.getDeclaringClass().isInterface() && isTracedLayerInterface(m.getDeclaringClass())) {
            return true;
        }
        Class<?> current = concreteClass;
        while (current != null && current != Object.class) {
            for (Class<?> itf : current.getInterfaces()) {
                if (declaresOnTracedLayer(itf, m)) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }

    /**
     * @return {@code true} when this interface declares the method AND is one of the four traced
     * layers (directly or by extending one of them)
     */
    private boolean declaresOnTracedLayer(Class<?> itf, Method m) {
        boolean declaresIt;
        try {
            itf.getMethod(m.getName(), m.getParameterTypes());
            declaresIt = true;
        } catch (NoSuchMethodException e) {
            declaresIt = false;
        }
        if (declaresIt && isTracedLayerInterface(itf)) {
            return true;
        }
        // the method may be declared further up, on the layer interface this one extends
        for (Class<?> parent : itf.getInterfaces()) {
            if (declaresOnTracedLayer(parent, m)) {
                return true;
            }
        }
        return false;
    }

    private boolean isTracedLayerInterface(Class<?> itf) {
        return RestApi.class.isAssignableFrom(itf)
                || BaseApi.class.isAssignableFrom(itf)
                || BaseSystemApi.class.isAssignableFrom(itf)
                || BaseRepository.class.isAssignableFrom(itf);
    }

    /**
     * Exclusions are additive on top of the layer scope: {@link NoTraffic} on the method, on the
     * interface method, or on the component, plus the {@code water.traffic.s2s.exclude} prefixes.
     * Any of them excludes.
     */
    private boolean isExcluded(Class<?> concreteClass, Method m) {
        if (hasNoTraffic(m, concreteClass)) {
            return true;
        }
        String className = concreteClass.getName();
        for (String prefix : exclusions()) {
            if (className.startsWith(prefix) || (className + "#" + m.getName()).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for {@link NoTraffic} where a developer would reasonably put it: on the invoked method
     * (which under a JDK proxy is the interface method), on the concrete implementation of that
     * method, or on the component type itself.
     */
    private boolean hasNoTraffic(Method m, Class<?> concreteClass) {
        if (m.isAnnotationPresent(NoTraffic.class) || concreteClass.isAnnotationPresent(NoTraffic.class)) {
            return true;
        }
        try {
            return concreteClass.getMethod(m.getName(), m.getParameterTypes()).isAnnotationPresent(NoTraffic.class);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * {@link ReportTraffic} is no longer what turns capture on - it is an override for the operation
     * name when the method name is not the label you want to see in the telemetry.
     */
    private String operationOf(Class<?> concreteClass, Method m) {
        ReportTraffic annotation = findReportTraffic(m, concreteClass);
        if (annotation != null && annotation.operation() != null && !annotation.operation().isBlank()) {
            return annotation.operation();
        }
        return m.getName();
    }

    private ReportTraffic findReportTraffic(Method m, Class<?> concreteClass) {
        ReportTraffic onMethod = m.getAnnotation(ReportTraffic.class);
        if (onMethod != null) {
            return onMethod;
        }
        try {
            return concreteClass.getMethod(m.getName(), m.getParameterTypes()).getAnnotation(ReportTraffic.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private List<String> exclusions() {
        ApplicationProperties props = resolveApplicationProperties();
        if (props == null) {
            return Collections.emptyList();
        }
        String raw = props.getPropertyOrDefault(PROP_S2S_EXCLUDE, "");
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }

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
            log.debug("ApplicationProperties not resolvable yet, S2S capture stays off: {}", e.getMessage());
            return null;
        }
        applicationProperties = resolved;
        return resolved;
    }

    /**
     * Resolves the {@link TrafficReporter} optionally from the registry (ADR-11 short-circuit):
     * returns {@code null} when none is registered instead of propagating.
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
        } catch (Exception e) {
            log.debug("No TrafficReporter registered, S2S capture disabled");
            return null;
        }
    }

    /**
     * Same heuristic as the interceptors this class replaces: a component is treated as a SystemApi
     * when one of its interfaces is named {@code *SystemApi}.
     */
    private boolean isSystemApi(Class<?> concreteClass) {
        for (Class<?> iface : concreteClass.getInterfaces()) {
            if (iface.getSimpleName().endsWith("SystemApi")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves the concrete service class behind a (possibly proxied) Water service, mirroring
     * {@code LogMethodExecutionInterceptor#computeServiceClass}.
     */
    private <S> Class<?> computeServiceClass(S service) {
        if (service instanceof Service && Proxy.isProxyClass(service.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(service);
            if (invocationHandler instanceof WaterAbstractInterceptor) {
                return ((WaterAbstractInterceptor<?>) invocationHandler).getOriginalConcreteClass();
            }
            return invocationHandler.getClass();
        }
        return service.getClass();
    }

    /**
     * Unused here: this interceptor is not annotation-driven. Declared because
     * {@link WaterAbstractInterceptor} descendants historically expose it; the framework never calls
     * it for a global interceptor.
     */
    @SuppressWarnings({"rawtypes"})
    public Class<? extends Annotation> getAnnotation() {
        return null;
    }

    /**
     * @Author Aristide Cittadino
     * Cache key for "is this method part of a traced layer?". The concrete class belongs to the key
     * because the same {@link Method} can be reached through components implementing different
     * interface sets.
     */
    private static final class LayerKey {

        private final Class<?> owner;
        private final Method method;

        private LayerKey(Class<?> owner, Method method) {
            this.owner = owner;
            this.method = method;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LayerKey)) {
                return false;
            }
            LayerKey other = (LayerKey) o;
            return owner.equals(other.owner) && method.equals(other.method);
        }

        @Override
        public int hashCode() {
            return 31 * owner.hashCode() + method.hashCode();
        }
    }
}
