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

package it.water.core.api.user;

import it.water.core.api.model.User;
import it.water.core.api.service.Service;

import java.util.Collection;

/**
 * @Author Aristide Cittadino
 * Generic service for user management.
 */
public interface UserManager extends Service {

    User addUser(String username, String name, String lastname, String email,String password,String salt, boolean isAdmin);

    void removeUser(String username);

    User findUser(String username);

    Collection<User> all();
}
