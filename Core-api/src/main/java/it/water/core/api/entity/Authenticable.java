
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

package it.water.core.api.entity;


import it.water.core.api.model.BaseEntity;

import java.util.Collection;


/**
 * @Author Aristide Cittadino.
 * This interface marks an entity to be "authenticable".
 * This means that it must expose methods for getting password and passwordConfirm fields.
 * This interface is useful if you want exploits password validation for
 * your entity. This enables sensor, devices, users to connect to your Platform.
 */
public interface Authenticable extends BaseEntity {

    /**
     * @return the field name which contains the screenName
     */
    String getScreenNameFieldName();

    /**
     * @return the username or thingname
     */
    String getScreenName();

    /**
     * @return true if it is an admin user
     */
    boolean isAdmin();

    /**
     * @return
     */
    Collection<String> getRoles();

    /**
     * @return the password
     */
    String getPassword();

    /**
     * @return the confirm password
     */
    String getPasswordConfirm();

    /**
     * @return true if the authenticable is activated for authentication
     */
    boolean isActive();
}
