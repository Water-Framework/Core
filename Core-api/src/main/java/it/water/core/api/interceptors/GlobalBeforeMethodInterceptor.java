
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
 * Pre-processing hook invoked on EVERY intercepted service method, with no annotation to opt in.
 * <p>
 * This is the cross-cutting counterpart of {@link BeforeMethodInterceptor}: the annotation-driven
 * variant answers "this method asked to be intercepted", this one answers "everything is
 * intercepted unless it says otherwise". It exists for concerns that are worthless when they have
 * to be remembered one method at a time - observability being the obvious case: telemetry you must
 * annotate is telemetry you will forget exactly where it matters.
 * <p>
 * <b>Scoping is the implementation's job, and it is not optional.</b> The framework offers every
 * intercepted method: it cannot guess which ones matter to an arbitrary cross-cutting concern. Two
 * consequences follow. First, the runtimes offer different sets - OSGi proxies interfaces only, while
 * the Spring pointcut also matches the target's own methods - so an implementation that does not
 * scope itself explicitly will behave differently in each. Second, Water's own plumbing is made of
 * {@link Service}s too (the interceptors themselves are, and the chain calls them while dispatching),
 * so an unscoped implementation ends up observing the machinery that is observing - recursively. The
 * traffic capture solves both by accepting only methods declared on the four architectural layers
 * (RestApi, Api, SystemApi, repository), which is also what makes its output the flow of a request
 * rather than a log of everything.
 * <p>
 * <b>Implementations must be cheap and fail-safe.</b> They sit on the hottest path there is - every
 * call to every service - so they are expected to short-circuit on their own configuration before
 * doing any work, and to never propagate an exception to the caller: a global interceptor that
 * throws would break unrelated business code.
 */
public interface GlobalBeforeMethodInterceptor {
    /**
     * @param destination Service which is going to be invoked
     * @param m           Method being invoked (an interface method under a JDK proxy, possibly the
     *                    concrete one under Spring - see the scoping note above)
     * @param args        Method arguments
     * @param <S>         Service Type
     */
    <S extends Service> void interceptMethod(S destination, Method m, Object[] args);
}
