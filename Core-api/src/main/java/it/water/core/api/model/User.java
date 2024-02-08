
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

/**
 * @Author Aristide Cittadino.
 * Interface for defining the concept of user.
 */
public interface User {
    /**
     * Identifies an user uniquely
     * @return
     */
    long getId();

    /**
     * Gets the HUser name
     *
     * @return a string that represents HUser name
     */
    String getName();

    /**
     * Gets the HUser lastname
     *
     * @return a string that represents HUser lastname
     */
    String getLastname();

    /**
     * Gets the HUser email
     *
     * @return a string that represents HUser email
     */
    String getEmail();

    /**
     * Gets the HUser username
     *
     * @return a string that represents HUser username
     */
    String getUsername();


    /**
     * Find if a role has been found by role name
     *
     * @param roleName parameter that indicates the Role name
     * @return true if a role has been found
     */
    boolean hasRole(String roleName);


    /**
     * Gets if HUser is administrator
     *
     * @return true if HUser is administrator
     */
    boolean isAdmin();
}
