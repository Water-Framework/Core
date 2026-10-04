
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
 * Excludes a method - or every method of a component - from the Service-to-Service traffic capture.
 * <p>
 * The S2S capture is <b>opt-out</b>: once {@code water.traffic.s2s.enabled} is on, every method a
 * component exposes through its interfaces is reported. That is the point - telemetry you have to
 * remember to enable per method is telemetry you will not have where you need it - but a few methods
 * are pure noise or pure cost, and this annotation is how you say so at the source instead of
 * maintaining a list of patterns far away from the code.
 * <p>
 * Use it for: very high frequency accessors whose record carries no information, methods handling
 * data you do not want even referenced in telemetry, and anything on the traffic pipeline's own path.
 * <p>
 * For exclusions that belong to a deployment rather than to the code, prefer the
 * {@code water.traffic.s2s.exclude} property: it takes class or class#method prefixes and needs no
 * recompilation. This annotation and that property are additive - either one excludes.
 * <p>
 * Applied to a TYPE it excludes every method of that component; applied to a METHOD it excludes that
 * method only. Placing it on an interface method excludes it for every implementation.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoTraffic {
}
