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

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.api.service.BaseEntitySystemApi;

public interface TestEntitySystemService extends BaseEntitySystemApi<TestProtectedEntity> {
    boolean genericPermissionMethod();

    boolean specificPermissionMethod(long resourceId);

    TestProtectedEntity permissionOnReturnMethod();

    boolean allowRolesMethod();

    @Override
    default TestProtectedEntity save(TestProtectedEntity entity) {
        return null;
    }

    @Override
    default TestProtectedEntity update(TestProtectedEntity entity) {
        return null;
    }

    @Override
    default void remove(long id) {

    }

    @Override
    default TestProtectedEntity find(long id) {
        return new TestProtectedEntity();
    }

    @Override
    default TestProtectedEntity find(Query filter) {
        return null;
    }

    @Override
    default PaginableResult<TestProtectedEntity> findAll(Query filter, int delta, int page, QueryOrder queryOrder) {
        return null;
    }

    @Override
    default long countAll(Query filter) {
        return 0;
    }

    @Override
    default Class<TestProtectedEntity> getEntityType() {
        return null;
    }

    @Override
    default QueryBuilder getQueryBuilderInstance() {
        return null;
    }
}
