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
package it.water.core.service;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.service.api.TestServiceApi;
import it.water.core.testing.utils.bundle.TestInitializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WaterServiceTest {

    private TestInitializer initializer;

    @BeforeAll
    public void initializeTestFramework() {
        initializer = new TestInitializer();
        initializer.withFakePermissionManager()
                .start();
    }

    /**
     * Checking wether all framework components have been initialized correctly
     */
    @Test
    void testRegisteredComponents() {
        Assertions.assertNotNull(initializer.getComponentRegistry());
        Assertions.assertNotNull(initializer.getRuntime());
        Assertions.assertNotNull(initializer.getComponentRegistry().findComponents(ApplicationProperties.class, null));
        Assertions.assertNotNull(initializer.getComponentRegistry().findComponents(TestServiceApi.class, null));
    }

    @Test
    void testIntegrationServices() {
        TestServiceApi testServiceApi = initializer.getComponentRegistry().findComponent(TestServiceApi.class, null);
        Assertions.assertDoesNotThrow(() -> testServiceApi.doSomething());
    }
}
