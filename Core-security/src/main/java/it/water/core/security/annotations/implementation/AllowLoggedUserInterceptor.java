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

package it.water.core.security.annotations.implementation;

import it.water.core.api.bundle.Runtime;
import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.permission.annotations.AllowLoggedUser;
import it.water.core.permission.exceptions.UnauthorizedException;
import lombok.Setter;

import java.lang.reflect.Method;

/**
 * @Author Aristide Cittadino
 *
 * This method checks wether the current annotated method is executed in a secured context.
 * The security context is created automatically for rest services , validating JWT Tokens.
 * For other use case you have to provide the logic to inject security context in the thread context.
 */
@FrameworkComponent(services = {BeforeMethodInterceptor.class})
public class AllowLoggedUserInterceptor extends AbstractPermissionInterceptor implements BeforeMethodInterceptor<it.water.core.permission.annotations.AllowLoggedUser> {
    @Inject
    @Setter
    private Runtime currentRuntime;

    @Override
    public <S extends Service> void interceptMethod(S destination, Method m, Object[] args, AllowLoggedUser annotation) {
        if (currentRuntime == null || currentRuntime.getSecurityContext() == null || currentRuntime.getSecurityContext().getLoggedEntityId() == 0)
            throw new UnauthorizedException("No security context found, please login");
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Class getAnnotation() {
        return AllowLoggedUser.class;
    }
}
