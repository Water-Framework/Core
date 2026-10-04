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

package it.water.core.service.traffic;

import java.util.UUID;

/**
 * @Author Aristide Cittadino.
 * Per-thread correlation scope of the traffic pipeline (F4, ADR-7).
 * <p>
 * A single incoming request legitimately produces SEVERAL records - the REST call itself, the
 * service-to-service invocations it fans out to, the domain events they emit - and ADR-7 keeps them
 * as distinct, correlated records rather than one aggregate. This context is what correlates them:
 * the REST adapter opens a scope when the request comes in, every record reported on that thread
 * inherits its {@code correlationId}, and the scope is closed when the response is on its way out.
 * <p>
 * It is also what makes head-based sampling coherent (ADR-5): sampling the {@code correlationId}
 * rather than each record means a request tree is either captured whole or not at all - a sampled
 * REST call is never left with its children missing.
 * <p>
 * <b>Leak safety.</b> {@link #open()} always OVERWRITES any value left behind on the thread, so a
 * pooled thread whose previous scope was never closed (a runtime that skips the response hook, an
 * async dispatch) self-heals on its next request instead of silently correlating two unrelated
 * requests forever. {@link #close()} is still the contract, and callers should invoke it in a
 * {@code finally}.
 * <p>
 * Outside any scope {@link #current()} returns null and each record simply keeps its own identity:
 * correlation is an enrichment, never a requirement.
 */
public final class TrafficCorrelationContext {

    private static final ThreadLocal<String> CORRELATION_ID = new ThreadLocal<>();

    private TrafficCorrelationContext() {
        // utility holder, no instances
    }

    /**
     * Opens a fresh correlation scope on the current thread, replacing any previous one.
     *
     * @return the newly generated correlation id
     */
    public static String open() {
        String correlationId = UUID.randomUUID().toString();
        CORRELATION_ID.set(correlationId);
        return correlationId;
    }

    /**
     * Joins an existing correlation scope, e.g. one propagated by an upstream service through a
     * header. A null or blank value opens a fresh scope instead of leaving the thread uncorrelated.
     *
     * @param correlationId the correlation id to adopt
     * @return the correlation id actually in force
     */
    public static String open(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            return open();
        }
        CORRELATION_ID.set(correlationId);
        return correlationId;
    }

    /**
     * @return the correlation id in force on this thread, or null when no scope is open
     */
    public static String current() {
        return CORRELATION_ID.get();
    }

    /**
     * Closes the scope and releases the ThreadLocal. Safe to call when no scope is open.
     */
    public static void close() {
        CORRELATION_ID.remove();
    }
}
