
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

package it.water.core.api.interceptors;

import it.water.core.api.service.Service;

import java.lang.reflect.Method;


/**
 * @Author Aristide Cittadino
 * Post-processing hook invoked on EVERY intercepted service method, with no annotation to opt in.
 * See {@link GlobalBeforeMethodInterceptor} for the rationale, the mandatory self-scoping, and the
 * cheapness / fail-safety obligations, which apply here identically.
 * <p>
 * <b>Two mutually exclusive outcomes.</b> {@link #interceptMethod} runs after a normal return,
 * {@link #interceptError} after a failed invocation - exactly one of them fires per intercepted call.
 * Implementations that keep per-call state (an open span, a timer, a stack) MUST handle both, or the
 * state leaks on every exception.
 */
public interface GlobalAfterMethodInterceptor {
    /**
     * @param destination  Service which was invoked
     * @param m            Method invoked (see the scoping note on the before-hook)
     * @param args         Method arguments
     * @param returnResult Object returned by the method
     * @param <S>          Service Type
     */
    <S extends Service> void interceptMethod(S destination, Method m, Object[] args, Object returnResult);

    /**
     * Invoked when the intercepted method threw, INSTEAD of {@link #interceptMethod}. The exception is
     * always rethrown to the caller afterwards: this hook observes, it cannot swallow a failure.
     * <p>
     * Default no-op, so an implementation that only cares about successful calls stays valid - but note
     * that without it a failure is invisible, and any per-call state opened by the before-hook is never
     * released.
     *
     * @param destination Service which was invoked
     * @param m           Method invoked
     * @param args        Method arguments
     * @param error       the exception the method threw, already unwrapped from any reflective wrapper
     * @param <S>         Service Type
     */
    default <S extends Service> void interceptError(S destination, Method m, Object[] args, Throwable error) {
        // no-op by default
    }
}
