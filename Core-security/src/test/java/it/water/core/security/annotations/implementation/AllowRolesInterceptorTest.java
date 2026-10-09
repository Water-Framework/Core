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
import it.water.core.api.permission.PermissionUtil;
import it.water.core.api.permission.SecurityContext;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.annotations.AllowRoles;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.security.service.TestEntityService;
import it.water.core.security.service.TestEntityServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

/**
 * Pure unit tests of {@link AllowRolesInterceptor} (no Water runtime): null context, admin bypass,
 * role query delegation.
 */
@ExtendWith(MockitoExtension.class)
class AllowRolesInterceptorTest {
    private static final String USERNAME = "someUser";

    @Mock
    private Runtime runtime;
    @Mock
    private PermissionUtil permissionUtil;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private TestEntityService apiService;

    private AllowRolesInterceptor interceptor;
    private Method method;
    private AllowRoles annotation;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        interceptor = new AllowRolesInterceptor();
        interceptor.setWaterRuntime(runtime);
        interceptor.setWaterPermissionUtil(permissionUtil);
        method = TestEntityServiceImpl.class.getMethod("allowAnyOfTwoRolesMethod");
        annotation = method.getAnnotation(AllowRoles.class);
        Assertions.assertNotNull(annotation);
    }

    @Test
    void testInterceptMethod_nullSecurityContext_throwsUnauthorized() {
        Mockito.when(runtime.getSecurityContext()).thenReturn(null);
        Assertions.assertThrows(UnauthorizedException.class,
                () -> interceptor.interceptMethod(apiService, method, new Object[]{}, annotation));
        Mockito.verifyNoInteractions(permissionUtil);
    }

    @Test
    void testInterceptMethod_loggedAdmin_passesWithoutRoleQuery() {
        Mockito.when(runtime.getSecurityContext()).thenReturn(securityContext);
        Mockito.when(securityContext.isLoggedIn()).thenReturn(true);
        Mockito.when(securityContext.isAdmin()).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> interceptor.interceptMethod(apiService, method, new Object[]{}, annotation));
        Mockito.verifyNoInteractions(permissionUtil);
    }

    @Test
    void testInterceptMethod_nonAdminWithRole_passes() {
        Mockito.when(runtime.getSecurityContext()).thenReturn(securityContext);
        Mockito.when(securityContext.isLoggedIn()).thenReturn(true);
        Mockito.when(securityContext.isAdmin()).thenReturn(false);
        Mockito.when(securityContext.getLoggedUsername()).thenReturn(USERNAME);
        Mockito.when(permissionUtil.userHasRoles(Mockito.eq(USERNAME), Mockito.any(String[].class))).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> interceptor.interceptMethod(apiService, method, new Object[]{}, annotation));
        Mockito.verify(permissionUtil).userHasRoles(Mockito.eq(USERNAME), Mockito.any(String[].class));
    }

    @Test
    void testInterceptMethod_nonAdminWithoutRole_throwsUnauthorized() {
        Mockito.when(runtime.getSecurityContext()).thenReturn(securityContext);
        Mockito.when(securityContext.isLoggedIn()).thenReturn(true);
        Mockito.when(securityContext.isAdmin()).thenReturn(false);
        Mockito.when(securityContext.getLoggedUsername()).thenReturn(USERNAME);
        Mockito.when(permissionUtil.userHasRoles(Mockito.eq(USERNAME), Mockito.any(String[].class))).thenReturn(false);
        Assertions.assertThrows(UnauthorizedException.class,
                () -> interceptor.interceptMethod(apiService, method, new Object[]{}, annotation));
    }

    @Test
    void testInterceptMethod_notLoggedInAdminFlag_doesNotBypass() {
        Mockito.when(runtime.getSecurityContext()).thenReturn(securityContext);
        Mockito.when(securityContext.isLoggedIn()).thenReturn(false);
        Mockito.when(securityContext.getLoggedUsername()).thenReturn(null);
        Mockito.when(permissionUtil.userHasRoles(Mockito.isNull(), Mockito.any(String[].class))).thenReturn(false);
        Assertions.assertThrows(UnauthorizedException.class,
                () -> interceptor.interceptMethod(apiService, method, new Object[]{}, annotation));
    }

    @Test
    void testInterceptMethod_emptyRoles_throwsWaterRuntimeException() {
        AllowRoles emptyRoles = Mockito.mock(AllowRoles.class);
        Mockito.when(emptyRoles.rolesNames()).thenReturn(new String[]{});
        Assertions.assertThrows(WaterRuntimeException.class,
                () -> interceptor.interceptMethod(apiService, method, new Object[]{}, emptyRoles));
    }

    @Test
    void testGetAnnotation_returnsAllowRoles() {
        Assertions.assertEquals(AllowRoles.class, interceptor.getAnnotation());
    }
}
