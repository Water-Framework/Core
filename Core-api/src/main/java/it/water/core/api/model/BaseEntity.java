
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

package it.water.core.api.model;

import java.util.Date;


/**
 * @Author Aristide Cittadino.
 * Generic Interface Component for BaseEntity.
 * This interface defines methods for obtaining and setting the entity id of the  platform.
 */
public interface BaseEntity extends Resource {
    /**
     * Gets the entity id
     *
     * @return entity id
     */
    long getId();

    /**
     * @return the creation timestamp
     */
    Date getEntityCreateDate();

    /**
     * @return the entity update timestamp
     */
    Date getEntityModifyDate();

    /**
     * @return the entity version
     */
    Integer getEntityVersion();

    /**
     * @return set entity version
     */
    void setEntityVersion(Integer entityVersion);

    /**
     * Checks wheter the entity is expandable
     *
     * @return
     */
    default boolean isExpandableEntity() {
        return ExpandableEntity.class.isAssignableFrom(this.getClass());
    }

    /**
     * Used to reference categories with AssetCategoryManager
     * @return categoryIds
     */
    default long[] getCategoryIds() {
        return new long[0];
    }

    /**
     * @param categoryIds category Ids
     */
    default void setCategoryIds(long[] categoryIds) {
        // default: no-op, override in AbstractEntity
    }

    /**
     * Used to reference tags with AssetTagManager
     * @return tagIds
     */
    default long[] getTagIds() {
        return new long[0];
    }

    /**
     * @param tagIds tag Ids
     */
    default void setTagIds(long[] tagIds) {
        // default: no-op, override in AbstractEntity
    }
}
