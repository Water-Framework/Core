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

package it.water.core.interceptors;

import it.water.core.api.interceptors.*;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author Aristide Cittadino
 */
@AllArgsConstructor
@NoArgsConstructor
public abstract class WaterAbstractInterceptor<S extends Service> implements it.water.core.api.interceptors.Proxy {
    private static Logger log = LoggerFactory.getLogger(WaterAbstractInterceptor.class);

    /**
     * How long a resolved list of global interceptors stays valid. Deliberately short and not
     * configurable: it only has to be long enough to keep the registry off the per-invocation path,
     * while still letting a component registered later (OSGi bundles start in any order) become
     * active on its own.
     */
    private static final long GLOBAL_INTERCEPTORS_CACHE_TTL_MILLIS = 1_000L;

    /**
     * Shared by every proxy: which global interceptors are registered depends on the registry, never
     * on the instance, and a per-proxy cache would multiply the same entries by the number of
     * components.
     */
    private static final Map<Class<?>, GlobalInterceptorsSnapshot> globalInterceptorsCache = new ConcurrentHashMap<>();

    //original service
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private S service;


    /**
     * Loads all BeforeMethodInterceptor Components registered as OSGi services and execute them before method invocation
     *
     * @param method
     * @param args
     * @throws NoSuchMethodException
     */
    protected void executeInterceptorBeforeMethod(S service, Method method, Object[] args) throws NoSuchMethodException {
        this.executeInterceptor(service, method, args, null, BeforeMethodInterceptor.class);
    }

    /**
     * Loads all AfterMethodInterceptor Components registered as OSGi services and execute them after method invocation
     *
     * @param method
     * @param args
     * @param result
     * @throws NoSuchMethodException
     */
    protected void executeInterceptorAfterMethod(S service, Method method, Object[] args, Object result) throws NoSuchMethodException {
        this.executeInterceptor(service, method, args, result, AfterMethodInterceptor.class);
    }

