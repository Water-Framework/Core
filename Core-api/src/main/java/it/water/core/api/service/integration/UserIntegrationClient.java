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

package it.water.core.api.service.integration;

import it.water.core.api.model.User;

/**
 * @Author Aristide Cittadino
 * This class implements the logic of user information retrieval.
 *
 * It supports different implementation:
 *  - local - using queries
 *  - remote - using remote apis
 *
 */
public interface UserIntegrationClient extends EntityIntegrationClient {
    User fetchUserByUsername(String username);
    User fetchUserByEmailAddress(String emailAddress);
    User fetchUserByUserId(long userId);
}
