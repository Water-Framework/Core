
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

/**
 * @Author Aristide Cittadino.
 * Generic interface component for Resource.
 * This interface maps the concept of an abstract resource.
 */
public interface Resource {
    /**
     * Gets the resource name of  platform
     *
     * @return resource name of  platform
     */
    default String getResourceName() {
        return this.getClass().getName();
    }
}
