
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

package it.water.core.api.repository;


import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.api.service.Service;

/**
 * @param <T> parameter that indicates a generic class which must extend Water Entity
 *            Interface component for WaterBaseRepository.
 *            This interface defines the methods for basic CRUD operations.
 * @Author Aristide Cittadino.
 */
public interface BaseRepository<T extends BaseEntity> extends Service {

    /**
     * Save an entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @return entity saved
     */
    T persist(T entity);

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
     * @param entity
     */
    void remove(T entity);

    /**
     * Remove an entity in database
     *
     * @param ids parameter that indicates a list of entity ids
     */
    void removeAllByIds(Iterable<Long> ids);

    /**
     * @param entities
     */
    void removeAll(Iterable<T> entities);

    /**
     * Removes all entities
     */
    void removeAll();

    /**
     * @param id entity id
     * @return Entity found or runtime Exception
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
     * Find an existing entity in database
     *
     * @param filterStr filter as string in form <field>=<value> AND|OR ....
     * @return Entity if found
     */
    T find(String filterStr);

    /**
     * @param delta
     * @param page
     * @param filter
     * @param queryOrder
     * @return
     */
    PaginableResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder);


    /**
     * @param filter filter
     * @return total number of entities based on the given filter.
     */
    long countAll(Query filter);

    /**
     *
     * @return a valid query builder instance for the specific repository type
     */
    QueryBuilder getQueryBuilderInstance();
}
