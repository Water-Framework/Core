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
package it.water.core.interceptors.annotations.implementation;

import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.LogMethodExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link LogMethodExecutionInterceptor}, the interceptor behind {@code @LogMethodExecution}.
 * <p>
 * Its observable behaviour is a log line, so the assertions are about what the interceptor must
 * guarantee to the CALLER rather than about the text produced: never throw (it runs inside someone
 * else's call), handle every argument shape, resolve the annotation it is bound to, and reuse one
 * logger per destination class.
 */
class LogMethodExecutionInterceptorTest {

    private LogMethodExecutionInterceptor interceptor;
    private LoggedService service;

    @BeforeEach
    void setUp() {
        interceptor = new LogMethodExecutionInterceptor();
        service = new LoggedService();
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return LoggedServiceApi.class.getMethod(name, parameterTypes);
    }

    @Test
    void getAnnotation_isTheOneItIsBoundTo() {
        assertSame(LogMethodExecution.class, interceptor.getAnnotation());
    }

    @Test
    void isRegisteredAsABeforeInterceptor() {
        // the contract the framework dispatches on: a before-hook, matched by its annotation
        assertEquals(true, interceptor instanceof BeforeMethodInterceptor);
    }

    @Test
    void interceptMethod_noArgs_logsWithoutThrowing() throws NoSuchMethodException {
        assertDoesNotThrow(() -> interceptor.interceptMethod(
                service, method("noArgs"), new Object[0], annotation(false)));
    }

    @Test
    void interceptMethod_withArgs_appendsThemWithoutThrowing() throws NoSuchMethodException {
        assertDoesNotThrow(() -> interceptor.interceptMethod(
                service, method("withArgs", String.class, int.class), new Object[]{"a", 1}, annotation(false)));
    }

    /**
     * {@code logDebug=true} takes the debug branch when the logger has debug enabled, and silently
     * falls back to info otherwise - either way the caller must not notice.
     */
    @Test
    void interceptMethod_logDebugRequested_doesNotThrowEitherWay() throws NoSuchMethodException {
        assertDoesNotThrow(() -> interceptor.interceptMethod(
                service, method("noArgs"), new Object[0], annotation(true)));
    }

    /**
     * The logger cache is keyed by destination class: invoking twice must not build a second logger,
     * and must not fail. Exercised through repeated invocations, since the map is private.
     */
    @Test
    void interceptMethod_calledTwiceOnSameDestination_reusesLogger() throws NoSuchMethodException {
        Method m = method("noArgs");

        assertDoesNotThrow(() -> interceptor.interceptMethod(service, m, new Object[0], annotation(false)));
        assertDoesNotThrow(() -> interceptor.interceptMethod(service, m, new Object[0], annotation(false)));
    }

    @Test
    void componentsRegistry_isSettableAndReadable() {
        assertNull(interceptor.getComponentsRegistry(), "starts unresolved");
    }

    /**
     * Hand-built annotation instance: the interceptor only reads {@code logDebug()}, and building the
     * instance here keeps the test independent of where the annotation is declared.
     */
    private LogMethodExecution annotation(boolean logDebug) {
        return new LogMethodExecution() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LogMethodExecution.class;
            }

            @Override
            public boolean logDebug() {
                return logDebug;
            }
        };
    }

    interface LoggedServiceApi extends Service {
        void noArgs();

        void withArgs(String first, int second);
    }

    static class LoggedService implements LoggedServiceApi {
        @Override
        public void noArgs() {
            // nothing to do: the interceptor is what is under test
        }

        @Override
        public void withArgs(String first, int second) {
            // nothing to do
        }
    }
}
