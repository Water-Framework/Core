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

import it.water.core.api.permission.PermissionManager;

/**
 * @Author Aristide Cittadino
 * Simple Permission Manager which supports operation of user management for test purpose.
 * As a way to simplify tests TestPermissionManager injectx also UserManager is just a shortcut.
 * The developer can use TestpermissionManager or TestUserManager to add/remove, they both use the same core bean which is InMemoryTestPermissionManager
 */
public interface TestPermissionManager extends PermissionManager {

}
