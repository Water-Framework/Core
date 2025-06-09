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
import it.water.core.api.security.Authenticable;
import it.water.core.api.security.AuthenticationProvider;
import it.water.core.api.service.integration.UserIntegrationClient;
import it.water.core.api.user.UserManager;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.testing.utils.api.TestUserManager;
import it.water.core.testing.utils.model.TestHUser;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@FrameworkComponent(priority = 0, services = {UserManager.class, TestUserManager.class, UserIntegrationClient.class, AuthenticationProvider.class})
public class InMemoryUserManager implements TestUserManager, UserIntegrationClient, AuthenticationProvider {
    private static int userCounter = 1;
    private static final String ADMIN = "admin";
    private Set<User> users = new HashSet<>();
    @Getter
    private String uid;

    @Inject
    @Setter
    private UserIntegrationClient userIntegrationClient;

    public InMemoryUserManager() {
        this.uid = UUID.randomUUID().toString();
        addUser(ADMIN, ADMIN, ADMIN, ADMIN + "@ADMIN.com", "Password._", "Salt", true);
    }

    @Override
    public User addUser(String username, String name, String lastname, String email, String password, String salt, boolean isAdmin) {
        long userId = getUserCounterAndIncrement();
        User u = new TestHUser(userId, name, lastname, email, username, password, salt, Collections.emptyList(), isAdmin,true);
        users.add(u);
        return u;
    }

    @Override
    public void removeUser(String username) {
        User u = findUser(username);
        if (u != null) users.remove(u);
    }

    @Override
    public User findUser(String username) {
        Optional<User> userOpt = this.users.stream().filter(user -> user.getUsername().equals(username)).findFirst();
        if (userOpt.isPresent()) return userOpt.get();
        return null;
    }

    @Override
    public Collection<User> all() {
        return Collections.unmodifiableSet(users);
    }

    @Override
    public User fetchUserByUsername(String username) {
        //if user integration client is this component avoid stackoverflow
        if (this.equals(userIntegrationClient)) return findUser(username);
        return userIntegrationClient.fetchUserByUsername(username);
    }

    @Override
    public User fetchUserByEmailAddress(String emailAddress) {
        //if user integration client is this component avoid stackoverflow
        if (this.equals(userIntegrationClient))
            return users.stream().filter(user -> user.getEmail().equalsIgnoreCase(emailAddress)).findAny().orElse(null);
        return userIntegrationClient.fetchUserByEmailAddress(emailAddress);
    }

    @Override
    public User fetchUserByUserId(long userId) {
        //if user integration client is this component avoid stackoverflow
        if (this.equals(userIntegrationClient))
            return users.stream().filter(user -> user.getId() == userId).findAny().orElse(null);
        return userIntegrationClient.fetchUserByUserId(userId);
    }

    @Override
    public Authenticable login(String username, String password) {
        User user = fetchUserByUsername(username);
        if (user == null || password == null || password.isBlank() || !password.equals(user.getPassword()))
            throw new UnauthorizedException();
        return user;
    }

    @Override
    public Collection<String> issuersNames() {
        return List.of(TestHUser.class.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof TestUserManager that)) return false;
        return Objects.equals(this.getUid(), that.getUid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getUid());
    }

    private static long getUserCounterAndIncrement() {
        return userCounter++;
    }

}
