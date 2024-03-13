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

package it.water.core.permission.annotations;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author Aristide Cittadino.
 * Annotation used on resource classes to identify available permission on that resource.
 * Eventually the developer can specify which roles has default permissions.
 * <p>
 * This annotation supports RBAC permission management and Granular permission also.
 */
@Target({ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@IndexAnnotated
public @interface AccessControl {
    /**
     * Position of actions matters, because permission are managed in bitwise AND logic.
     * Every action has an actionId integer associated which is the position inside the list.
     *
     * @return
     */
    String[] availableActions() default {};

    /**
     * Default roles and permission that should be created at the system startup
     *
     * @return
     */
    DefaultRoleAccess[] rolesPermissions() default {};
}
