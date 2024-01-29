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
package it.water.core.security.service;

import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.permission.annotations.AllowPermissions;

@FrameworkComponent(services = {TestEntityService1.class})
public class TestEntityServiceImpl1 implements TestEntityService1 {
    private TestProtectedEntity resource = new TestProtectedEntity();

    @AllowPermissions(actions = "GET", checkById = true, systemApiRef = "it.water.core.security.service.TestEntitySystemService")
    public boolean alternativeSpecificPermissionMethod(long resourceId) {
        return true;
    }

}
