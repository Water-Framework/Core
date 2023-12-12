/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */
package it.water.core.bundle;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @Author Aristide Cittadino
 * Global properties available for an  Environment
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertiesNames {
    public static final String HYPERIOT_TEST_MODE = "water.testMode";
    public static final String HYPERIOT_LAYER = "water.layer";
    public static final String HYPERIOT_NODE_ID = "water.nodeId";
    public static final String HYPERIOT_BASE_REST_CONTEST = "water.base.rest.context";
    public static final String HYPERIOT_SERVICES_URL = "water.services.url";
    public static final String HYPERIOT_FRONTEND_URL = "water.frontend.url";
    public static final String HYPERIOT_ACTIVATE_ACCOUNT_URL = "water.activateAccount.url";
    public static final String HYPERIOT_RESET_PASSWORD = "water.resetPassword.url";
}
