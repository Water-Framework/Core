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

import java.util.Map;

/**
 * @Author Aristide Cittadino
 * This is an extension of BaseEntity allowing the programmer to extend exisiting framework entities.
 * The developer can register an EntityExtesion of an Expandable entity with 2 informations:
 * 1) related type: the entity type the expandable entity is referred to
 * 2) extension type: the type of the entity which implements the expansion.
 *
 * extraFields Map allows to pass more data that can be converted to the specific entity before persistence.
 *
 * <p>
 * For example suppose you have User entity with specific field and you want to add more fields.
 * You can develop a new module with an Extension Point with a new Entity ex. UserExpansion in which
 * you specify new fields. Then the framework does the magic for you.
 * <p>
 * This is just a marker interface in order to make sure that the developer wants to make its own entity exapandable.
 */
public interface ExpandableEntity extends BaseEntity {
    /**
     * Used to have an extension point for entities
     * This map is used in order to receive dynamic fields eventually from outside for example web.
     * This is for "external" use
     * @return
     */
    Map<String, Object> getExtraFields();

    /**
     * Used to have an extension point for entities
     *
     * @param extraFields
     */
    void setExtraFields(Map<String, Object> extraFields);

    /**
     * Used to convert the map in a concrete object that must be saved inside the repository.
     * This object is for internal use. In some point of the framework the Map<String,Object> is converted
     * to EntityExtension
     * @return
     */
    EntityExtension getExtension();

    /**
     *
     * @param extension
     */
    void setExtension(EntityExtension extension);
}
