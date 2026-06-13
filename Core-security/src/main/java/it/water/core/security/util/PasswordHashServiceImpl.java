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

package it.water.core.security.util;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.api.security.PasswordHashService;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * @Author Aristide Cittadino
 * Default {@link PasswordHashService} based on PBKDF2-HMAC-SHA256 (JDK built-in, no extra deps).
 * Produces {@code $pbkdf2-sha256$i=<iterations>$<base64Salt>$<base64Hash>}; the PHC id makes
 * hashes self-describing, so migrating to Argon2id later only requires changing this component.
 * Legacy values (no leading {@code '$'}) are verified via {@link EncryptionUtil} (PBKDF2-SHA1).
 */
@Slf4j
@FrameworkComponent(services = {PasswordHashService.class})
public class PasswordHashServiceImpl implements PasswordHashService {

    private static final String PHC_ID = "pbkdf2-sha256";
    private static final String PHC_PREFIX = "$" + PHC_ID + "$";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    //OWASP 2023 recommendation for PBKDF2-HMAC-SHA256
    private static final int DEFAULT_ITERATIONS = 600000;

    //H8 - password hashing (PHC / PBKDF2) cost parameter
    private static final String PASSWORD_HASH_ITERATIONS = "water.security.password.hash.iterations";

    private final SecureRandom secureRandom = new SecureRandom();

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    @Inject
    @Setter
    private EncryptionUtil encryptionUtil;

    private int configuredIterations() {
        if (applicationProperties == null)
            return DEFAULT_ITERATIONS;
        Object raw = applicationProperties.getProperty(PASSWORD_HASH_ITERATIONS);
        if (raw == null)
            return DEFAULT_ITERATIONS;
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid value for {} ('{}'), falling back to default {}", PASSWORD_HASH_ITERATIONS, raw, DEFAULT_ITERATIONS);
            return DEFAULT_ITERATIONS;
        }
    }

    @Override
    public String hash(char[] clearTextPassword) {
        int iterations = configuredIterations();
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] digest = pbkdf2(clearTextPassword, salt, iterations);
        return PHC_PREFIX + "i=" + iterations + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(salt) + "$"
                + Base64.getEncoder().withoutPadding().encodeToString(digest);
    }

    @Override
    public boolean matches(char[] clearTextPassword, String storedPhcHash) {
        return matches(clearTextPassword, storedPhcHash, null);
    }

    @Override
    public boolean matches(char[] clearTextPassword, String storedHash, String legacySalt) {
        if (storedHash == null || storedHash.isEmpty())
            return false;
        if (isPhc(storedHash))
            return matchesPhc(clearTextPassword, storedHash);
        return matchesLegacy(clearTextPassword, storedHash, legacySalt);
    }

    @Override
    public boolean needsRehash(String storedHash) {
        if (storedHash == null || storedHash.isEmpty())
            return true;
        if (!isPhc(storedHash))
            return true;
        // PHC hash with stale cost parameters should be upgraded as well
        try {
            String[] parts = storedHash.split("\\$");
            // parts: ["", "pbkdf2-sha256", "i=<n>", salt, hash]
            int iterations = parseIterations(parts[2]);
            return iterations < configuredIterations();
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return true;
        }
    }

    private boolean isPhc(String value) {
        return value.startsWith("$");
    }

    private boolean matchesPhc(char[] clearTextPassword, String storedPhcHash) {
        try {
            String[] parts = storedPhcHash.split("\\$");
            // ["", "pbkdf2-sha256", "i=<n>", base64Salt, base64Hash]
            int iterations = parseIterations(parts[2]);
            byte[] salt = Base64.getDecoder().decode(parts[3]);
            byte[] expected = Base64.getDecoder().decode(parts[4]);
            byte[] actual = pbkdf2(clearTextPassword, salt, iterations);
            return constantTimeEquals(expected, actual);
        } catch (RuntimeException e) { //NOSONAR fail-closed: any parsing/compute error rejects auth
            log.warn("Malformed PHC hash, rejecting authentication");
            return false;
        }
    }

    //Reproduces the historical EncryptionUtil.hashPassword digest so existing accounts keep
    //logging in; callers should rehash via hash(char[]) on a successful match.
    private boolean matchesLegacy(char[] clearTextPassword, String storedHash, String legacySalt) {
        try {
            if (encryptionUtil == null || legacySalt == null || legacySalt.isEmpty())
                return false;
            byte[] saltBytes = Base64.getDecoder().decode(legacySalt);
            String actual = new String(encryptionUtil.hashPassword(saltBytes, new String(clearTextPassword)));
            return constantTimeEquals(actual.getBytes(StandardCharsets.UTF_8),
                    storedHash.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.warn("Unable to verify legacy hash: {}", e.getMessage());
            return false;
        }
    }

    private int parseIterations(String param) {
        // expects "i=<number>"
        int eq = param.indexOf('=');
        return Integer.parseInt(param.substring(eq + 1).trim());
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            KeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            //PBKDF2-HMAC-SHA256 is the intended password hashing primitive
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to compute PBKDF2 hash", e);
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }
}
