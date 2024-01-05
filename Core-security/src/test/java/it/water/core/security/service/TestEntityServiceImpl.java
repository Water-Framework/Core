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

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.permission.action.WebAPIAction;
import it.water.core.permission.annotations.AllowGenericPermissions;
import it.water.core.permission.annotations.AllowPermissions;
import it.water.core.permission.annotations.AllowPermissionsOnReturn;
import it.water.core.permission.annotations.AllowRoles;

@FrameworkComponent(services = {TestEntityService.class})
public class TestEntityServiceImpl implements TestEntityService {
    private TestProtectedEntity resource = new TestProtectedEntity();

    @AllowGenericPermissions(actions = WebAPIAction.ActionNames.GET, resourceName = "it.water.core.security.TestProtectedResource")
    public boolean genericPermissionMethod() {
        return true;
    }

    @AllowGenericPermissions(actions = WebAPIAction.ActionNames.GET, resourceName = "it.water.core.security.TestProtectedResource")
    public boolean genericPermissionMethodWithoutResourceName() {
        return true;
    }

    @AllowPermissions(actions = WebAPIAction.ActionNames.GET, checkById = true)
    public boolean specificPermissionMethod(long resourceId) {
        return true;
    }

    @AllowPermissions(actions = WebAPIAction.ActionNames.GET)
    public boolean specificPermissionMethodWithoutIdIndex(TestProtectedEntity resourceId) {
        return true;
    }

    @AllowPermissions(actions = WebAPIAction.ActionNames.GET, checkById = true, systemApiRef = "it.water.core.security.service.TestEntitySystemService")
    public boolean specificPermissionMethodWithSystemApi(long resourceId) {
        return true;
    }

    @AllowPermissionsOnReturn(actions = WebAPIAction.ActionNames.GET)
    public TestProtectedEntity permissionOnReturnMethod() {
        TestProtectedEntity r = new TestProtectedEntity();
        return r;
    }

    @AllowRoles(rolesNames = "ROLE")
    public boolean allowRolesMethod() {
        return true;
    }

    @Override
    public TestProtectedEntity save(TestProtectedEntity entity) {
        return null;
    }

    @Override
    public TestProtectedEntity update(TestProtectedEntity entity) {
        return null;
    }

    @Override
    public void remove(long id) {

    }

    @Override
    public TestProtectedEntity find(long id) {
        return resource;
    }

    @Override
    public TestProtectedEntity find(Query filter) {
        return null;
    }

    @Override
    public PaginableResult<TestProtectedEntity> findAll(Query filter, int delta, int page, QueryOrder queryOrder) {
        return null;
    }

    @Override
    public long countAll(Query filter) {
        return 0;
    }

    @Override
    public Class<TestProtectedEntity> getEntityType() {
        return null;
    }
}
