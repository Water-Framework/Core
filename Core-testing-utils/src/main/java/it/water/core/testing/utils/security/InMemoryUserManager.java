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

package it.water.core.testing.utils.security;

import it.water.core.api.model.User;
import it.water.core.api.service.integration.UserIntegrationClient;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.testing.utils.api.TestUserManager;

import java.util.*;

@FrameworkComponent(priority = 0,services = {TestUserManager.class,UserIntegrationClient.class})
public class InMemoryUserManager implements TestUserManager, UserIntegrationClient {
    private static int userCounter = 1;
    Set<User> users = new HashSet<>();

    public InMemoryUserManager() {
        users.add(new User() {
            @Override
            public long getId() {
                return userCounter++;
            }

            @Override
            public String getName() {
                return "admin";
            }

            @Override
            public String getLastname() {
                return "admin";
            }

            @Override
            public String getEmail() {
                return "admin@water.com";
            }

            @Override
            public String getUsername() {
                return "admin";
            }

            @Override
            public boolean isAdmin() {
                return true;
            }
        });
    }

    @Override
    public User addUser(String username, String name, String lastname, String email, boolean isAdmin) {
        User u = new User() {
            private long id = userCounter++;

            @Override
            public long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getLastname() {
                return lastname;
            }

            @Override
            public String getEmail() {
                return email;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public boolean isAdmin() {
                return isAdmin;
            }

            @Override
            public int hashCode() {
                return username.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof User))
                    return false;
                return ((User) obj).getUsername().equals(this.getUsername());
            }
        };
        users.add(u);
        return u;
    }

    @Override
    public void removeUser(String username) {
        User u = findUser(username);
        if (u != null)
            users.remove(u);
    }

    @Override
    public User findUser(String username) {
        Optional<User> userOpt = this.users.stream().filter(user -> user.getUsername().equals(username)).findFirst();
        if (userOpt.isPresent())
            return userOpt.get();
        return null;
    }

    @Override
    public Collection<User> all() {
        return Collections.unmodifiableSet(users);
    }

    @Override
    public User fetchUserByUsername(String username) {
        return findUser(username);
    }
}
