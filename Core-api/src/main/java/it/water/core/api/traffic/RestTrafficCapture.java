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

package it.water.core.api.traffic;

/**
 * @Author Aristide Cittadino.
 * Runtime-agnostic entry point for capturing INCOMING REST calls into the traffic pipeline.
 * <p>
 * REST is the one source Water interceptors cannot observe (they only hook Service methods), so it
 * needs a per-runtime adapter: a JAX-RS request/response filter under CXF, a {@code HandlerInterceptor}
 * under Spring. Those adapters are thin on purpose - they only extract the raw HTTP facts from their
 * own request context and hand them here; ALL the shared behaviour (enablement, path normalization,
 * exclusions, identity enrichment, outcome and record building) lives behind this contract, so the
 * two runtimes cannot drift apart.
 * <p>
 * Implementations must never throw: a telemetry failure can never break the HTTP request being served.
 */
public interface RestTrafficCapture {

    /**
     * Whether REST capture is currently active. Adapters should check this BEFORE doing any work of
     * their own (e.g. reading the client IP off the request), so that a disabled pipeline costs
     * essentially nothing on the request path.
     *
     * @return true when the traffic pipeline is enabled AND REST capture is turned on
     */
    boolean isEnabled();

    /**
     * Opens the correlation scope for an incoming request, on the thread that will serve it. Every
     * record produced while serving that request - the REST call itself, the service-to-service
     * invocations it triggers, the domain events they emit - inherits the same correlation id
     * (ADR-7), and head-based sampling then keeps or drops the tree as a whole (ADR-5).
     * <p>
     * Adapters call it on the way in, right where they start timing; {@link #captureRestCall} closes
     * the scope on the way out.
     */
    void requestStarted();

    /**
     * Reports one completed incoming REST call, and closes the correlation scope opened by
     * {@link #requestStarted()} - always, even when the call is not reported at all.
     *
     * @param httpMethod     HTTP verb, e.g. {@code GET}
     * @param path           request path as seen by the runtime; the {@code /water} base context is
     *                       stripped by the implementation, so adapters may pass it either way
     * @param statusCode     HTTP status of the response, null when the runtime cannot provide one
     * @param clientIp       caller IP, taken from the runtime request context by the adapter (it is
     *                       NOT read from the security context)
     * @param durationMillis wall-clock duration of the call
     * @param error          exception that aborted the call, or null on the success path
     */
    void captureRestCall(String httpMethod, String path, Integer statusCode, String clientIp,
                         long durationMillis, Throwable error);
}
