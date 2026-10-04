
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

package it.water.core.api.traffic.model;

/**
 * @Author Aristide Cittadino.
 * Specialization of {@link TrafficRecord} for method-style invocations captured at the
 * REST, Api, SystemApi and persistence layers. The HTTP-specific fields
 * (httpMethod/path/statusCode/clientIp) are populated only for REST records and are null
 * for Api/SystemApi/persistence invocations.
 */
public interface TrafficCallRecord extends TrafficRecord {
    String source();

    String destination();

    String operation();

    /**
     * {@code recordId} of the call this one was made from, or {@code null} for the outermost call of
     * a request.
     * <p>
     * The {@code correlationId} alone only says that a set of records belongs to the same request:
     * it cannot say who called whom. With the S2S capture enabled a single request produces one
     * record per hop (REST &rarr; Api &rarr; SystemApi &rarr; repository), so without this link the
     * result is a flat bag of siblings instead of a call tree.
     */
    String parentId();

    /**
     * Nesting level of this call within its request: {@code 0} for the outermost one, incremented at
     * every nested hop. Redundant with respect to the {@code parentId} chain, but it makes the common
     * queries ("only the entry points", "how deep does this request go") answerable without
     * reconstructing the tree.
     */
    int depth();

    String httpMethod();

    String path();

    Integer statusCode();

    String clientIp();

    String argsRef();
}
