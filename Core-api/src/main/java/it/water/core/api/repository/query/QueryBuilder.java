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
package it.water.core.api.repository.query;

import it.water.core.api.repository.query.operands.FieldNameOperand;

public interface QueryBuilder {

    /**
     * Creates a filter from an expression ex. name LIKE 'mario' OR age < 50
     *
     * @param filter
     * @return
     */
    Query createQueryFilter(String filter);

    /**
     * Start a new composite query object from a field name.
     * Ex. qb.field("name").equalTo("Mario").or(qb.field("age").lessThan(50))
     *
     * @param name
     * @return
     */
    FieldNameOperand field(String name);
}
