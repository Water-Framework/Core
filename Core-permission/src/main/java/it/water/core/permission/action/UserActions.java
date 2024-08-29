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

package it.water.core.permission.action;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @Author Aristide Cittadino
 * Class which exposes User additional permission such as impersonification which is the ability for a user to impersonate another one.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserActions {
    public static final String IMPERSONATE = "impersonate";
    public static final String ACTIVATE = "activate";
    public static final String DEACTIVATE = "deactivate";
}
