
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

package it.water.core.model.validation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @Author Aristide Cittadino.
 * Validation Messages, must be translated on client
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationMessages {
    public static final String NO_MALITIUS_CODE_VALIDATION = "No malitius code is allowed";
    public static final String POW_OF_2_VALIDATION = "Value must be pow of 2";
}
