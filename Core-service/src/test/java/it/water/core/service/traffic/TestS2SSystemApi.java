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

import it.water.core.api.service.BaseSystemApi;

/**
 * Test-only "SystemApi"-named interface: {@link ReportTrafficAfterInterceptor}'s heuristic
 * classifies a call as {@code RecordType.SYSTEM_API} when the concrete service directly implements
 * an interface whose simple name ends with {@code "SystemApi"} (see
 * {@code it.water.core.service.traffic.interceptors.ReportTrafficAfterInterceptor#isSystemApi}).
 */
public interface TestS2SSystemApi extends BaseSystemApi {
    String doSystemWork(String input);
}
