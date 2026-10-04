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

import it.water.core.api.service.BaseApi;

/**
 * Test-only Service-to-Service (S2S) API used by {@link S2STrafficCaptureHarnessTest} to exercise
 * {@code @ReportTraffic} through a genuine {@code TestServiceProxy}-proxied invocation (resolved,
 * proxied, from the {@code ComponentRegistry} - see {@code TestComponentRegistry#registerComponent}).
 * Registered manually (not {@code @FrameworkComponent}) from the harness test's {@code @BeforeAll}.
 */
public interface TestS2SApi extends BaseApi {
    String doWork(String input);

    String doWorkDefaultOperation(String input);

    String doWorkThrows(String input);

    /**
     * Carries NO annotation whatsoever: it is the proof that capture is opt-out, i.e. that a method
     * nobody thought about is reported anyway.
     */
    String doPlainWork(String input);

    /**
     * Excluded at the source with {@code @NoTraffic}.
     */
    String doSilentWork(String input);

    /**
     * Calls into the SystemApi, producing a nested record whose {@code parentId} must be this call's
     * record and whose {@code depth} must be one deeper.
     */
    String doNestedWork(String input);
}
