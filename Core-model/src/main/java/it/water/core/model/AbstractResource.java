
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

package it.water.core.model;


import it.water.core.api.model.Resource;

/**
 * @Author Aristide Cittadino.
 * Model class for AbstractResource.
 * This class implements Resource methods to obtain the resource name
 * of the entity mapped to the database.
 */
public abstract class AbstractResource implements Resource {

    /**
     * Gets the resource name of entity of  platform
     */
    @Override
    public String getResourceName() {
        return this.getClass().getName();
    }
}
