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

package it.water.core.bundle;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.permission.SecurityContext;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;

/**
 * @Author Aristide Cittadino.
 * Component which exposes global information such as : Security Context and application properties
 */
@FrameworkComponent
public class WaterRuntime implements Runtime {
    private static ThreadLocal<SecurityContext> currentThreadSecurityContext = new ThreadLocal<>();

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Override
    public SecurityContext getSecurityContext() {
        return currentThreadSecurityContext.get();
    }

    @Override
    public void fillSecurityContext(SecurityContext securityContext) {
        currentThreadSecurityContext.remove();
        currentThreadSecurityContext.set(securityContext);
    }

    @Override
    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }
}
