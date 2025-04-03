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

package it.water.core.registry;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.EntityExtensionService;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;

/**
 * @Author Aristide Cittadino
 * Abstract layer of all component registries
 */
public abstract class AbstractComponentRegistry implements ComponentRegistry {

    /**
     * Finds the system service related to the extension entity
     *
     * @return
     */
    public <T extends BaseEntity> BaseRepository<T> findEntityExtensionRepository(Class<T> type) {
        try {
            ComponentFilter filter = this.getComponentFilterBuilder().createFilter(EntityExtensionService.RELATED_ENTITY_PROPERTY, type.getName());
            EntityExtensionService entityExtensionService = this.findComponent(EntityExtensionService.class, filter);
            if (entityExtensionService != null) {
                log.debug("Found entity extension :{} for entity type: {} ", entityExtensionService.type(), entityExtensionService.relatedType().getName());
                //find the system api of the extension in order to propagate changes
                return this.findEntityRepository(entityExtensionService.type().getName());
            }
        } catch (NoComponentRegistryFoundException ex) {
            log.debug("No extension point found fot entity {}", type.getName(), ex);
        }
        return null;
    }
}
