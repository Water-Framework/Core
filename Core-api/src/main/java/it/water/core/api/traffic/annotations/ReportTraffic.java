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

package it.water.core.api.traffic.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Aristide Cittadino.
 * Customizes how a Service method appears in the Service-to-Service (S2S) traffic capture.
 * <p>
 * <b>This annotation no longer turns capture on.</b> The S2S capture is opt-out: with
 * {@code water.traffic.s2s.enabled} on, every method exposed through a component's interfaces is
 * reported whether or not it carries this annotation - annotating what to observe means missing
 * exactly the calls nobody thought about. Use {@code @NoTraffic} to exclude, and this annotation only
 * to override what is reported.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReportTraffic {
    /**
     * Logical operation name reported on the traffic record. When left blank the intercepted
     * method name is used as the operation.
     *
     * @return the operation label, or empty string to fall back to the method name
     */
    String operation() default "";
}
