
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.core.api.repository.query;

import java.util.List;


/**
 * @Author Aristide Cittadino.
 * This interface represent the abstract concept of a query.
 * The main goal is to abstract the way the end user performs query from the concrete technology
 * where entities are saved.
 */
public interface Query {
    /**
     * @param operands list of operands AND|OR|==|!=....
     */
    void defineOperands(Query... operands);

    /**
     * @return a String definition which is an abstraction of the query, for example:
     * "name == 'Mario' AND lastname == 'Rossi' OR age <= 25"
     */
    String getDefinition();

    /**
     * @param rightQuery
     */
    Query and(Query rightQuery);

    /**
     * @param rightQuery
     */
    Query or(Query rightQuery);

    /**
     * @param values
     */
    Query in(List<?> values);

    /**
     *
     */
    Query not();

}
