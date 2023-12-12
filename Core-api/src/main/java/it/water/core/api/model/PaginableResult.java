
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

package it.water.core.api.model;

import java.util.Collection;


/**
 * @Author Aristide Cittadino
 * @param <T> entity which must extend  Resource
 * Abstraction for a paginable result as a query output
 */
public interface PaginableResult<T extends Resource> {
    /**
     * @return num of pages
     */
    int getNumPages();

    /**
     * @return the current page
     */
    int getCurrentPage();

    /**
     * @return the next page
     */
    int getNextPage();

    /**
     * @return items per page
     */
    int getDelta();

    /**
     * @return query results
     */
    Collection<T> getResults();
}
