
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
 * @Author Aristide Cittadino.
 * Class that enumerate SHARE action. Sharing is the possibility to mark an Entity as Sharable.
 * Sharing entity or resource let other users can see and operate on them.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class ShareAction {
    public static final String SHARE = "share";

}
