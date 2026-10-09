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
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.testing.utils.junit.WaterTestExtension;
import lombok.Setter;
import org.bouncycastle.openssl.PEMException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Tests for the additive EncryptionUtil extension: AES-256-GCM sealing with AAD,
 * RSA-OAEP-SHA256 key wrapping and key pair loading from (named) keystores.
 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EncryptionUtilAeadTest implements Service {

    private static final int AES_KEY_LENGTH = 32;
    private static final int AEAD_OVERHEAD = 29;
    private static final int RSA_2048 = 2048;
    private static final int RSA_2048_BYTES = 256;
    private static final int RSA_OAEP_SHA256_MAX_PAYLOAD = 190;
    private static final int IV_OFFSET = 1;
    private static final int CIPHERTEXT_OFFSET = 13;
    private static final int LARGE_PLAINTEXT_LENGTH = 1024 * 1024;
    private static final byte VERSION_1 = 0x01;
    private static final byte VERSION_2 = 0x02;
    private static final String OPEN_ERROR = "Unable to open sealed data";
    private static final String UNWRAP_ERROR = "Unable to unwrap key";
    private static final String WRAP_ERROR = "Unable to wrap key";
    private static final String SERVER_ALIAS = "server-cert";
    private static final String KEK_ALIAS = "kek";
    private static final String HMAC_ALIAS = "hmac";
    private static final String AES_ALIAS = "aes";
    private static final String SERVER_TYPE_PROPERTY = "water.keystore.type";
    private static final byte[] PLAINTEXT = "top secret payload".getBytes(StandardCharsets.UTF_8);
    private static final byte[] AAD = "tenant=1;entity=42".getBytes(StandardCharsets.UTF_8);

    @Inject
    @Setter
    private EncryptionUtil encryptionUtil;

    @Inject
    @Setter
    private ApplicationProperties applicationProperties;

    private static byte[] randomBytes(int length) {
        byte[] b = new byte[length];
        new SecureRandom().nextBytes(b);
        return b;
    }

    private static byte[] flipBit(byte[] source, int index) {
        byte[] copy = Arrays.copyOf(source, source.length);
        copy[index] = (byte) (copy[index] ^ 0x01);
        return copy;
    }

    // ---------------------------------------------------------------- AEAD round trip

    @Test
    void sealOpenAead_withAad_roundTrip() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(key, sealed, AAD));
    }

    @Test
    void sealOpenAead_emptyAad_roundTrip() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, new byte[0]);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(key, sealed, new byte[0]));
    }

    @Test
    void sealOpenAead_nullAad_roundTrip() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, null);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(key, sealed, null));
    }

    @Test
    void sealOpenAead_nullAndEmptyAad_areInterchangeable() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealedWithNull = encryptionUtil.sealAead(key, PLAINTEXT, null);
        byte[] sealedWithEmpty = encryptionUtil.sealAead(key, PLAINTEXT, new byte[0]);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(key, sealedWithNull, new byte[0]));
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(key, sealedWithEmpty, null));
    }

    @Test
    void sealOpenAead_emptyPlaintext_producesMinimalOutput() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, new byte[0], AAD);
        Assertions.assertEquals(AEAD_OVERHEAD, sealed.length);
        Assertions.assertEquals(0, encryptionUtil.openAead(key, sealed, AAD).length);
    }

    @Test
    void sealOpenAead_largePlaintext_roundTrip() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] big = randomBytes(LARGE_PLAINTEXT_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, big, AAD);
        Assertions.assertEquals(big.length + AEAD_OVERHEAD, sealed.length);
        Assertions.assertArrayEquals(big, encryptionUtil.openAead(key, sealed, AAD));
    }

    // ---------------------------------------------------------------- AEAD format

    @Test
    void sealAead_format_versionByteAndLength() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        Assertions.assertEquals(VERSION_1, sealed[0]);
        Assertions.assertEquals(AEAD_OVERHEAD + PLAINTEXT.length, sealed.length);
    }

    @Test
    void sealAead_sameInputTwice_producesDifferentOutputs() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] first = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        byte[] second = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        Assertions.assertFalse(Arrays.equals(first, second));
    }

    // ---------------------------------------------------------------- openAead failures

    @Test
    void openAead_wrongKey_throwsGenericErrorWithoutCause() {
        byte[] sealed = encryptionUtil.sealAead(randomBytes(AES_KEY_LENGTH), PLAINTEXT, AAD);
        byte[] otherKey = randomBytes(AES_KEY_LENGTH);
        assertOpenFails(otherKey, sealed, AAD, true);
    }

    @Test
    void openAead_wrongAad_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        assertOpenFails(key, sealed, "other-aad".getBytes(StandardCharsets.UTF_8), true);
    }

    @Test
    void openAead_aadAtSealButNullAtOpen_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        assertOpenFails(key, sealed, null, true);
    }

    @Test
    void openAead_nullAadAtSealButAadAtOpen_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, null);
        assertOpenFails(key, sealed, AAD, true);
    }

    @Test
    void openAead_flippedBitInIv_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        assertOpenFails(key, flipBit(sealed, IV_OFFSET), AAD, true);
    }

    @Test
    void openAead_flippedBitInCiphertext_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        assertOpenFails(key, flipBit(sealed, CIPHERTEXT_OFFSET), AAD, true);
    }

    @Test
    void openAead_flippedBitInTag_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        assertOpenFails(key, flipBit(sealed, sealed.length - 1), AAD, true);
    }

    @Test
    void openAead_unsupportedVersion_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        sealed[0] = VERSION_2;
        assertOpenFails(key, sealed, AAD, false);
    }

    @Test
    void openAead_tooShortData_throwsGenericError() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        byte[] shortData = new byte[AEAD_OVERHEAD - 1];
        shortData[0] = VERSION_1;
        assertOpenFails(key, shortData, AAD, false);
        assertOpenFails(key, new byte[0], AAD, false);
    }

    private void assertOpenFails(byte[] key, byte[] sealed, byte[] aad, boolean expectNoCause) {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.openAead(key, sealed, aad));
        Assertions.assertEquals(OPEN_ERROR, ex.getMessage());
        if (expectNoCause)
            Assertions.assertNull(ex.getCause());
    }

    // ---------------------------------------------------------------- AEAD argument validation

    @Test
    void sealAead_nullKey_throwsIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.sealAead(null, PLAINTEXT, AAD));
    }

    @Test
    void sealAead_invalidKeyLengths_throwIllegalArgument() {
        for (int len : new int[]{16, 31, 33}) {
            byte[] badKey = randomBytes(len);
            Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.sealAead(badKey, PLAINTEXT, AAD));
        }
    }

    @Test
    void sealAead_nullPlaintext_throwsIllegalArgument() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.sealAead(key, null, AAD));
    }

    @Test
    void openAead_nullKey_throwsIllegalArgument() {
        byte[] sealed = encryptionUtil.sealAead(randomBytes(AES_KEY_LENGTH), PLAINTEXT, AAD);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.openAead(null, sealed, AAD));
    }

    @Test
    void openAead_invalidKeyLengths_throwIllegalArgument() {
        byte[] sealed = encryptionUtil.sealAead(randomBytes(AES_KEY_LENGTH), PLAINTEXT, AAD);
        for (int len : new int[]{16, 31, 33}) {
            byte[] badKey = randomBytes(len);
            Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.openAead(badKey, sealed, AAD));
        }
    }

    @Test
    void openAead_nullSealed_throwsIllegalArgument() {
        byte[] key = randomBytes(AES_KEY_LENGTH);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.openAead(key, null, AAD));
    }

    // ---------------------------------------------------------------- RSA-OAEP wrap / unwrap

    @Test
    void wrapUnwrapKeyWithRSAOAEP_roundTrip() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] dek = randomBytes(AES_KEY_LENGTH);
        byte[] wrapped = encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), dek);
        Assertions.assertEquals(RSA_2048_BYTES, wrapped.length);
        Assertions.assertArrayEquals(dek, encryptionUtil.unwrapKeyWithRSAOAEP(pair.getPrivate(), wrapped));
    }

    @Test
    void wrapKeyWithRSAOAEP_isRandomized() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] dek = randomBytes(AES_KEY_LENGTH);
        Assertions.assertFalse(Arrays.equals(
                encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), dek),
                encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), dek)));
    }

    @Test
    void unwrapKeyWithRSAOAEP_differentPrivateKey_throwsGenericError() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        KeyPair other = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] wrapped = encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), randomBytes(AES_KEY_LENGTH));
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.unwrapKeyWithRSAOAEP(other.getPrivate(), wrapped));
        Assertions.assertEquals(UNWRAP_ERROR, ex.getMessage());
        Assertions.assertNull(ex.getCause());
    }

    @Test
    void unwrapKeyWithRSAOAEP_tamperedData_throwsGenericError() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] wrapped = encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), randomBytes(AES_KEY_LENGTH));
        byte[] tampered = flipBit(wrapped, wrapped.length - 1);
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.unwrapKeyWithRSAOAEP(pair.getPrivate(), tampered));
        Assertions.assertEquals(UNWRAP_ERROR, ex.getMessage());
    }

    @Test
    void unwrapKeyWithRSAOAEP_wrongLengthData_throwsGenericError() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] shortData = randomBytes(AES_KEY_LENGTH);
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.unwrapKeyWithRSAOAEP(pair.getPrivate(), shortData));
        Assertions.assertEquals(UNWRAP_ERROR, ex.getMessage());
    }

    @Test
    void unwrapKeyWithRSAOAEP_nonRsaKey_throwsGenericErrorWithCause() throws NoSuchAlgorithmException {
        KeyPair ecPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        byte[] data = randomBytes(RSA_2048_BYTES);
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.unwrapKeyWithRSAOAEP(ecPair.getPrivate(), data));
        Assertions.assertEquals(UNWRAP_ERROR, ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
    }

    @Test
    void wrapKeyWithRSAOAEP_nonRsaKey_throwsWrapError() throws NoSuchAlgorithmException {
        KeyPair ecPair = KeyPairGenerator.getInstance("EC").generateKeyPair();
        byte[] dek = randomBytes(AES_KEY_LENGTH);
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.wrapKeyWithRSAOAEP(ecPair.getPublic(), dek));
        Assertions.assertEquals(WRAP_ERROR, ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
    }

    @Test
    void wrapKeyWithRSAOAEP_nullPublicKey_throwsIllegalArgument() {
        byte[] dek = randomBytes(AES_KEY_LENGTH);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.wrapKeyWithRSAOAEP(null, dek));
    }

    @Test
    void wrapKeyWithRSAOAEP_nullOrEmptyMaterial_throwsIllegalArgument() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), new byte[0]));
    }

    @Test
    void wrapKeyWithRSAOAEP_materialTooLarge_throwsWaterRuntimeException() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] tooBig = randomBytes(RSA_OAEP_SHA256_MAX_PAYLOAD + 1);
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), tooBig));
        Assertions.assertEquals(WRAP_ERROR, ex.getMessage());
    }

    @Test
    void wrapKeyWithRSAOAEP_maxPayload_roundTrip() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] max = randomBytes(RSA_OAEP_SHA256_MAX_PAYLOAD);
        byte[] wrapped = encryptionUtil.wrapKeyWithRSAOAEP(pair.getPublic(), max);
        Assertions.assertArrayEquals(max, encryptionUtil.unwrapKeyWithRSAOAEP(pair.getPrivate(), wrapped));
    }

    @Test
    void unwrapKeyWithRSAOAEP_nullPrivateKey_throwsIllegalArgument() {
        byte[] data = randomBytes(RSA_2048_BYTES);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.unwrapKeyWithRSAOAEP(null, data));
    }

    @Test
    void unwrapKeyWithRSAOAEP_nullOrEmptyWrappedKey_throwsIllegalArgument() {
        KeyPair pair = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.unwrapKeyWithRSAOAEP(pair.getPrivate(), null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.unwrapKeyWithRSAOAEP(pair.getPrivate(), new byte[0]));
    }

    // ---------------------------------------------------------------- full envelope

    @Test
    void envelope_sealWrapUnwrapOpen_returnsOriginalPlaintext() {
        KeyPair kek = encryptionUtil.generateSSLKeyPairValue(RSA_2048);
        byte[] dek = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(dek, PLAINTEXT, AAD);
        byte[] wrappedDek = encryptionUtil.wrapKeyWithRSAOAEP(kek.getPublic(), dek);

        byte[] recoveredDek = encryptionUtil.unwrapKeyWithRSAOAEP(kek.getPrivate(), wrappedDek);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(recoveredDek, sealed, AAD));
    }

    @Test
    void envelope_withKeystoreKeyPair_returnsOriginalPlaintext() {
        KeyPair kek = encryptionUtil.getKeyPairByAlias("kek", KEK_ALIAS);
        byte[] dek = randomBytes(AES_KEY_LENGTH);
        byte[] sealed = encryptionUtil.sealAead(dek, PLAINTEXT, AAD);
        byte[] wrappedDek = encryptionUtil.wrapKeyWithRSAOAEP(kek.getPublic(), dek);

        byte[] recoveredDek = encryptionUtil.unwrapKeyWithRSAOAEP(kek.getPrivate(), wrappedDek);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(recoveredDek, sealed, AAD));
    }

    // ---------------------------------------------------------------- key pair by alias

    @Test
    void getKeyPairByAlias_serverAlias_matchesServerKeyPair() {
        KeyPair byAlias = encryptionUtil.getKeyPairByAlias(SERVER_ALIAS);
        KeyPair server = encryptionUtil.getServerKeyPair();
        Assertions.assertEquals(server.getPublic(), byAlias.getPublic());
        Assertions.assertNotNull(byAlias.getPrivate());
    }

    @Test
    void getKeyPairByAlias_nullOrBlankKeystoreName_usesServerKeystore() {
        KeyPair server = encryptionUtil.getServerKeyPair();
        Assertions.assertEquals(server.getPublic(), encryptionUtil.getKeyPairByAlias(null, SERVER_ALIAS).getPublic());
        Assertions.assertEquals(server.getPublic(), encryptionUtil.getKeyPairByAlias("", SERVER_ALIAS).getPublic());
        Assertions.assertEquals(server.getPublic(), encryptionUtil.getKeyPairByAlias("   ", SERVER_ALIAS).getPublic());
    }

    @Test
    void getKeyPairByAlias_unknownAliasOnServerKeystore_throwsWaterRuntimeException() {
        Assertions.assertThrows(WaterRuntimeException.class, () -> encryptionUtil.getKeyPairByAlias("missing-alias"));
    }

    @Test
    void getKeyPairByAlias_blankAlias_throwsIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getKeyPairByAlias(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getKeyPairByAlias(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getKeyPairByAlias("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getKeyPairByAlias("kek", " "));
    }

    @Test
    void getKeyPairByAlias_namedKeystoreWithoutType_defaultsToPkcs12() {
        KeyPair pair = encryptionUtil.getKeyPairByAlias("kek", KEK_ALIAS);
        Assertions.assertNotNull(pair.getPublic());
        Assertions.assertNotNull(pair.getPrivate());
        //"kek" has no type and points to the PKCS12 file, "kekp12" is the same file with explicit type
        Assertions.assertEquals(encryptionUtil.getKeyPairByAlias("kekp12", KEK_ALIAS).getPublic(), pair.getPublic());
    }

    @Test
    void getKeyPairByAlias_namedJksKeystoreWithoutType_backwardCompatibleViaJdkCompatMode() {
        KeyPair pair = encryptionUtil.getKeyPairByAlias("kekjksdefault", KEK_ALIAS);
        Assertions.assertNotNull(pair.getPublic());
        Assertions.assertNotNull(pair.getPrivate());
    }

    @Test
    void getKeyPairByAlias_namedJksKeystoreWithClasspathExplicitTypeAndKeyPassword_success() {
        KeyPair pair = encryptionUtil.getKeyPairByAlias("kekcp", KEK_ALIAS);
        //same JKS file loaded without explicit type
        KeyPair reference = encryptionUtil.getKeyPairByAlias("kekjksdefault", KEK_ALIAS);
        Assertions.assertEquals(reference.getPublic(), pair.getPublic());
    }

    @Test
    void getKeyPairByAlias_namedPkcs12Keystore_success() {
        KeyPair pair = encryptionUtil.getKeyPairByAlias("kekp12", KEK_ALIAS);
        Assertions.assertNotNull(pair.getPublic());
        Assertions.assertNotNull(pair.getPrivate());
    }

    @Test
    void getKeyPairByAlias_namedKeystoreUnknownAlias_throwsWaterRuntimeException() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getKeyPairByAlias("kek", "unknown"));
        Assertions.assertTrue(ex.getMessage().contains("No key pair found"));
    }

    @Test
    void getKeyPairByAlias_keystoreNotConfigured_throwsNotConfigured() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getKeyPairByAlias("neverconfigured", KEK_ALIAS));
        Assertions.assertTrue(ex.getMessage().contains("is not configured"));
    }

    @Test
    void getKeyPairByAlias_passwordMissing_throwsNotConfigured() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getKeyPairByAlias("nopwd", KEK_ALIAS));
        Assertions.assertTrue(ex.getMessage().contains("is not configured"));
    }

    @Test
    void getKeyPairByAlias_classpathFileNotFound_throwsNotConfigured() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getKeyPairByAlias("nofile", KEK_ALIAS));
        Assertions.assertTrue(ex.getMessage().contains("is not configured"));
    }

    @Test
    void getKeyPairByAlias_wrongKeystorePassword_throwsWaterRuntimeException() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getKeyPairByAlias("badpwd", KEK_ALIAS));
        Assertions.assertTrue(ex.getMessage().contains("No key pair found"));
    }

    @Test
    void getKeyPairByAlias_wrongKeyPassword_throwsWaterRuntimeException() {
        Assertions.assertThrows(WaterRuntimeException.class, () -> encryptionUtil.getKeyPairByAlias("badkeypwd", KEK_ALIAS));
    }

    @Test
    void getKeyPairByAlias_invalidKeystoreNames_throwIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getKeyPairByAlias("a.b", KEK_ALIAS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getKeyPairByAlias("../x", KEK_ALIAS));
    }

    // ---------------------------------------------------------------- getServerKeystoreFilePath regression

    @Test
    void getServerKeystoreFilePath_plainPath_isReturnedAsIs() {
        Assertions.assertEquals("src/test/resources/certs/server.keystore", encryptionUtil.getServerKeystoreFilePath());
    }

    @Test
    void getServerKeystoreFilePath_classpathPrefix_isResolvedAndMissingResourceYieldsEmpty() {
        String original = String.valueOf(applicationProperties.getProperty("water.keystore.file"));
        try {
            setProperty("water.keystore.file", "classpath:certs/server.keystore");
            String resolved = encryptionUtil.getServerKeystoreFilePath();
            Assertions.assertTrue(resolved.endsWith("certs/server.keystore"));
            Assertions.assertFalse(resolved.startsWith("classpath:"));

            setProperty("water.keystore.file", "classpath:certs/not-there.keystore");
            Assertions.assertEquals("", encryptionUtil.getServerKeystoreFilePath());

            setProperty("water.keystore.file", "");
            Assertions.assertEquals("", encryptionUtil.getServerKeystoreFilePath());
        } finally {
            setProperty("water.keystore.file", original);
        }
        Assertions.assertEquals(original, encryptionUtil.getServerKeystoreFilePath());
    }

    // ---------------------------------------------------------------- server keystore type (water.keystore.type)

    @Test
    void serverKeystore_withoutTypeProperty_loadsAsJks() throws PEMException {
        Assertions.assertNotNull(encryptionUtil.getServerKeyPair().getPrivate());
        Assertions.assertNotNull(encryptionUtil.getServerRootCert());
    }

    @Test
    void serverKeystore_typePkcs12_stillLoadsThroughCompatMode() throws Exception {
        String original = originalServerType();
        try {
            setProperty(SERVER_TYPE_PROPERTY, "PKCS12");
            Assertions.assertNotNull(encryptionUtil.getServerKeyPair().getPrivate());
            Assertions.assertNotNull(encryptionUtil.getServerRootCert());
            Assertions.assertNotNull(encryptionUtil.getKeyPairByAlias(SERVER_ALIAS).getPrivate());
        } finally {
            setProperty(SERVER_TYPE_PROPERTY, original);
        }
    }

    @Test
    void serverKeystore_bogusType_getServerKeyPairThrowsWaterRuntimeException() {
        String original = originalServerType();
        try {
            setProperty(SERVER_TYPE_PROPERTY, "BOGUS");
            Assertions.assertThrows(WaterRuntimeException.class, () -> encryptionUtil.getServerKeyPair());
        } finally {
            setProperty(SERVER_TYPE_PROPERTY, original);
        }
    }

    @Test
    void serverKeystore_bogusType_getServerRootCertThrowsPemException() {
        String original = originalServerType();
        try {
            setProperty(SERVER_TYPE_PROPERTY, "BOGUS");
            PEMException ex = Assertions.assertThrows(PEMException.class, () -> encryptionUtil.getServerRootCert());
            Assertions.assertTrue(ex.getMessage().contains("unable to convert key pair"));
        } finally {
            setProperty(SERVER_TYPE_PROPERTY, original);
        }
    }

    @Test
    void serverKeystore_bogusType_getKeyPairByAliasThrowsNoKeyPairFound() {
        String original = originalServerType();
        try {
            setProperty(SERVER_TYPE_PROPERTY, "BOGUS");
            WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                    () -> encryptionUtil.getKeyPairByAlias(SERVER_ALIAS));
            Assertions.assertTrue(ex.getMessage().contains("No key pair found"));
        } finally {
            setProperty(SERVER_TYPE_PROPERTY, original);
        }
    }

    @Test
    void serverKeystore_blankType_fallsBackToJks() throws PEMException {
        String original = originalServerType();
        try {
            setProperty(SERVER_TYPE_PROPERTY, "   ");
            Assertions.assertNotNull(encryptionUtil.getServerKeyPair().getPrivate());
            Assertions.assertNotNull(encryptionUtil.getServerRootCert());
            Assertions.assertNotNull(encryptionUtil.getKeyPairByAlias(SERVER_ALIAS).getPrivate());
        } finally {
            setProperty(SERVER_TYPE_PROPERTY, original);
        }
    }

    // ---------------------------------------------------------------- secret key by alias

    @Test
    void getSecretKeyByAlias_hmacEntry_returns32BytesUsableWithMac() throws Exception {
        byte[] key = encryptionUtil.getSecretKeyByAlias("hmac", HMAC_ALIAS);
        Assertions.assertEquals(AES_KEY_LENGTH, key.length);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        Assertions.assertEquals(AES_KEY_LENGTH, mac.doFinal(PLAINTEXT).length);
    }

    @Test
    void getSecretKeyByAlias_aesEntry_returns32BytesUsableForAeadRoundTrip() {
        byte[] key = encryptionUtil.getSecretKeyByAlias("hmac", AES_ALIAS);
        Assertions.assertEquals(AES_KEY_LENGTH, key.length);
        byte[] sealed = encryptionUtil.sealAead(key, PLAINTEXT, AAD);
        Assertions.assertArrayEquals(PLAINTEXT, encryptionUtil.openAead(key, sealed, AAD));
    }

    @Test
    void getSecretKeyByAlias_calledTwice_returnsEqualButDistinctArrays() {
        byte[] first = encryptionUtil.getSecretKeyByAlias("hmac", HMAC_ALIAS);
        byte[] second = encryptionUtil.getSecretKeyByAlias("hmac", HMAC_ALIAS);
        Assertions.assertArrayEquals(first, second);
        Assertions.assertNotSame(first, second);
        //the caller owns the array: zeroing it must not affect later calls
        Arrays.fill(first, (byte) 0);
        Assertions.assertArrayEquals(second, encryptionUtil.getSecretKeyByAlias("hmac", HMAC_ALIAS));
    }

    @Test
    void getSecretKeyByAlias_hmacAndAesEntries_areDifferentKeys() {
        Assertions.assertFalse(Arrays.equals(
                encryptionUtil.getSecretKeyByAlias("hmac", HMAC_ALIAS),
                encryptionUtil.getSecretKeyByAlias("hmac", AES_ALIAS)));
    }

    @Test
    void getSecretKeyByAlias_unknownAlias_throwsNoSecretKeyFound() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getSecretKeyByAlias("hmac", "unknown"));
        Assertions.assertTrue(ex.getMessage().startsWith("No secret key found for alias"));
    }

    @Test
    void getSecretKeyByAlias_aliasHoldingPrivateKey_throwsNoSecretKeyFound() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getSecretKeyByAlias("kekp12", KEK_ALIAS));
        Assertions.assertTrue(ex.getMessage().startsWith("No secret key found for alias"));
    }

    @Test
    void getKeyPairByAlias_aliasHoldingSecretKey_throwsNoKeyPairFound() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getKeyPairByAlias("hmac", HMAC_ALIAS));
        Assertions.assertTrue(ex.getMessage().contains("No key pair found"));
    }

    @Test
    void getSecretKeyByAlias_blankOrNullAlias_throwsIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getSecretKeyByAlias("hmac", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getSecretKeyByAlias("hmac", ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getSecretKeyByAlias("hmac", "   "));
    }

    @Test
    void getSecretKeyByAlias_invalidKeystoreNames_throwIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getSecretKeyByAlias("a.b", HMAC_ALIAS));
        Assertions.assertThrows(IllegalArgumentException.class, () -> encryptionUtil.getSecretKeyByAlias("../x", HMAC_ALIAS));
    }

    @Test
    void getSecretKeyByAlias_keystoreNotConfigured_throwsNotConfigured() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getSecretKeyByAlias("neverconfigured", HMAC_ALIAS));
        Assertions.assertTrue(ex.getMessage().contains("is not configured"));
    }

    @Test
    void getSecretKeyByAlias_wrongKeystorePassword_throwsWaterRuntimeException() {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getSecretKeyByAlias("hmacbadpwd", HMAC_ALIAS));
        Assertions.assertTrue(ex.getMessage().startsWith("No secret key found for alias"));
    }

    @Test
    void getSecretKeyByAlias_nullNameOnServerKeystore_throwsWaterRuntimeException() {
        //the server keystore (JKS) cannot hold secret keys and has no such alias
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class,
                () -> encryptionUtil.getSecretKeyByAlias(null, HMAC_ALIAS));
        Assertions.assertTrue(ex.getMessage().startsWith("No secret key found for alias"));
    }

    // ---------------------------------------------------------------- no leak of paths / passwords in messages

    @Test
    void keystoreFailures_messagesNeverContainPathOrPassword() {
        List<String> messages = new ArrayList<>();
        messages.add(messageOf(() -> encryptionUtil.getSecretKeyByAlias("hmacbadpwd", HMAC_ALIAS)));
        messages.add(messageOf(() -> encryptionUtil.getSecretKeyByAlias("hmac", "unknown")));
        messages.add(messageOf(() -> encryptionUtil.getKeyPairByAlias("badpwd", KEK_ALIAS)));
        messages.add(messageOf(() -> encryptionUtil.getKeyPairByAlias("badkeypwd", KEK_ALIAS)));
        messages.add(messageOf(() -> encryptionUtil.getKeyPairByAlias("nopwd", KEK_ALIAS)));
        messages.add(messageOf(() -> encryptionUtil.getKeyPairByAlias("nofile", KEK_ALIAS)));
        for (String message : messages) {
            Assertions.assertNotNull(message);
            Assertions.assertFalse(message.contains("src/test/resources"), message);
            Assertions.assertFalse(message.contains(".p12"), message);
            Assertions.assertFalse(message.contains(".keystore"), message);
            Assertions.assertFalse(message.contains("wrong-password"), message);
            Assertions.assertFalse(message.contains("kektest"), message);
        }
    }

    private String messageOf(Runnable action) {
        WaterRuntimeException ex = Assertions.assertThrows(WaterRuntimeException.class, action::run);
        return ex.getMessage();
    }

    private String originalServerType() {
        //absent property is equivalent to a blank one (both mean JKS), so "" is a faithful restore value
        return applicationProperties.getPropertyOrDefault(SERVER_TYPE_PROPERTY, "");
    }

    private void setProperty(String key, String value) {
        Properties p = new Properties();
        p.put(key, value);
        applicationProperties.loadProperties(p);
    }
}
