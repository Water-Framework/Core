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
package it.water.core.testing.utils;

import it.water.core.api.action.Action;
import it.water.core.api.action.ActionsManager;
import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.Role;
import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.role.RoleManager;
import it.water.core.api.service.Service;
import it.water.core.api.service.integration.UserIntegrationClient;
import it.water.core.api.user.UserManager;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.validation.ValidationError;
import it.water.core.permission.action.CrudActions;
import it.water.core.testing.utils.api.TestPermissionManager;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import it.water.core.testing.utils.filter.TestComponentFilterBuilder;
import it.water.core.testing.utils.interceptors.TestServiceProxy;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.core.testing.utils.model.TestHUser;
import it.water.core.testing.utils.model.TestRole;
import it.water.core.testing.utils.runtime.TestRuntimeUtils;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.Assert;

import java.io.File;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@ExtendWith({MockitoExtension.class, WaterTestExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestingUtilsTest implements Service {
    private TestRuntimeInitializer initializer;
    private static final String ROLE = "ROLE";
    @Inject
    @Setter
    //injecting test permission manager in order to perform some basic security tests
    private TestPermissionManager testPermissionManager;

    @Inject
    @Setter
    private UserManager userManager;
    @Inject
    @Setter
    private RoleManager roleManager;
    @Inject
    @Setter
    private Runtime runtime;
    private User userOk;

    @BeforeAll
    public void initializeTestFramework() {
        initializer = TestRuntimeInitializer.getInstance();
        this.userOk = userManager.addUser("usernameOk", "username", "username", "email@mail.com", "pwd", "salt", false);
        Assertions.assertEquals("usernameOk", userOk.getUsername());
        Assertions.assertEquals("username", userOk.getName());
        Assertions.assertEquals("username", userOk.getLastname());
        Assertions.assertEquals("email@mail.com", userOk.getEmail());
    }

    @Test
    void testApplicationProertiesLoader() {
        ApplicationProperties applicationProperties = initializer.getComponentRegistry().findComponent(ApplicationProperties.class, null);
        Assertions.assertNotNull(applicationProperties.getProperty("rs.security.signature.algorithm"));
        Assertions.assertTrue(applicationProperties.containsKey("rs.security.signature.algorithm"));
        File customPropFile = new File("src/test/resources/custom.properties");
        applicationProperties.loadProperties(customPropFile);
        Assertions.assertTrue((Boolean) applicationProperties.containsKey("custom.property"));
        Assertions.assertEquals("customValue", applicationProperties.getProperty("custom.property"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> applicationProperties.unloadProperties(customPropFile));
        Assertions.assertNotNull(applicationProperties);
        Assertions.assertDoesNotThrow(() -> initializer.impersonate(userOk, runtime));
        Assertions.assertNotNull(initializer.getPermissionManager());
    }

    @Test
    void testComponentFilter() {
        TestComponentFilterBuilder testComponentFilterBuilder = new TestComponentFilterBuilder();
        Properties props = new Properties();
        props.put("name", "value");
        props.put("name2", "value");
        ComponentFilter filter = testComponentFilterBuilder.createFilter("name", "value");
        Assertions.assertTrue(filter.matches(props));
        Assertions.assertEquals("(name=value)", filter.getFilter());
        ComponentFilter filterAnd = filter.and(testComponentFilterBuilder.createFilter("name2", "value"));
        Assertions.assertTrue(filterAnd.matches(props));
        Assertions.assertEquals("(&(name=value)(name2=value))", filterAnd.getFilter());
        ComponentFilter filterOr = filter.or(testComponentFilterBuilder.createFilter("nameNone", "value2"));
        Assertions.assertTrue(filterOr.matches(props));
        Assertions.assertEquals("(|(name=value)(nameNone=value2))", filterOr.getFilter());
        Assertions.assertEquals("(!(name=value))", filter.not().getFilter());
        Assertions.assertEquals("(!(&(!(name=value))(name2=value)))", filterAnd.not().getFilter());
        Assertions.assertEquals("(!(|(!(name=value))(nameNone=value2)))", filterOr.not().getFilter());
    }

    @Test
    void testInterceptors() {
        TestServiceApi service = initializer.getComponentRegistry().findComponent(TestServiceApi.class, null);
        Assertions.assertDoesNotThrow(() -> service.doSomething());
        TestServiceProxy<TestServiceApi> proxy = (TestServiceProxy<TestServiceApi>) Proxy.getInvocationHandler(service);
        Assertions.assertNotNull(proxy.getRegistration());
        Assertions.assertNotNull(proxy.getRegistration().getRegistrationClass());
        Assertions.assertNotNull(proxy.getRegistration().getConfiguration());
        Assertions.assertNotNull(proxy.getRegistration().getRegistration());
        Assertions.assertTrue(initializer.getComponentRegistry().unregisterComponent(proxy.getRegistration()));
    }

    @Test
    void testModels() {
        Role testRole = new TestRole(ROLE);
        User user = new TestHUser(1000, "name", "lastname", "email", "username", Arrays.asList(testRole), false);
        roleManager.addRole(user.getId(), testRole);
        Assertions.assertTrue(roleManager.hasRole(user.getId(), ROLE));
    }

    @Test
    void testRegistry() {
        final ComponentRegistry registry = initializer.getComponentRegistry();
        Assertions.assertThrows(RuntimeException.class, () -> registry.registerComponent(TestServiceApi.class, null, null));
        Assertions.assertNotNull(initializer.getComponentRegistry().getComponentFilterBuilder());
    }

    @Test
    void testRuntime() {
        TestRuntimeInitializer.getInstance().impersonate(userOk, runtime);
        Assertions.assertNotNull(runtime.getSecurityContext());
        Assertions.assertDoesNotThrow(() -> initializer.impersonate(userOk, runtime));
    }

    @Test
    void testSecurity() {
        Role testRole = new TestRole(ROLE);
        User user = new TestHUser(1001, "name", "lastname", "email", "usernameOk", Arrays.asList(testRole), false);
        PermissionManager permissionManager = initializer.getComponentRegistry().findComponents(PermissionManager.class, null).get(0);
        ActionsManager actionManager = initializer.getComponentRegistry().findComponent(ActionsManager.class, null);
        Role role = roleManager.createIfNotExists(ROLE);
        Assertions.assertTrue(roleManager.exists(ROLE));
        roleManager.addRole(userOk.getId(), role);
        initializer.impersonate(userOk, runtime);
        SecurityContext securityContext = runtime.getSecurityContext();
        TestResource res = new TestResource();
        Action action = actionManager.getActions().get(TestResource.class.getName()).getAction(CrudActions.SAVE);
        Assertions.assertTrue(permissionManager.checkPermission("usernameOk", res.getResourceName(), action));
        Assertions.assertTrue(permissionManager.checkPermission("usernameOk", res, action));
        Assertions.assertTrue(permissionManager.checkPermissionAndOwnership("usernameOk", res.getResourceName(), action));
        Assertions.assertTrue(permissionManager.checkPermissionAndOwnership("usernameOk", res, action));
        Assertions.assertTrue(permissionManager.userHasRoles("usernameOk", new String[]{ROLE}));
        Assertions.assertTrue(permissionManager.checkUserOwnsResource(user, new String[]{ROLE}));

        Assertions.assertTrue(securityContext.getLoggedEntityId() > 0);
        Assertions.assertEquals("usernameOk", securityContext.getLoggedUsername());
        Assertions.assertFalse(securityContext.isAdmin());
    }

    @Test
    void testRuntimeUtils() {
        Assertions.assertDoesNotThrow(() -> TestRuntimeUtils.impersonateAdmin(initializer.getComponentRegistry()));
        TestRuntimeUtils.assertValidationException(() -> {
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError("username", "username", "username is required"));
            throw new ValidationException(errors);
        }, "username");
        User user = new TestHUser(1000, "name", "lastname", "email", "username", List.of(), false);
        Assertions.assertDoesNotThrow(() -> TestRuntimeUtils.getAs(user, () -> user));
        Assertions.assertDoesNotThrow(() -> TestRuntimeUtils.runAs(user, () -> {}));
    }

    @Test
    void testInMemoryUserManager(){
        User toRemove = userManager.addUser("usernameToRemove", "usernameToRemove", "usernameToRemove", "usernameToRemove@mail.com", "usernameToRemove", "salt", false);
        roleManager.addRole(toRemove.getId(),roleManager.getRole(ROLE));
        Assertions.assertTrue(roleManager.hasRole(toRemove.getId(), ROLE));
        roleManager.removeRole(toRemove.getId(),roleManager.getRole(ROLE));
        Assertions.assertFalse(roleManager.hasRole(toRemove.getId(), ROLE));
        UserIntegrationClient userIntegrationClient = (UserIntegrationClient) userManager;
        Assertions.assertNotNull(userManager.findUser("usernameToRemove"));
        Assertions.assertNotNull(userIntegrationClient.fetchUserByUsername("usernameToRemove"));
        Assertions.assertNotNull(userIntegrationClient.fetchUserByEmailAddress("usernameToRemove@mail.com"));
        Assertions.assertNull(userIntegrationClient.fetchUserByUserId(0));
        userManager.removeUser("usernameToRemove");
        Assertions.assertNull(userManager.findUser("usernameToRemove"));
        Assertions.assertNotNull(userManager.all());
    }
}
