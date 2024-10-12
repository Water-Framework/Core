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

package it.water.core.testing.utils.runtime;

import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.User;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.user.UserManager;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.validation.ValidationError;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * @Author Aristide Cittadino
 * During the test phase thread local may fail to associate the right security context to requests.
 * With this componente used onlu for test purpose we should overcome this problem.
 * This component is useful specially when tests are executed outside the junit thread for example karate.
 */
public class TestRuntimeUtils {
    private TestRuntimeUtils(){

    }
    public static void impersonateAdmin(ComponentRegistry componentRegistry) {
        Runtime runtime = componentRegistry.findComponent(Runtime.class, null);
        UserManager userManager = componentRegistry.findComponent(UserManager.class, null);
        User adminUser = userManager.findUser("admin");
        runtime.fillSecurityContext(new SecurityContext() {
            @Override
            public String getLoggedUsername() {
                return adminUser.getUsername();
            }

            @Override
            public boolean isLoggedIn() {
                return true;
            }

            @Override
            public boolean isAdmin() {
                return adminUser.isAdmin();
            }

            @Override
            public long getLoggedEntityId() {
                return adminUser.getId();
            }
        });
    }

    /**
     * Launches one operation and checks validation exception
     *
     * @param r
     * @param invalidFields
     */
    public static void assertValidationException(Runnable r, String... invalidFields) {
        List<ValidationError> errors = null;
        try {
            r.run();
        } catch (ValidationException e) {
            errors = e.getViolations();
        }
        //checking wether the expected field is contained in the violation message
        Assertions.assertTrue(errors != null && errors.stream().anyMatch(violation -> Arrays.stream(invalidFields).anyMatch(invalidField -> violation.getField().endsWith(invalidField))));
    }

    /**
     * Perform an action impersonating a specific user
     *
     * @param user
     * @param operation
     */
    public static void runAs(User user, Runnable operation) {
        Runtime runtime = TestRuntimeInitializer.getInstance().getComponentRegistry().findComponent(Runtime.class, null);
        TestRuntimeInitializer.getInstance().impersonate(user, runtime);
        operation.run();
        runtime.fillSecurityContext(null);
    }

    /**
     * Invoke a supplier function with specific user
     *
     * @param user
     * @param supplier
     * @param <R>
     * @return
     */
    public static <R> R getAs(User user, Supplier<R> supplier) {
        Runtime runtime = TestRuntimeInitializer.getInstance().getComponentRegistry().findComponent(Runtime.class, null);
        TestRuntimeInitializer.getInstance().impersonate(user, runtime);
        R returnValue = supplier.get();
        runtime.fillSecurityContext(null);
        return returnValue;
    }

}
