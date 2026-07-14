
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

package it.water.core.api.entity.tenant;

import it.water.core.api.model.BaseEntity;


/**
 * @Author Aristide Cittadino.
 * This interface tells Water system that a specific resource which implements this interface
 * belongs to a single company (tenant). The system scopes access to these resources on the
 * active company found in the SecurityContext and, when a new tenant resource is created,
 * automatically assigns to it the currently active company id.
 * This prevents malicious users from creating or accessing entities across tenants.
 * <p>
 * The company id is an opaque {@link Long} (NOT a JPA relationship to a Company entity):
 * tenancy is managed inside the entity's own module, never coupled to a central Company table.
 * A {@code null} company id means a global / cross-tenant instance (e.g. a global role) that is
 * visible regardless of the active company.
 */
public interface TenantResource extends BaseEntity {
    static final String COMPANY_ID_FIELD_NAME = "companyId";

    /**
     * @return the company (tenant) this entity belongs to, or null if it is global / cross-tenant
     */
    Long getCompanyId();

    void setCompanyId(Long companyId);
}
