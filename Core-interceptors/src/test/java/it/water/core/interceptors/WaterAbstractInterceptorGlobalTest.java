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

import it.water.core.api.interceptors.GlobalAfterMethodInterceptor;
import it.water.core.api.interceptors.GlobalBeforeMethodInterceptor;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the annotation-free dispatch added to {@link WaterAbstractInterceptor}: the hook that lets a
 * component observe EVERY intercepted method without anything opting in.
 * <p>
 * Hand-written stubs instead of Mockito, for two reasons: the registry has to COUNT how many times it
 * is asked (the whole point of the cache is that it is asked once per TTL rather than once per call),
 * and the dispatch must be observed on a real {@link WaterAbstractInterceptor} subclass, since the
 * class is abstract and its registry accessor is the extension point.
 */
class WaterAbstractInterceptorGlobalTest {

    private CountingRegistry registry;
    private TestInterceptor interceptor;
    private TestService service;
    private Method method;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        // the cache is static and shared: without clearing it, one test would observe the previous
        // test's interceptors for up to the TTL
        WaterAbstractInterceptor.clearGlobalInterceptorsCache();
        registry = new CountingRegistry();
        service = new TestService();
        interceptor = new TestInterceptor(service, registry);
        method = TestServiceApi.class.getMethod("doSomething", String.class);
    }

    @Test
    void beforeHook_isInvokedForEveryMethod_withServiceMethodAndArgs() throws NoSuchMethodException {
        RecordingBefore before = new RecordingBefore();
        registry.beforeInterceptors = List.of(before);

        interceptor.runBefore(method, new Object[]{"arg-1"});

        assertEquals(1, before.calls.size());
        assertSame(service, before.calls.get(0).target);
        assertEquals("doSomething", before.calls.get(0).method.getName());
        assertEquals("arg-1", before.calls.get(0).args[0]);
    }

    @Test
    void afterHook_isInvokedWithTheReturnedResult() throws NoSuchMethodException {
        RecordingAfter after = new RecordingAfter();
        registry.afterInterceptors = List.of(after);

        interceptor.runAfter(method, new Object[]{"arg-1"}, "the-result");

        assertEquals(1, after.results.size());
        assertEquals("the-result", after.results.get(0));
    }

    @Test
    void beforeAndAfter_areDispatchedToTheirOwnTypeOnly() throws NoSuchMethodException {
        RecordingBefore before = new RecordingBefore();
        RecordingAfter after = new RecordingAfter();
        registry.beforeInterceptors = List.of(before);
        registry.afterInterceptors = List.of(after);

        interceptor.runBefore(method, new Object[]{"x"});

        assertEquals(1, before.calls.size());
        assertTrue(after.results.isEmpty(), "the after-hook must not fire on method entry");
    }

    @Test
    void noGlobalInterceptorRegistered_isANoOp() {
        registry.beforeInterceptors = Collections.emptyList();

        assertDoesNotThrow(() -> interceptor.runBefore(method, new Object[]{"x"}));
    }

    @Test
    void registryThrowing_isSwallowed() {
        registry.throwOnLookup = true;

        assertDoesNotThrow(() -> interceptor.runBefore(method, new Object[]{"x"}));
    }

    /**
     * Not a hypothetical: this hook also runs on the calls a container makes while it is still wiring
     * itself up, and {@code SpringComponentRegistry} throws a raw {@link NullPointerException} when its
     * {@code ApplicationContext} has not been injected yet. Letting that escape aborted the entire
     * Spring context startup, so ANY failure of the lookup has to be absorbed - not just the framework's
     * own "not found" exception.
     */
    @Test
    void registryFailingWithAnUncheckedException_doesNotBreakTheCaller() {
        registry.failWithNpe = true;

        assertDoesNotThrow(() -> interceptor.runBefore(method, new Object[]{"x"}));
    }

    /**
     * A global interceptor runs on every call of every service: one that throws must not be able to
     * break the intercepted business method, nor stop the other interceptors from running.
     */
    @Test
    void throwingInterceptor_isIsolated_andTheOthersStillRun() throws NoSuchMethodException {
        RecordingBefore healthy = new RecordingBefore();
        registry.beforeInterceptors = List.of(new ThrowingBefore(), healthy);

        assertDoesNotThrow(() -> interceptor.runBefore(method, new Object[]{"x"}));
        assertEquals(1, healthy.calls.size(), "a failing interceptor must not stop the next one");
    }

    /**
     * A global interceptor declares two services (before and after), so the framework instantiates and
     * registers the component once per declared service and the registry hands back several DISTINCT
     * instances of the same class. Running them all multiplied every downstream effect - with the
     * traffic capture it turned one call into three nested records - so instances of the same class
     * must collapse to one.
     */
    @Test
    void sameInstanceReturnedSeveralTimes_isInvokedOnce() throws NoSuchMethodException {
        RecordingBefore duplicated = new RecordingBefore();
        registry.beforeInterceptors = List.of(duplicated, duplicated, duplicated);

        interceptor.runBefore(method, new Object[]{"x"});

        assertEquals(1, duplicated.calls.size(), "the same instance must run once per invocation");
    }

    /**
     * The real shape of the defect: three separate instances of the SAME class, exactly what the
     * registry returns for a component declaring several services. Only one may run.
     */
    @Test
    void severalInstancesOfTheSameClass_onlyOneRuns() throws NoSuchMethodException {
        RecordingBefore first = new RecordingBefore();
        RecordingBefore second = new RecordingBefore();
        RecordingBefore third = new RecordingBefore();
        registry.beforeInterceptors = List.of(first, second, third);

        interceptor.runBefore(method, new Object[]{"x"});

        assertEquals(1, first.calls.size() + second.calls.size() + third.calls.size(),
                "instances of one interceptor class must not multiply the work");
    }

    /**
     * The flip side: different classes are different concerns and all of them must run.
     */
    @Test
    void interceptorsOfDifferentClasses_allRun() throws NoSuchMethodException {
        RecordingBefore recording = new RecordingBefore();
        SecondRecordingBefore other = new SecondRecordingBefore();
        registry.beforeInterceptors = List.of(recording, other);

        interceptor.runBefore(method, new Object[]{"x"});

        assertEquals(1, recording.calls.size());
        assertEquals(1, other.calls);
    }

    /**
     * The reason the cache exists: in Spring a registry lookup is {@code getBeansOfType} plus a sort,
     * which cannot happen on every call to every service.
     */
    @Test
    void interceptorsAreResolvedOncePerTtl_notOncePerInvocation() throws NoSuchMethodException {
        registry.beforeInterceptors = List.of(new RecordingBefore());

        for (int i = 0; i < 50; i++) {
            interceptor.runBefore(method, new Object[]{"x"});
        }

        assertEquals(1, registry.lookupCount.get(),
                "50 invocations must not mean 50 registry lookups");
    }

    @Test
    void clearingTheCache_makesTheNextInvocationResolveAgain() throws NoSuchMethodException {
        registry.beforeInterceptors = List.of(new RecordingBefore());
        interceptor.runBefore(method, new Object[]{"x"});

        WaterAbstractInterceptor.clearGlobalInterceptorsCache();
        interceptor.runBefore(method, new Object[]{"x"});

        assertEquals(2, registry.lookupCount.get());
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    interface TestServiceApi extends Service {
        String doSomething(String input);
    }

    static class TestService implements TestServiceApi {
        @Override
        public String doSomething(String input) {
            return "done:" + input;
        }
    }

    /**
     * Concrete {@link WaterAbstractInterceptor} exposing the two protected hooks the runtimes call.
     */
    static class TestInterceptor extends WaterAbstractInterceptor<TestService> {

        private final ComponentRegistry registry;

        TestInterceptor(TestService service, ComponentRegistry registry) {
            this.setService(service);
            this.registry = registry;
        }

        @Override
        protected ComponentRegistry getComponentsRegistry() {
            return registry;
        }

        void runBefore(Method m, Object[] args) throws NoSuchMethodException {
            executeInterceptorBeforeMethod(getService(), m, args);
        }

        void runAfter(Method m, Object[] args, Object result) throws NoSuchMethodException {
            executeInterceptorAfterMethod(getService(), m, args, result);
        }
    }

    /**
     * Counts lookups (so the cache is observable) and answers only the global interceptor types,
     * returning an empty list for anything else the interception chain asks for.
     */
    static class CountingRegistry implements ComponentRegistry {

        final AtomicInteger lookupCount = new AtomicInteger();
        List<?> beforeInterceptors = Collections.emptyList();
        List<?> afterInterceptors = Collections.emptyList();
        boolean throwOnLookup = false;
        boolean failWithNpe = false;

        @SuppressWarnings("unchecked")
        @Override
        public <T> List<T> findComponents(Class<T> componentClass, it.water.core.api.registry.filter.ComponentFilter filter) {
            if (componentClass == GlobalBeforeMethodInterceptor.class || componentClass == GlobalAfterMethodInterceptor.class) {
                lookupCount.incrementAndGet();
                if (throwOnLookup) {
                    throw new NoComponentRegistryFoundException();
                }
                if (failWithNpe) {
                    // mirrors SpringComponentRegistry with a null ApplicationContext during startup
                    throw new NullPointerException("applicationContext is null");
                }
                return (List<T>) (componentClass == GlobalBeforeMethodInterceptor.class ? beforeInterceptors : afterInterceptors);
            }
            return Collections.emptyList();
        }

        @Override
        public <T> T findComponent(Class<T> componentClass, it.water.core.api.registry.filter.ComponentFilter filter) {
            return null;
        }

        @Override
        public <T, K> it.water.core.api.registry.ComponentRegistration<T, K> registerComponent(
                Class<? extends T> componentClass, T component,
                it.water.core.api.registry.ComponentConfiguration configuration) {
            return null;
        }

        @Override
        public <T> boolean unregisterComponent(it.water.core.api.registry.ComponentRegistration<T, ?> registration) {
            return false;
        }

        @Override
        public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
            return false;
        }

        @Override
        public it.water.core.api.registry.filter.ComponentFilterBuilder getComponentFilterBuilder() {
            return null;
        }

        // entity-oriented lookups: irrelevant to the global dispatch, never called by these tests

        @Override
        public <T extends it.water.core.api.service.BaseEntitySystemApi> T findEntitySystemApi(String entityClassName) {
            return null;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public <T extends it.water.core.api.repository.BaseRepository> T findEntityRepository(String entityClassName) {
            return null;
        }

        @Override
        public <T extends it.water.core.api.model.BaseEntity> it.water.core.api.repository.BaseRepository<T> findEntityExtensionRepository(Class<T> type) {
            return null;
        }
    }

    static class Invocation {
        final Object target;
        final Method method;
        final Object[] args;

        Invocation(Object target, Method method, Object[] args) {
            this.target = target;
            this.method = method;
            this.args = args;
        }
    }

    static class RecordingBefore implements GlobalBeforeMethodInterceptor {
        final List<Invocation> calls = new ArrayList<>();

        @Override
        public <S extends Service> void interceptMethod(S destination, Method m, Object[] args) {
            calls.add(new Invocation(destination, m, args));
        }
    }

    static class RecordingAfter implements GlobalAfterMethodInterceptor {
        final List<Object> results = new ArrayList<>();

        @Override
        public <S extends Service> void interceptMethod(S destination, Method m, Object[] args, Object returnResult) {
            results.add(returnResult);
        }
    }

    /**
     * A second, distinct interceptor CLASS: needed to tell "same class, several instances" (collapses)
     * apart from "different concerns" (all run).
     */
    static class SecondRecordingBefore implements GlobalBeforeMethodInterceptor {
        int calls;

        @Override
        public <S extends Service> void interceptMethod(S destination, Method m, Object[] args) {
            calls++;
        }
    }

    static class ThrowingBefore implements GlobalBeforeMethodInterceptor {
        @Override
        public <S extends Service> void interceptMethod(S destination, Method m, Object[] args) {
            throw new IllegalStateException("global interceptor blew up");
        }
    }
}
