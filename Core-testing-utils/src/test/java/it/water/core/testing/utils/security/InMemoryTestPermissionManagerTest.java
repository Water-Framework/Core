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
package it.water.core.testing.utils.security;

import it.water.core.api.model.User;
import it.water.core.api.role.RoleManager;
import it.water.core.api.user.UserManager;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link InMemoryTestPermissionManager#userHasRoles(String, String[])}:
 * OR semantics, null/empty roles and unknown users.
 */
@ExtendWith(MockitoExtension.class)
class InMemoryTestPermissionManagerTest {
    private static final String USERNAME = "someUser";
    private static final long USER_ID = 7L;
    private static final String ROLE_A = "roleA";
    private static final String ROLE_B = "roleB";

    @Mock
    private UserManager userManager;
    @Mock
    private RoleManager roleManager;
    @Mock
    private User user;

    private InMemoryTestPermissionManager permissionManager;

    @BeforeEach
    void setUp() {
        permissionManager = new InMemoryTestPermissionManager();
        permissionManager.setUserManager(userManager);
        permissionManager.setRoleManager(roleManager);
    }

    private void givenExistingUser() {
        Mockito.when(userManager.findUser(USERNAME)).thenReturn(user);
        Mockito.when(user.getId()).thenReturn(USER_ID);
    }

    @Test
    void testUserHasRoles_hasOnlyOneOfTwoRoles_returnsTrue() {
        givenExistingUser();
        Mockito.when(roleManager.hasRole(USER_ID, ROLE_A)).thenReturn(true);
        Mockito.lenient().when(roleManager.hasRole(USER_ID, ROLE_B)).thenReturn(false);
        Assertions.assertTrue(permissionManager.userHasRoles(USERNAME, new String[]{ROLE_A, ROLE_B}));
    }

    @Test
    void testUserHasRoles_hasOnlySecondRole_returnsTrue() {
        givenExistingUser();
        Mockito.when(roleManager.hasRole(USER_ID, ROLE_A)).thenReturn(false);
        Mockito.when(roleManager.hasRole(USER_ID, ROLE_B)).thenReturn(true);
        Assertions.assertTrue(permissionManager.userHasRoles(USERNAME, new String[]{ROLE_A, ROLE_B}));
    }

    @Test
    void testUserHasRoles_hasNoneOfTheRoles_returnsFalse() {
        givenExistingUser();
        Mockito.when(roleManager.hasRole(USER_ID, ROLE_A)).thenReturn(false);
        Mockito.when(roleManager.hasRole(USER_ID, ROLE_B)).thenReturn(false);
        Assertions.assertFalse(permissionManager.userHasRoles(USERNAME, new String[]{ROLE_A, ROLE_B}));
    }

    @Test
    void testUserHasRoles_emptyRolesArray_returnsFalse() {
        Assertions.assertFalse(permissionManager.userHasRoles(USERNAME, new String[]{}));
        Mockito.verifyNoInteractions(userManager, roleManager);
    }

    @Test
    void testUserHasRoles_nullRolesArray_returnsFalse() {
        Assertions.assertFalse(permissionManager.userHasRoles(USERNAME, null));
        Mockito.verifyNoInteractions(userManager, roleManager);
    }

    @Test
    void testUserHasRoles_userManagerReturnsNull_returnsFalse() {
        Mockito.when(userManager.findUser(USERNAME)).thenReturn(null);
        Assertions.assertFalse(permissionManager.userHasRoles(USERNAME, new String[]{ROLE_A}));
        Mockito.verifyNoInteractions(roleManager);
    }

    @Test
    void testUserHasRoles_nonExistentUserThrowsNoResult_returnsFalse() {
        Mockito.when(userManager.findUser(USERNAME)).thenThrow(new NoResultException("not found"));
        Assertions.assertFalse(permissionManager.userHasRoles(USERNAME, new String[]{ROLE_A}));
        Mockito.verifyNoInteractions(roleManager);
    }
}
