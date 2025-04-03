/*
 * Copyright 2025 Aristide Cittadino
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

package it.water.core.api.model;

/**
 * @Author Aristide Cittadino
 * Entity Extension point in which you specify the related type and the id.
 * This is another entity that is stored inside the database separately.
 */
public interface EntityExtension extends BaseEntity {

    /**
     *
     * @param extensionId the primary key of the extension since it's not possibile to pass it through external services
     * @param parentEntity the parent entity
     */
    void setupExtensionFields(long extensionId,BaseEntity parentEntity);

    /**
     * related entity id
     *
     * @return
     */
    long getRelatedEntityId();

    /**
     *
     * @param relatedEntityId
     */
    void setRelatedEntityId(long relatedEntityId);
}
