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
import it.water.core.api.user.UserManager;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.testing.utils.api.TestUserManager;
import lombok.Setter;

import java.util.*;

@FrameworkComponent(priority = 0, services = {UserManager.class, TestUserManager.class, UserIntegrationClient.class})
public class InMemoryUserManager implements UserManager, TestUserManager, UserIntegrationClient {
    private static int userCounter = 1;
    private static final String ADMIN = "admin";
    Set<User> users = new HashSet<>();
    @Inject
    @Setter
    private UserIntegrationClient userIntegrationClient;

    public InMemoryUserManager() {
        long userId = getUserCounterAndIncrement();
        users.add(new User() {
            private final long id = userId;

            @Override
            public long getId() {
                return id;
            }

            @Override
            public String getName() {
                return ADMIN;
            }

            @Override
            public String getLastname() {
                return ADMIN;
            }

            @Override
            public String getEmail() {
                return ADMIN;
            }

            @Override
            public String getUsername() {
                return ADMIN;
            }

            @Override
            public boolean isAdmin() {
                return true;
            }
        });
    }

    @Override
    public User addUser(String username, String name, String lastname, String email, String password, String salt, boolean isAdmin) {
        long userId = getUserCounterAndIncrement();
        User u = new User() {
            private final long id = userId;

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
        //if user integration client is this component avoid stackoverflow
        if (userIntegrationClient.equals(this))
            return findUser(username);
        return userIntegrationClient.fetchUserByUsername(username);
    }

    @Override
    public User fetchUserByEmailAddress(String emailAddress) {
        //if user integration client is this component avoid stackoverflow
        if (userIntegrationClient.equals(this))
            return users.stream().filter(user -> user.getEmail().equalsIgnoreCase(emailAddress)).findAny().orElse(null);
        return userIntegrationClient.fetchUserByEmailAddress(emailAddress);
    }

    @Override
    public User fetchUserByUserId(long userId) {
        //if user integration client is this component avoid stackoverflow
        if (userIntegrationClient.equals(this))
            return users.stream().filter(user -> user.getId() == userId).findAny().orElse(null);
        return userIntegrationClient.fetchUserByUserId(userId);
    }

    private static long getUserCounterAndIncrement(){
        return userCounter++;
    }

}
