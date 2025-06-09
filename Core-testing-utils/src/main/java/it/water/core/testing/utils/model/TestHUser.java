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
package it.water.core.testing.utils.model;

import java.util.List;

import it.water.core.api.model.Role;
import it.water.core.api.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class TestHUser implements User {

    @Getter
    private long id;
    @Getter
    private String name;
    @Getter
    private String lastname;
    @Getter
    private String email;
    @Getter
    private String username;
    @Getter
    private String password;
    @Getter
    private String salt;
    @Getter
    private List<Role> roles;
    @Getter
    private boolean isAdmin;
    @Getter
    private boolean isActive;

    @Override
    public Long getLoggedEntityId() {
        return id;
    }

    @Override
    public String getIssuer() {
        return TestHUser.class.getName();
    }

}
