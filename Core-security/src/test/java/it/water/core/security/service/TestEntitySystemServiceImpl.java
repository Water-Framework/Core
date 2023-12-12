/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.core.security.service;

import it.water.core.interceptors.annotations.FrameworkComponent;

@FrameworkComponent(services = {TestEntitySystemService.class})
public class TestEntitySystemServiceImpl implements TestEntitySystemService {
    private TestProtectedEntity resource = new TestProtectedEntity();

    public boolean genericPermissionMethod() {
        return true;
    }

    public boolean specificPermissionMethod(long resourceId) {
        return true;
    }

    public TestProtectedEntity permissionOnReturnMethod() {
        TestProtectedEntity r = new TestProtectedEntity();
        return r;
    }

    public boolean allowRolesMethod() {
        return true;
    }

}
