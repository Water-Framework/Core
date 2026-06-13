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
package it.water.core.security;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.api.security.PasswordHashService;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.testing.utils.junit.WaterTestExtension;
import lombok.Setter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Properties;

/**
 * Regression tests for H9 — PasswordHashService (PHC format, legacy verification,
 * needsRehash, and constant-time comparison).
 * <p>
 * Uses WaterTestExtension so that PasswordHashServiceImpl is resolved via the
 * TestComponentRegistry exactly as production code does.
 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PasswordHashServiceImplTest implements Service {

    /**
     * PHC prefix produced by PasswordHashServiceImpl
     */
    private static final String PHC_PREFIX = "$pbkdf2-sha256$";
    private static final String LEGACY_TEST_PASSWORD = "LegacyPass1!";

    @Inject
    @Setter
    private PasswordHashService passwordHashService;

    @Inject
    @Setter
    private EncryptionUtil encryptionUtil;

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    // H9-1  hash() produces a well-formed PHC string

    @Test
    @Order(1)
    void hashHappyPathReturnsPHCString() {
        String phc = passwordHashService.hash("MyPassword1!".toCharArray());

        Assertions.assertNotNull(phc, "hash() must never return null");
        Assertions.assertTrue(phc.startsWith(PHC_PREFIX),
                "hash() must start with the PHC id '$pbkdf2-sha256$'");

        // PHC layout: $pbkdf2-sha256$i=<n>$<saltB64>$<hashB64>  => 5 segments after split on '$'
        String[] parts = phc.split("\\$");
        Assertions.assertEquals(5, parts.length,
                "PHC string must contain exactly 4 '$' delimiters");
        Assertions.assertTrue(parts[2].startsWith("i="),
                "Third segment must be the iterations parameter 'i=<n>'");
        int iterations = Integer.parseInt(parts[2].substring(2));
        Assertions.assertTrue(iterations > 0, "Iterations must be a positive integer");

        // salt and hash segments must be valid unpadded Base64
        Assertions.assertDoesNotThrow(() -> Base64.getDecoder().decode(parts[3]),
                "Salt segment must be valid Base64");
        Assertions.assertDoesNotThrow(() -> Base64.getDecoder().decode(parts[4]),
                "Hash segment must be valid Base64");
    }

    @Test
    @Order(2)
    void hashEachCallProducesDifferentSalt() {
        char[] pwd = "SamePassword1!".toCharArray();
        String phc1 = passwordHashService.hash(pwd);
        String phc2 = passwordHashService.hash(pwd);

        // Different salts mean different hashes even for the same password
        Assertions.assertNotEquals(phc1, phc2,
                "Two successive hash() calls must produce different PHC strings (random salt)");
    }

    // H9-2  matches(pwd, phcHash) — PHC path

    @Test
    @Order(3)
    void matchesPhcCorrectPasswordReturnsTrue() {
        char[] pwd = "CorrectPass1!".toCharArray();
        String phc = passwordHashService.hash(pwd);

        Assertions.assertTrue(passwordHashService.matches(pwd, phc),
                "matches() must return true for the correct password");
    }

    @Test
    @Order(4)
    void matchesPhcWrongPasswordReturnsFalse() {
        String phc = passwordHashService.hash("CorrectPass1!".toCharArray());

        Assertions.assertFalse(passwordHashService.matches("WrongPass99!".toCharArray(), phc),
                "matches() must return false for an incorrect password");
    }

    @Test
    @Order(5)
    void matchesNullStoredHashReturnsFalse() {
        Assertions.assertFalse(passwordHashService.matches("AnyPwd1!".toCharArray(), null),
                "matches() with null stored hash must return false");
    }

    @Test
    @Order(6)
    void matchesEmptyStoredHashReturnsFalse() {
        Assertions.assertFalse(passwordHashService.matches("AnyPwd1!".toCharArray(), ""),
                "matches() with empty stored hash must return false");
    }

    @Test
    @Order(7)
    void matchesMalformedPhcHashReturnsFalse() {
        // Starts with '$' (is treated as PHC) but has wrong structure
        Assertions.assertFalse(
                passwordHashService.matches("AnyPwd1!".toCharArray(), "$malformed$nonsense"),
                "matches() with a malformed PHC string must return false, not throw");
    }

    // H9-3  matches(pwd, hash, legacySalt) — legacy path (retro-compatibility)

    /**
     * Builds a legacy hash exactly as WaterUser did before the PHC migration:
     * new String(encryptionUtil.hashPassword(saltBytes, password))
     * then verifies that PasswordHashService.matches() accepts it.
     */
    @Test
    @Order(8)
    void matchesLegacyCorrectPasswordReturnsTrue()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = encryptionUtil.generate16BytesSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);
        String password = LEGACY_TEST_PASSWORD;

        // Build legacy hash the same way the old code did
        String legacyHash = new String(encryptionUtil.hashPassword(saltBytes, password));

        boolean result = passwordHashService.matches(password.toCharArray(), legacyHash, saltBase64);
        Assertions.assertTrue(result,
                "Legacy PBKDF2-SHA1 hash must be accepted by matches(pwd, hash, legacySalt)");
    }

    @Test
    @Order(9)
    void matchesLegacyWrongPasswordReturnsFalse()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = encryptionUtil.generate16BytesSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(saltBytes);
        String password = LEGACY_TEST_PASSWORD;

        String legacyHash = new String(encryptionUtil.hashPassword(saltBytes, password));

        boolean result = passwordHashService.matches("WrongPassword99!".toCharArray(), legacyHash, saltBase64);
        Assertions.assertFalse(result,
                "Legacy verification must return false for an incorrect password");
    }

    @Test
    @Order(10)
    void matchesLegacyNullSaltReturnsFalse()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = encryptionUtil.generate16BytesSalt();
        String password = LEGACY_TEST_PASSWORD;
        String legacyHash = new String(encryptionUtil.hashPassword(saltBytes, password));

        // No salt supplied — legacy path cannot reconstruct the digest
        boolean result = passwordHashService.matches(password.toCharArray(), legacyHash, null);
        Assertions.assertFalse(result,
                "Legacy verification must return false when legacySalt is null");
    }

    @Test
    @Order(11)
    void matchesLegacyEmptySaltReturnsFalse()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = encryptionUtil.generate16BytesSalt();
        String password = LEGACY_TEST_PASSWORD;
        String legacyHash = new String(encryptionUtil.hashPassword(saltBytes, password));

        boolean result = passwordHashService.matches(password.toCharArray(), legacyHash, "");
        Assertions.assertFalse(result,
                "Legacy verification must return false when legacySalt is empty");
    }

    /**
     * When storedHash is a PHC string, the legacySalt argument must be ignored
     * and the embedded salt must be used.
     */
    @Test
    @Order(12)
    void matchesPhcHashWithLegacySaltArgumentUseEmbeddedSalt() {
        char[] pwd = "PhcPlusLegacySaltArg1!".toCharArray();
        String phc = passwordHashService.hash(pwd);

        // Passing a random legacy salt must not interfere — PHC path takes over
        boolean result = passwordHashService.matches(pwd, phc, "randomLegacySalt==");
        Assertions.assertTrue(result,
                "When stored hash is PHC, legacySalt argument must be ignored");
    }

    // H9-4  needsRehash()

    @Test
    @Order(13)
    void needsRehashLegacyHashReturnsTrue()
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] saltBytes = encryptionUtil.generate16BytesSalt();
        String legacyHash = new String(encryptionUtil.hashPassword(saltBytes, "SomePass1!"));

        Assertions.assertTrue(passwordHashService.needsRehash(legacyHash),
                "needsRehash() must return true for a legacy (non-PHC) hash");
    }

    @Test
    @Order(14)
    void needsRehashCurrentPhcHashReturnsFalse() {
        String phc = passwordHashService.hash("CurrentPass1!".toCharArray());

        Assertions.assertFalse(passwordHashService.needsRehash(phc),
                "needsRehash() must return false for a freshly produced PHC hash");
    }

    @Test
    @Order(15)
    void needsRehashNullHashReturnsTrue() {
        Assertions.assertTrue(passwordHashService.needsRehash(null),
                "needsRehash() must return true for a null hash (treat as missing)");
    }

    @Test
    @Order(16)
    void needsRehashEmptyHashReturnsTrue() {
        Assertions.assertTrue(passwordHashService.needsRehash(""),
                "needsRehash() must return true for an empty hash");
    }

    @Test
    @Order(17)
    void needsRehashPhcHashWithStaleCostParametersReturnsTrue() {
        // Force a low iteration count in the stored PHC string so the current
        // configured value (default 210 000) is higher → must signal rehash
        // We craft a minimal valid PHC string with i=1 manually.
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        // Compute a real digest with 1 iteration so the structure is valid
        String saltB64 = Base64.getEncoder().withoutPadding().encodeToString(salt);
        // Produce a dummy 32-byte hash body (doesn't need to match any password for this test)
        byte[] dummyHash = new byte[32];
        new java.security.SecureRandom().nextBytes(dummyHash);
        String hashB64 = Base64.getEncoder().withoutPadding().encodeToString(dummyHash);
        String stalePhc = "$pbkdf2-sha256$i=1$" + saltB64 + "$" + hashB64;

        Assertions.assertTrue(passwordHashService.needsRehash(stalePhc),
                "needsRehash() must return true when stored iterations < configured iterations");
    }

    // H9-5  configuredIterations() fallback via ApplicationProperties

    @Test
    @Order(18)
    void hashCustomIterationsReflectedInPhcString() {
        // Set a recognisable (but not too low) iteration count
        int customIterations = 1000;
        Properties props = new Properties();
        props.put("water.security.password.hash.iterations", String.valueOf(customIterations));
        applicationProperties.loadProperties(props);

        String phc = passwordHashService.hash("CustomIter1!".toCharArray());
        String[] parts = phc.split("\\$");
        int storedIterations = Integer.parseInt(parts[2].substring(2));
        Assertions.assertEquals(customIterations, storedIterations,
                "PHC string must embed the configured iteration count");

        // Restore default so subsequent tests are not affected
        Properties restore = new Properties();
        restore.put("water.security.password.hash.iterations", "210000");
        applicationProperties.loadProperties(restore);
    }

    @Test
    @Order(19)
    void hashInvalidIterationsPropertyFallsBackToDefault() {
        Properties props = new Properties();
        props.put("water.security.password.hash.iterations", "not-a-number");
        applicationProperties.loadProperties(props);

        // Must not throw; must produce a valid PHC string using the default iterations
        String phc = passwordHashService.hash("FallbackIter1!".toCharArray());
        Assertions.assertNotNull(phc);
        Assertions.assertTrue(phc.startsWith(PHC_PREFIX));

        // Restore
        Properties restore = new Properties();
        restore.put("water.security.password.hash.iterations", "210000");
        applicationProperties.loadProperties(restore);
    }

    // -----------------------------------------------------------------------
    // PBKDF2 600k — default iteration count raised from 210 000 to 600 000
    // -----------------------------------------------------------------------

    /**
     * PBKDF2-600k-1: when no property override is set, hash() must embed 600 000 iterations.
     * This test explicitly clears the property override so the hard-coded DEFAULT_ITERATIONS
     * constant is used.  It is the only test that checks the actual default — all others use
     * a custom low value to keep the suite fast.
     *
     * NOTE: this test is intentionally slow (600 000 PBKDF2 iterations ≈ 0.5–2 s on CI hardware).
     * There is exactly one such test to satisfy the coverage requirement without bloating the suite.
     */
    @Test
    @Order(20)
    void hashDefaultIterationsIs600000() {
        final int expectedDefaultIterations = 600_000;

        // Clear any override so the implementation falls back to DEFAULT_ITERATIONS
        Properties props = new Properties();
        // Setting to empty string: the parser will fail and fall back to the constant default
        props.put("water.security.password.hash.iterations", "invalid-so-fallback-fires");
        applicationProperties.loadProperties(props);

        String phc = passwordHashService.hash("Default600kTest1!".toCharArray());

        Assertions.assertNotNull(phc, "hash() must not return null");
        Assertions.assertTrue(phc.startsWith(PHC_PREFIX), "hash() must start with PHC prefix");

        String[] parts = phc.split("\\$");
        int storedIterations = Integer.parseInt(parts[2].substring(2)); // "i=<n>" → n
        Assertions.assertEquals(expectedDefaultIterations, storedIterations,
                "The default iteration count must be 600 000 (OWASP 2023 recommendation for PBKDF2-HMAC-SHA256)");

        // Restore to low iteration count for remaining tests to stay fast
        Properties restore = new Properties();
        restore.put("water.security.password.hash.iterations", "1000");
        applicationProperties.loadProperties(restore);
    }

    /**
     * PBKDF2-600k-2: needsRehash() must return true for a hash produced at 210 000 iterations
     * (the old default before H9) because 210 000 < 600 000.
     *
     * We craft the PHC string manually (real PBKDF2 computation would be too slow at 210k in tests)
     * by embedding a dummy hash body — needsRehash() only reads the iteration segment.
     */
    @Test
    @Order(21)
    void needsRehashFor210kIterationsReturnsTrue() {
        final int staleIterations = 210_000;

        // Craft a syntactically valid PHC string with i=210000
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] dummyHash = new byte[32];
        new java.security.SecureRandom().nextBytes(dummyHash);
        String stalePhc = "$pbkdf2-sha256$i=" + staleIterations + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(dummyHash);

        // Ensure the configured threshold is 600 000 so 210k < threshold
        Properties props = new Properties();
        props.put("water.security.password.hash.iterations", "600000");
        applicationProperties.loadProperties(props);

        Assertions.assertTrue(passwordHashService.needsRehash(stalePhc),
                "needsRehash() must return true for a hash with 210 000 iterations when the current default is 600 000");

        // Restore to low iteration count
        Properties restore = new Properties();
        restore.put("water.security.password.hash.iterations", "1000");
        applicationProperties.loadProperties(restore);
    }

    /**
     * PBKDF2-600k-3: needsRehash() must return false for a hash produced at exactly 600 000
     * iterations when the configured threshold is also 600 000.
     */
    @Test
    @Order(22)
    void needsRehashFor600kIterationsReturnsFalse() {
        final int currentIterations = 600_000;

        // Craft a PHC string with i=600000
        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        byte[] dummyHash = new byte[32];
        new java.security.SecureRandom().nextBytes(dummyHash);
        String currentPhc = "$pbkdf2-sha256$i=" + currentIterations + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(dummyHash);

        // Configure threshold to 600 000 so 600k == threshold → no rehash needed
        Properties props = new Properties();
        props.put("water.security.password.hash.iterations", String.valueOf(currentIterations));
        applicationProperties.loadProperties(props);

        Assertions.assertFalse(passwordHashService.needsRehash(currentPhc),
                "needsRehash() must return false for a hash whose iterations equal the current configured value (600 000)");

        // Restore to low iteration count
        Properties restore = new Properties();
        restore.put("water.security.password.hash.iterations", "1000");
        applicationProperties.loadProperties(restore);
    }

    /**
     * PBKDF2-600k-4: matches() remains correct for hashes produced at 600 000 iterations.
     * Uses a low custom iteration count so the test is fast; the important property being
     * tested is that the matches() logic reads the iteration count from the PHC string
     * (it is self-describing) and computes correctly regardless of the current default.
     */
    @Test
    @Order(23)
    void matchesCorrectPasswordFor600kIterationsReturnsTrue() {
        // Use a moderate number of iterations to keep the test fast while still
        // exercising the "read iterations from PHC" path
        final int testIterations = 2000;
        Properties props = new Properties();
        props.put("water.security.password.hash.iterations", String.valueOf(testIterations));
        applicationProperties.loadProperties(props);

        char[] pwd = "Pbkdf2600kMatch1!".toCharArray();
        String phc = passwordHashService.hash(pwd);

        String[] parts = phc.split("\\$");
        int embeddedIterations = Integer.parseInt(parts[2].substring(2));
        Assertions.assertEquals(testIterations, embeddedIterations,
                "PHC string must embed the configured iteration count");

        // Correct password must match
        Assertions.assertTrue(passwordHashService.matches(pwd, phc),
                "matches() must return true for the correct password against a fresh PHC hash");

        // Wrong password must not match
        Assertions.assertFalse(passwordHashService.matches("WrongPassword99!".toCharArray(), phc),
                "matches() must return false for an incorrect password");

        // Restore
        Properties restore = new Properties();
        restore.put("water.security.password.hash.iterations", "1000");
        applicationProperties.loadProperties(restore);
    }
}
