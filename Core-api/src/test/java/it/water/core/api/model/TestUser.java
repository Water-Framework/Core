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

import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.Collections;

@Setter
@NoArgsConstructor
public class TestUser implements User {
    private long id;
    private String name;
    private String lastname;
    private String email;
    private String username;
    private boolean admin;
    private Long loggedEntityId;
    private String issuer;
    private String password;
    private String salt;

    public TestUser(long id, String name, String lastname, String email, String username, boolean admin, String issuer,
            String password, String salt) {
        this.id = id;
        this.name = name;
        this.lastname = lastname;
        this.email = email;
        this.username = username;
        this.admin = admin;
        this.issuer = issuer;
        this.password = password;
        this.salt = salt;
    }

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
        return admin;
    }

    @Override
    public Long getLoggedEntityId() {
        return loggedEntityId;
    }

    @Override
    public String getIssuer() {
        return issuer;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getSalt() {
        return salt;
    }

    @Override
    public Collection<Role> getRoles() {
        return Collections.emptyList();
    }
}
