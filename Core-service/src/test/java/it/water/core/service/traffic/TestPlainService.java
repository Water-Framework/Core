
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

import it.water.core.api.service.Service;

/**
 * A plain {@code Service}: NOT a {@code RestApi}, {@code BaseApi}, {@code BaseSystemApi} nor a
 * repository. Proxied and intercepted like any Water service, and precisely for that reason it is the
 * proof that the S2S capture is scoped to the four architectural layers rather than to "everything
 * that is proxied" - which is what keeps the framework's own plumbing (interceptors, the traffic
 * pipeline itself) out of the telemetry without any exclusion list.
 */
public interface TestPlainService extends Service {
    String doInternalWork(String input);
}
