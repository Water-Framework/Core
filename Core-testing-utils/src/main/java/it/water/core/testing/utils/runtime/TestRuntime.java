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
package it.water.core.testing.utils.runtime;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.permission.SecurityContext;
import it.water.core.testing.utils.security.TestSecurityContext;
import lombok.Getter;

public class TestRuntime implements Runtime {
    private SecurityContext context;
    @Getter
    private ApplicationProperties applicationProperties;

    public TestRuntime(ApplicationProperties applicationProperties) {
        this.context = TestSecurityContext.createContext(0, null, false, null);
        this.applicationProperties = applicationProperties;
    }
    @Override
    public SecurityContext getSecurityContext() {
        return context;
    }

    public void switchSecurityContext(TestSecurityContext testSecurityContext) {
        this.context = testSecurityContext;
    }

}
