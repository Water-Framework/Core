
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
 * Marks a resource with a many-to-many tenancy relationship with companies (an instance can belong
 * to several companies). Carries no {@code companyId} column: membership lives in a domain-owned
 * table in the entity's own module and is resolved by a {@code TenantMembershipResolver}.
 */
public interface MultiTenantResource extends BaseEntity {
}