    /**
     * Notifies the global interceptors that the invocation FAILED, in place of the after-hook. Runtimes
     * must call it from their exception path and then rethrow: the hook observes, it never swallows.
     * <p>
     * Without this, an interceptor that opens per-call state in its before-hook (the traffic capture
     * opens a span) would leak it on every exception, and every failure would be invisible to
     * telemetry - the very calls worth investigating. Only the annotation-free interceptors are
     * notified: the annotation-driven ones keep their historical success-only contract.
     */
    protected void executeInterceptorOnErrorMethod(S service, Method method, Object[] args, Throwable error) {
        for (Object interceptor : resolveGlobalInterceptors(GlobalAfterMethodInterceptor.class)) {
            try {
                ((GlobalAfterMethodInterceptor) interceptor).interceptError(service, method, args, error);
            } catch (Exception e) {
                // a failing interceptor must never replace the business exception being propagated
                log.warn("Global interceptor {} failed handling the error of {}.{}: {}",
                        interceptor.getClass().getName(), service.getClass().getName(), method.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Analyzes the method invocation searching for WaterInterceptorExecutor Annotation.
     *
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @throws NoSuchMethodException
     */
    protected void executeInterceptor(S service, Method method, Object[] args, Object result, @SuppressWarnings("rawtypes") Class<? extends MethodInterceptor> interceptorClass) throws NoSuchMethodException {
        interceptAnnotationsOnFields(service, method, args, result, interceptorClass);
        interceptAnnotationsOnMethod(service, method, args, result, interceptorClass);
        interceptGlobally(service, method, args, result, interceptorClass);
    }

    /**
     * Runs the interceptors that apply to EVERY method, with no annotation opting in
     * ({@link GlobalBeforeMethodInterceptor} / {@link GlobalAfterMethodInterceptor}).
     * <p>
     * What makes this affordable on a path that every call to every service walks through is the
     * <b>cached resolution</b>: the registry is queried at most once per
     * {@link #GLOBAL_INTERCEPTORS_CACHE_TTL_MILLIS} instead of once per invocation - a lookup is
     * {@code getBeansOfType} plus a sort in Spring, which would be untenable here. The short TTL
     * (rather than a permanent cache) is what lets a global interceptor registered late - the normal
     * case in OSGi, where bundles start in any order - become active without a restart. With no global
     * interceptor registered, which is the default deployment, the whole hook costs one map lookup.
     * <p>
     * <b>No filtering is applied here.</b> Every intercepted method is offered to the global
     * interceptors, and each one decides what it cares about - the framework has no way to guess a
     * meaningful scope for an arbitrary cross-cutting concern. Note that this means the two runtimes
     * offer different sets: OSGi proxies interfaces only, whereas the Spring pointcut
     * ({@code execution(* *(..)) && target(Service+)}) also matches the target's own methods. An
     * implementation that must behave identically everywhere has to scope itself explicitly - the way
     * the traffic capture does, by only accepting methods declared on Water's four architectural
     * layers.
     * <p>
     * An interceptor that throws is isolated and logged: telemetry must never break business code.
     */
    @SuppressWarnings("rawtypes")
    private void interceptGlobally(S service, Method method, Object[] args, Object result, Class<? extends MethodInterceptor> interceptorClass) {
        boolean before = BeforeMethodInterceptor.class.isAssignableFrom(interceptorClass);
        Class<?> globalType = before ? GlobalBeforeMethodInterceptor.class : GlobalAfterMethodInterceptor.class;
        List<?> globalInterceptors = resolveGlobalInterceptors(globalType);
        if (globalInterceptors.isEmpty()) {
            return;
        }
        for (Object interceptor : globalInterceptors) {
            try {
                if (before) {
                    ((GlobalBeforeMethodInterceptor) interceptor).interceptMethod(service, method, args);
                } else {
                    ((GlobalAfterMethodInterceptor) interceptor).interceptMethod(service, method, args, result);
                }
            } catch (Exception e) {
                log.warn("Global interceptor {} failed on {}.{}: {}", interceptor.getClass().getName(),
                        service.getClass().getName(), method.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Keeps the FIRST occurrence of each interceptor TYPE, discarding further instances of the same
     * class.
     * <p>
     * A global interceptor normally declares two services - before and after are two interfaces on the
     * same class - and the framework instantiates and registers the component once per declared
     * service, so the registry hands back several DISTINCT instances of the same class (three, for the
     * traffic capture). Running them all multiplies every downstream effect: one logical call produced
     * three nested spans and three records, each pointing at the previous one as its parent - a call
     * tree that looked deep while being the same hop reported repeatedly, with inflated depths.
     * <p>
     * Deduplicating by type rather than by reference is what actually fixes it, and it is sound for
     * this contract: a global interceptor is a cross-cutting CONCERN, not a stateful participant -
     * every instance of the same class would do exactly the same work. The trade-off is explicit: two
     * deliberately different instances of the same interceptor class (e.g. registered with different
     * component properties) would collapse into one. Use two classes if you need two behaviours.
     * <p>
     * The first occurrence wins, which preserves the registry's priority ordering.
     */
    private static List<?> deduplicateByType(List<?> interceptors) {
        if (interceptors.size() < 2) {
            return interceptors;
        }
        Set<Class<?>> seenTypes = new HashSet<>();
        List<Object> unique = new ArrayList<>(interceptors.size());
        for (Object interceptor : interceptors) {
            if (seenTypes.add(interceptor.getClass())) {
                unique.add(interceptor);
            }
        }
        return unique;
    }

    /**
     * Visible for testing: drops the cached global interceptors so a test can change what the registry
     * returns without waiting out {@link #GLOBAL_INTERCEPTORS_CACHE_TTL_MILLIS}. Never call it from
     * production code - the TTL is what keeps the registry off the per-invocation path.
     */
    static void clearGlobalInterceptorsCache() {
        globalInterceptorsCache.clear();
    }

    /**
     * @return the registered global interceptors of the given type, from the cache when it is still
     * fresh; never {@code null}
     */
    private List<?> resolveGlobalInterceptors(Class<?> globalType) {
        GlobalInterceptorsSnapshot snapshot = globalInterceptorsCache.get(globalType);
        long now = System.nanoTime();
        if (snapshot != null && now - snapshot.takenAtNanos < GLOBAL_INTERCEPTORS_CACHE_TTL_MILLIS * 1_000_000L) {
            return snapshot.interceptors;
        }
        List<?> resolved = Collections.emptyList();
        if (getComponentsRegistry() != null) {
            try {
                List<?> found = getComponentsRegistry().findComponents(globalType, null);
                if (found != null) {
                    resolved = deduplicateByType(found);
                }
            } catch (Exception e) {
                // Exception, not just NoComponentRegistryFoundException: this runs on every service
                // call INCLUDING the ones the container makes while it is still wiring itself up, when
                // the registry may not be usable yet - SpringComponentRegistry throws a raw NPE if its
                // ApplicationContext has not been injected, and letting that escape aborted the whole
                // Spring context startup. A global interceptor must never break the caller, least of
                // all the bootstrap. The short cache TTL makes the next call retry.
                log.debug("Global interceptors of type {} not resolvable (yet): {}", globalType.getName(), e.getMessage());
            }
        }
        globalInterceptorsCache.put(globalType, new GlobalInterceptorsSnapshot(resolved, now));
        return resolved;
    }

    /**
     * Returns the original component generic interfaces
     *
     * @return
     */
    public Type[] getOriginalGenericInterfaces() {
        return this.getService().getClass().getGenericInterfaces();
    }

    @Override
    public Class<?> getOriginalConcreteClass() {
        return this.getService().getClass();
    }

    protected abstract ComponentRegistry getComponentsRegistry();

    /**
     * Method which scans fields searching for annotations which are related to WaterInterceptorExecutor.
     * If it is found, it means that the current field must be intercepted by the defined interceptor inside the annotation or a registed interceptor component.
     *
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     */
    private void interceptAnnotationsOnFields(S service, Method method, Object[] args, Object result, @SuppressWarnings("rawtypes") Class<? extends MethodInterceptor> interceptorClass) {
        Map<Annotation, List<Field>> annotationsMap = new HashMap<>();
        Arrays.stream(getAllDeclaredFields(service)).forEach(field -> Arrays.stream(field.getDeclaredAnnotations())
                //ex. WaterInject annotation that are not injected at startup
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(InterceptorExecutor.class)).forEach(annotation -> {
                    if (!annotationsMap.containsKey(annotation)) annotationsMap.put(annotation, new ArrayList<>());
                    annotationsMap.get(annotation).add(field);
                }));
        annotationsMap.keySet().iterator().forEachRemaining(annotation -> {
            boolean intercepted = interceptBasedOnAnnotationInterceptorExecutor(annotation, annotationsMap.get(annotation), service, method, args, result, interceptorClass);
            if (!intercepted)
                interceptBasedOnRegisterdInterceptorExecutor(annotation, annotationsMap.get(annotation), service, method, args, result, interceptorClass);
        });
    }

    /**
     * Method which scans method searching for annotations extending WaterInterceptorExecutor.
     * If found this means that the current field must be intercepted by the defined interceptor inside the annotation ir a registered interceptor component..
     *
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @throws NoSuchMethodException
     */
    private void interceptAnnotationsOnMethod(S service, Method method, Object[] args, Object result, @SuppressWarnings("rawtypes") Class<? extends MethodInterceptor> interceptorClass) throws NoSuchMethodException {
        try {
            Annotation[] annotations = service.getClass().getMethod(method.getName(), method.getParameterTypes()).getDeclaredAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                Annotation annotation = annotations[i];
                //trying to intercept first looking at annotation definition, if it contains WaterInterceptorExecutor annotation definition which specifies the implementation
                boolean intercepted = interceptBasedOnAnnotationInterceptorExecutor(annotation, null, service, method, args, result, interceptorClass);
                //if no annotation WaterInterceptorExecutor is found then we search inside the registry for an implementation for that interceptor
                if (!intercepted)
                    interceptBasedOnRegisterdInterceptorExecutor(annotation, null, service, method, args, result, interceptorClass);
            }
        } catch (NoSuchMethodException e) {
            log.debug(e.getMessage(), e);
        }
    }

    /**
     * Method which searches for WaterInterceptorExecutor annotation definition and execute the interceptor configured inside the annotation
     *
     * @param annotation
     * @param annotatedFields
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @return
     */
    private boolean interceptBasedOnAnnotationInterceptorExecutor(Annotation annotation, List<Field> annotatedFields, S service, Method method, Object[] args, Object result, @SuppressWarnings("rawtypes") Class<? extends MethodInterceptor> interceptorClass) {
        if (annotation.annotationType().isAnnotationPresent(InterceptorExecutor.class)) {
            InterceptorExecutor interceptorAnnotation = annotation.annotationType().getDeclaredAnnotation(InterceptorExecutor.class);
            Class<? extends MethodInterceptor<?>> executor = interceptorAnnotation.interceptor();
            if (this.getComponentsRegistry() != null) {
                MethodInterceptor<?> interceptor = this.getComponentsRegistry().findComponent(executor, null);
                if (interceptorClass.isAssignableFrom(interceptor.getClass())) {
                    doInterception(annotation, annotatedFields, service, method, args, result, interceptor);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Method which searches for compoents exposed as a service implementing WaterInterceptorExecutor
     *
     * @param annotation
     * @param annotatedFields
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @return
     */
    private boolean interceptBasedOnRegisterdInterceptorExecutor(Annotation annotation, List<Field> annotatedFields, S service, Method method, Object[] args, Object result, @SuppressWarnings("rawtypes") Class<? extends MethodInterceptor> interceptorClass) {
        if (this.getComponentsRegistry() != null) {
            //find the executor implementation based on registerd components which expose for example BeforeMethodInterceptor or AfterMethodInterceptor
            try {
                @SuppressWarnings("rawtypes")
                List<? extends MethodInterceptor> interceptors = this.getComponentsRegistry().findComponents(interceptorClass, null);
                //Filter amongs all interceptors which use the same annotation
                @SuppressWarnings("rawtypes")
                Optional<? extends MethodInterceptor> executor = interceptors.stream().filter(interceptor -> interceptor.getAnnotation().equals(annotation.annotationType())).findFirst();
                //if an interceptor is matched then run the interception
                if (executor.isPresent()) {
                    if (interceptorClass.isAssignableFrom(executor.get().getClass())) {
                        doInterception(annotation, annotatedFields, service, method, args, result, executor.get());
                    }
                    return true;
                }
            } catch (NoComponentRegistryFoundException e) {
                log.debug("No component found for: {}", interceptorClass);
            }
        }
        return false;
    }

    /**
     * Method which executes interception logic on field or methods based on the current interceptor type
     *
     * @param annotation
     * @param annotatedFields
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptor
     */
    private void doInterception(Annotation annotation, List<Field> annotatedFields, S service, Method method, Object[] args, Object result, MethodInterceptor<?> interceptor) {
        //avoiding calling after method with before methods
        if (interceptor != null) {
            //first most specific types since BeforeMethodFieldInterceptor is also BeforeMethodInterceptor
            if (BeforeMethodFieldInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                BeforeMethodFieldInterceptor<Annotation> beforeInterceptor = (BeforeMethodFieldInterceptor) interceptor;
                beforeInterceptor.interceptMethod(service, method, annotatedFields, args, annotation);
            } else if (AfterMethodFieldInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                AfterMethodFieldInterceptor<Annotation> afterInterceptor = (AfterMethodFieldInterceptor) interceptor;
                afterInterceptor.interceptMethod(service, method, annotatedFields, args, annotation);
            }
            //Then we compare generic types
            else if (BeforeMethodInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                BeforeMethodInterceptor<Annotation> beforeInterceptor = (BeforeMethodInterceptor) interceptor;
                beforeInterceptor.interceptMethod(service, method, args, annotation);
            } else if (AfterMethodInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                AfterMethodInterceptor<Annotation> afterInterceptor = (AfterMethodInterceptor) interceptor;
                afterInterceptor.interceptMethod(service, method, args, result, annotation);
            }
        }
    }

    /**
     * Returs all declared fields inside a class
     *
     * @param service
     * @return
     */
    private Field[] getAllDeclaredFields(S service) {
        List<Field> fieldsList = new ArrayList<>();
        Class<?> currentClass = service.getClass();
        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            fieldsList.addAll(Arrays.asList(fields));
            Class<?> superclass = currentClass.getSuperclass();
            if (!superclass.equals(currentClass) && Service.class.isAssignableFrom(superclass))
                currentClass = superclass;
            else currentClass = null;
        }
        Field[] fields = new Field[fieldsList.size()];
        return fieldsList.toArray(fields);
    }

    /**
     * @Author Aristide Cittadino
     * Immutable list of resolved global interceptors plus the instant it was taken, replaced as a
     * whole so a reader never observes a half-updated cache.
     */
    private static final class GlobalInterceptorsSnapshot {

        private final List<?> interceptors;
        private final long takenAtNanos;

        private GlobalInterceptorsSnapshot(List<?> interceptors, long takenAtNanos) {
            this.interceptors = interceptors;
            this.takenAtNanos = takenAtNanos;
        }
    }

}
