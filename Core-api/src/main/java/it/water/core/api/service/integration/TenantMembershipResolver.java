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

package it.water.core.api.service.integration;

import java.util.Set;

/**
 * @Author Aristide Cittadino.
 * Resolves which instances of a {@code MultiTenantResource} (an entity with a many-to-many
 * relationship with companies, e.g. users belonging to several companies) belong to a given
 * company. The tenant enforcement seam in the Api layer uses this resolver to scope query results
 * for M:N tenant entities, the same way {@link SharedEntityIntegrationClient} scopes shared ones.
 * <p>
 * The implementation lives in the entity's own module (it owns the domain membership table) and is
 * selected by resource name via {@link #supports(String)}, so tenancy is never coupled to a central
 * Company table.
 */
public interface TenantMembershipResolver extends EntityIntegrationClient {

    /**
     * @param entityResourceName fully-qualified class name of the entity being scoped
     * @return true if this resolver can resolve membership for the given entity type
     */
    boolean supports(String entityResourceName);

    /**
     * @param entityResourceName fully-qualified class name of the M:N tenant entity
     * @param companyId          the active company (tenant) id
     * @return the ids of the entity instances that belong to the given company (empty if none)
     */
    Set<Long> getEntityIdsInCompany(String entityResourceName, long companyId);
}
