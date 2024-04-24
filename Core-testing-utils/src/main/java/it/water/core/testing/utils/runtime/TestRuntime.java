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

import it.water.core.api.bundle.Runtime;
import it.water.core.api.permission.SecurityContext;
import it.water.core.bundle.WaterRuntime;
import it.water.core.interceptors.annotations.FrameworkComponent;

/**
 * @Author Aristide Cittadino
 * During the test phase thread local may fail to associate the right security context to requests.
 * With this componente used onlu for test purpose we should overcome this problem.
 * This component is useful specially when tests are executed outside the junit thread for example karate.
 */
@FrameworkComponent(priority = 1, services = {Runtime.class})
public class TestRuntime extends WaterRuntime implements Runtime {
    private SecurityContext securityContext;

    /**
     * Test runtime starts with admin security context set, in order to simplify rest tests.
     */
    public TestRuntime() {
        fillSecurityContext(new SecurityContext() {
            @Override
            public String getLoggedUsername() {
                return "test-admin";
            }

            @Override
            public boolean isLoggedIn() {
                return true;
            }

            @Override
            public boolean isAdmin() {
                return true;
            }

            @Override
            public long getLoggedEntityId() {
                return Integer.MAX_VALUE;
            }
        });
    }

    @Override
    public SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    public void fillSecurityContext(SecurityContext securityContext) {
        this.securityContext = securityContext;
    }

}
