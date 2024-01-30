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

package it.water.core.testing.utils.api;

import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionManager;

/**
 * @Author Aristide Cittadino
 * Simple Permission Manager which supports operation of user management for test purpose.
 */
public interface TestPermissionManager extends PermissionManager {
    User addUser(String username, String name, String lastname, String email);
    void removeUser(String username);
    User findUser(String username);
}
