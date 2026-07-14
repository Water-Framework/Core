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
package it.water.core.security.model.principal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Multitenancy Tassello 1 (see multitenancy-analysis-proposal.md, section 9): unit tests for the
 * additive {@code companyId} field on {@link UserPrincipal}.
 * <p>
 * Covers:
 * <ul>
 *     <li>the new 5-arg constructor propagating a non-null companyId;</li>
 *     <li>the pre-existing 4-arg constructor staying backward compatible (companyId == null);</li>
 *     <li>all other getters (name/isAdmin/loggedEntityId/issuer) remaining unaffected in both cases.</li>
 * </ul>
 * Plain JUnit 5, no Water runtime needed: {@link UserPrincipal} is a POJO.
 */
class UserPrincipalTest {

    private static final String TEST_NAME = "user1";
    private static final boolean TEST_IS_ADMIN = true;
    private static final long TEST_LOGGED_ENTITY_ID = 100L;
    private static final String TEST_ISSUER = "testIssuer";
    private static final Long TEST_COMPANY_ID = 42L;

    @Test
    void testFiveArgConstructor_getCompanyId_returnsConfiguredValue() {
        UserPrincipal principal = new UserPrincipal(TEST_NAME, TEST_IS_ADMIN, TEST_LOGGED_ENTITY_ID, TEST_ISSUER, TEST_COMPANY_ID);
        Assertions.assertEquals(TEST_COMPANY_ID, principal.getCompanyId());
    }

    @Test
    void testFourArgConstructor_getCompanyId_isNull_backwardCompatible() {
        UserPrincipal principal = new UserPrincipal(TEST_NAME, TEST_IS_ADMIN, TEST_LOGGED_ENTITY_ID, TEST_ISSUER);
        Assertions.assertNull(principal.getCompanyId(),
                "4-arg constructor must keep delegating to the 5-arg one with a null companyId (backward compat)");
    }

    @Test
    void testFiveArgConstructor_nullCompanyId_isAllowed() {
        UserPrincipal principal = new UserPrincipal(TEST_NAME, false, TEST_LOGGED_ENTITY_ID, TEST_ISSUER, null);
        Assertions.assertNull(principal.getCompanyId());
    }

    @Test
    void testFiveArgConstructor_otherGetters_unaffectedByCompanyId() {
        UserPrincipal principal = new UserPrincipal(TEST_NAME, TEST_IS_ADMIN, TEST_LOGGED_ENTITY_ID, TEST_ISSUER, TEST_COMPANY_ID);
        Assertions.assertEquals(TEST_NAME, principal.getName());
        Assertions.assertTrue(principal.isAdmin());
        Assertions.assertEquals(TEST_LOGGED_ENTITY_ID, principal.getLoggedEntityId());
        Assertions.assertEquals(TEST_ISSUER, principal.getIssuer());
    }

    @Test
    void testFourArgConstructor_otherGetters_unaffectedByMissingCompanyId() {
        UserPrincipal principal = new UserPrincipal(TEST_NAME, false, TEST_LOGGED_ENTITY_ID, TEST_ISSUER);
        Assertions.assertEquals(TEST_NAME, principal.getName());
        Assertions.assertFalse(principal.isAdmin());
        Assertions.assertEquals(TEST_LOGGED_ENTITY_ID, principal.getLoggedEntityId());
        Assertions.assertEquals(TEST_ISSUER, principal.getIssuer());
    }
}
