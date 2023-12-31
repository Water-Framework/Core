
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

package it.water.core.api.service;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;


/**
 * @param <T> parameter that indicates a generic class
 *            Interface component for WaterBaseEntityApi. This interface defines the methods for basic CRUD operations.
 *            These methods are reusable by all entities that interact with the framework.
 * @Author Aristide Cittadino.
 */
public interface BaseEntitySystemApi<T extends BaseEntity> extends BaseSystemApi {

    /**
     * Save an entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @return entity saved
     */
    T save(T entity);

    /**
     * Update an existing entity in database
     *
     * @param entity parameter that indicates a generic entity
     */
    T update(T entity);

    /**
     * Remove an entity in database
     *
     * @param id parameter that indicates a entity id
     */
    void remove(long id);

    /**
     * @param id
     * @return
     */
    T find(long id);


    /**
     * Find an existing entity in database
     *
     * @param filter filter
     * @return Entity if found
     */
    T find(Query filter);


    /**
     * Find all entity in database
     *
     * @param queryOrder parameter that define order's criteria
     * @param delta
     * @param page
     * @return Collection of entity
     */
    PaginableResult<T> findAll(Query filter, int delta, int page, QueryOrder queryOrder);

    /**
     * @param filter filter
     * @return total number of entities based on the given filter.
     */
    long countAll(Query filter);

    /**
     * Return current entity type
     */
    Class<T> getEntityType();

    /**
     *
     * @return a valid query builder instance for the specific repository type
     */
    QueryBuilder getQueryBuilderInstance();

}
