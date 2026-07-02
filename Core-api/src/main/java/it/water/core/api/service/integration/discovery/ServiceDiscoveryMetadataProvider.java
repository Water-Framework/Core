/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package it.water.core.api.service.integration.discovery;

import it.water.core.api.service.Service;

import java.util.Locale;

/**
 * Exposes service discovery metadata derived from Water service conventions.
 * Implemented by base services so modules can override the default service name only when convention is not enough.
 */
public interface ServiceDiscoveryMetadataProvider extends Service {
    default String getServiceName() {
        return deriveServiceName(this.getClass().getSimpleName());
    }

    static String deriveServiceName(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) {
            return "";
        }
        String value = removeSuffix(simpleName, "SpringRestApi");
        value = removeSuffix(value, "RestApi");
        value = removeSuffix(value, "SystemServiceImpl");
        value = removeSuffix(value, "ServiceImpl");
        value = removeSuffix(value, "Service");
        value = removeSuffix(value, "Impl");
        value = removeSuffix(value, "Api");
        // Insert a hyphen at camelCase and acronym boundaries using zero-width
        // look-arounds. These avoid the overlapping greedy quantifier of
        // "([A-Z]+)([A-Z][a-z])", which is flagged by Sonar (S5852) as vulnerable
        // to polynomial backtracking (ReDoS).
        return value
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", "-")
                .replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", "-")
                .toLowerCase(Locale.ROOT);
    }

    static String removeSuffix(String value, String suffix) {
        if (value.endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        }
        return value;
    }
}
