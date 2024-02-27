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

package it.water.core.api.bundle;

import it.water.core.api.permission.SecurityContext;

/**
 * @Author Aristide Cittadino.
 * Wraps the concept of concrete runtime
 */
public interface Runtime {
    /**
     * @return Current Security Context
     */
    SecurityContext getSecurityContext();

    /**
     * Insert current security context
     *
     * @return
     */
    void fillSecurityContext(SecurityContext securityContext);

    /**
     * @return Application defined properties
     */
    ApplicationProperties getApplicationProperties();

}
