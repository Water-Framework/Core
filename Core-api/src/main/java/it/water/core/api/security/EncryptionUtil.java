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
package it.water.core.api.security;

import it.water.core.api.service.Service;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.x500.X500PrivateCredential;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public interface EncryptionUtil extends Service {
    /**
     * @return
     */
    String getEncryptionAlgorithm();

    /**
     * Generates KeyPair value with 2048 bytes
     *
     * @return
     */

    //Returning keystore save in it.water.jwt.config
    String getServerKeystoreFilePath();

    //Returning keystore save in it.water.jwt.config
    String getServerKeystorePassword();

    //Returning keystore save in it.water.jwt.config
    String getServerKeyPassword();

    //Returning keystore save in it.water.jwt.config
    String getServerKeystoreAlias();

    /**
     * @param keySize
     * @return
     */
    KeyPair generateSSLKeyPairValue(int keySize);

    /**
     * @param pair
     * @param subjectString
     * @return
     * @throws Exception
     */
    PKCS10CertificationRequest generateCertificationRequest(KeyPair pair, String subjectString) throws OperatorCreationException;

    /**
     * @param subjectStr
     * @param validDays
     * @param keyPair
     * @param caCert
     * @return
     */
    X500PrivateCredential createServerClientX509Cert(String subjectStr, int validDays, KeyPair keyPair, java.security.cert.Certificate caCert);

    /**
     * @return
     * @throws PEMException
     */
    java.security.cert.Certificate getServerRootCert() throws PEMException;

    /**
     * @return the key Pair associated with the current instance of this server
     */
    KeyPair getServerKeyPair();

    /**
     * @param publicKey current publick key
     * @return String rapresentation of the publick key
     */
    String getPublicKeyString(PublicKey publicKey);

    String getPrivateKeyString(PrivateKey privateKey);

    /**
     * Accepts PKCS8 Keys only
     *
     * @param key
     * @return
     */
    PublicKey getPublicKeyFromString(String key);

    /**
     * Accepts PKCS8 Keys only
     *
     * @param key
     * @return
     */
    PrivateKey getPrivateKeyFromString(String key);

    /**
     * @param padding
     * @return
     */
    Cipher getCipherRSA(String padding);

    /**
     * @param padding
     * @return
     * @deprecated Exposes RSA in ECB mode and, when called with PKCS#1 v1.5 padding,
     * the classic Bleichenbacher padding-oracle attack surface. Prefer the OAEP-SHA256
     * default ({@link #getCipherRSAOAEPPAdding()}). Scheduled for removal.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    Cipher getCipherRSAECB(String padding) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * @param ecb
     * @return
     * @deprecated Uses RSA PKCS#1 v1.5 padding, which is vulnerable to Bleichenbacher
     * padding-oracle attacks. Prefer the OAEP-SHA256 default ({@link #getCipherRSAOAEPPAdding()}).
     * Scheduled for removal.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    Cipher getCipherRSAPKCS1Padding(boolean ecb);

    /**
     * @return
     */
    Cipher getCipherRSAOAEPPAdding();

    /**
     * @return Default cipher CBC/PKCS5PADDING.
     * CBC provides confidentiality only (no integrity): for new data prefer the authenticated
     * encryption API {@link #sealAead(byte[], byte[], byte[])} / {@link #openAead(byte[], byte[], byte[])}.
     */
    Cipher getCipherAES();

    /**
     * @param padding
     * @return
     */
    Cipher getCipherAES(String padding) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * @param plainTextMessage
     * @param publicKeyBytes   Bytes of the key String of the pem file
     * @return
     */
    byte[] encodeMessageWithPublicKey(byte[] plainTextMessage, byte[] publicKeyBytes);

    /**
     * @param plainTextMessage
     * @param privateKeyBytes  Bytes of the key String of the pem file
     * @return
     */
    byte[] encodeMessageWithPrivateKey(byte[] plainTextMessage, byte[] privateKeyBytes);

    /**
     * @param cipherText     Encrypted Text
     * @param publicKeyBytes Public Key encoded bytes
     * @return decoded String message
     */
    byte[] decodeMessageWithPublicKey(byte[] cipherText, byte[] publicKeyBytes);

    /**
     * @param cipherText Encrypted Text
     * @return Decoded String message using the current private key loaded form the keystore
     */
    byte[] decodeMessageWithServerPrivateKey(byte[] cipherText, Cipher asymmetricCipher);

    /**
     * @param cipherText Encrypted Text
     * @return Decoded String message using the current private key loaded form the keystore
     */
    byte[] decodeMessageWithPrivateKey(PrivateKey key, byte[] cipherText);

    /**
     * @param cipherText Encrypted Text
     * @return Decoded String message using the current private key loaded form the keystore
     */
    byte[] decodeMessageWithPrivateKey(PrivateKey key, byte[] cipherText, Cipher asymmetricCipher);

    /**
     * Create a signature of the input string with the server private certificate.
     *
     * @param data         data to be signed
     * @param encodeBase64 true if you want the result be encoded in base64
     * @return
     */
    byte[] signDataWithServerCert(byte[] data, boolean encodeBase64);

    /**
     * Verifies signed data with server cert
     *
     * @param inputData
     * @param signedData
     * @param decodeSignedDataFromBase64
     * @return
     */
    boolean verifyDataSignedWithServerCert(byte[] inputData, byte[] signedData, boolean decodeSignedDataFromBase64);

    /**
     * @param plainTextMessage plain text message
     * @param cipherText       encrypted challenge text
     * @param publicKeyBytes   public key
     * @return true if plain text message and decrypted cipherText are equal
     */
    boolean checkChallengeMessage(String plainTextMessage, String cipherText, byte[] publicKeyBytes);

    /**
     * @param pk   Private Key
     * @param text String to encrypt
     * @return encrypted text
     */
    byte[] encryptText(PrivateKey pk, byte[] text, boolean encodeInBase64, Cipher asymmetricCipher);

    /**
     * @param pk   Publick key
     * @param text String to encrypt
     * @return encrypted text
     */
    byte[] encryptText(PublicKey pk, byte[] text, boolean encodeInBase64, Cipher asymmetricCipher);

    /**
     * @return Random 32 byte length password
     */
    byte[] generateRandomAESPassword() throws NoSuchAlgorithmException;

    /**
     * @return Random byte init vector
     */
    IvParameterSpec generateRandomAESInitVector() throws NoSuchAlgorithmException;

    /**
     * Generates AES key from a basic password
     *
     * @param password
     * @param salt
     * @param hashMethod
     * @return
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    byte[] getAESKeyFromPassword(String password, String hashMethod, byte[] salt, int numIterations, int keyBitSize) throws InvalidKeySpecException, NoSuchAlgorithmException;

    /**
     * @param aesPassword secret aes key
     * @param content     message content
     * @param aesCipher   AES Cipher
     * @return Encrypted text as a String encoded in base 64 ivBytes+encryptedBytes
     */
    byte[] encryptWithAES(byte[] aesPassword, String content, Cipher aesCipher);


    /**
     * @param aesPassword secret aes key
     * @param salt        salt
     * @param content     content
     * @param aesCipher   AES Cipher
     * @return Encrypted text as a String encoded in base 64 salt+ivBytes+encryptedBytes
     */
    byte[] encryptWithAES(byte[] aesPassword, byte[] salt, String content, Cipher aesCipher);

    /**
     * @param aesPassword secret aes key
     * @param content     Content to decrypt
     * @return Decrypted text as a String
     * @throws InvalidKeyException      Invalid key exception
     * @throws NoSuchPaddingException   No Such padding
     * @throws NoSuchAlgorithmException No Such Algotithm
     */
    byte[] decryptWithAES(byte[] aesPassword, byte[] initVector, String content, Cipher aesCipher) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException;

    /**
     * @param password generates password hash.
     * @param salt necessary to compare passwords
     * Default algorithm is PBKDF2.
     * @return
     */
    byte[] hashPassword(byte[] salt, String password) throws NoSuchAlgorithmException, InvalidKeySpecException;

    /**
     * Generates 16 bytes random salt
     * @return
     */
    byte[] generate16BytesSalt();

    /**
     * Generates a cryptographically secure random password of the requested length
     * over a wide alphabet (upper/lower case letters, digits and a few symbols).
     *
     * @param length exact number of characters of the generated password
     * @return the generated password
     */
    String generateRandomPassword(int length);

    /**
     * Authenticated encryption (AEAD) with AES-256-GCM.
     * <p>
     * A fresh 96-bit IV is drawn from {@link SecureRandom} for every call, the authentication tag is 128 bits.
     * The output is self-describing so that the algorithm can evolve without ambiguity:
     * <pre>
     *   version (1 byte, 0x01 = AES-256-GCM, 96-bit IV, 128-bit tag) || IV (12 bytes) || ciphertext + tag (plaintext.length + 16 bytes)
     * </pre>
     * The version byte is itself authenticated (it is bound to the tag together with {@code aad}).
     * The same {@code aad} must be supplied to {@link #openAead(byte[], byte[], byte[])}: it is not stored in the output.
     * This is the preferred API for new data; {@link #getCipherAES()} (CBC) is kept for backward compatibility.
     *
     * @param key       AES key, exactly 32 bytes (AES-256), e.g. from {@link #generateRandomAESPassword()}
     * @param plaintext data to protect, not null (may be empty)
     * @param aad       additional authenticated data bound to the ciphertext (not encrypted, not stored);
     *                  null is equivalent to an empty array
     * @return sealed bytes in the format described above
     * @throws IllegalArgumentException if key or plaintext is null, or key is not 32 bytes long
     */
    byte[] sealAead(byte[] key, byte[] plaintext, byte[] aad);

    /**
     * Inverse of {@link #sealAead(byte[], byte[], byte[])}: verifies the authentication tag and decrypts.
     * <p>
     * Any failure on the sealed data (wrong key, wrong aad, tampered or truncated data, unknown version)
     * raises the same runtime exception with the same generic message, never a partial or null result.
     * The caller owns the returned array and should zero it after use when it holds a secret.
     *
     * @param key    AES key, exactly 32 bytes (AES-256)
     * @param sealed output of {@link #sealAead(byte[], byte[], byte[])}, not null
     * @param aad    the same additional authenticated data used when sealing; null is equivalent to an empty array
     * @return the plaintext
     * @throws IllegalArgumentException if key or sealed is null, or key is not 32 bytes long
     */
    byte[] openAead(byte[] key, byte[] sealed, byte[] aad);

    /**
     * Wraps (encrypts) key material, typically a data encryption key, with an RSA public key using
     * RSA-OAEP with SHA-256 and MGF1-SHA-256 (explicit parameters, provider independent).
     *
     * @param publicKey   RSA public key of the key encryption key, not null
     * @param keyMaterial raw key bytes to wrap, not null nor empty
     * @return the wrapped key
     * @throws IllegalArgumentException on invalid input
     */
    byte[] wrapKeyWithRSAOAEP(PublicKey publicKey, byte[] keyMaterial);

    /**
     * Inverse of {@link #wrapKeyWithRSAOAEP(PublicKey, byte[])}.
     * Any failure (wrong private key, tampered data) raises a runtime exception with a generic message,
     * never an empty or null result. The caller owns the returned array and should zero it after use.
     *
     * @param privateKey RSA private key of the key encryption key, not null
     * @param wrappedKey output of {@link #wrapKeyWithRSAOAEP(PublicKey, byte[])}, not null nor empty
     * @return the raw key material
     * @throws IllegalArgumentException on invalid input
     */
    byte[] unwrapKeyWithRSAOAEP(PrivateKey privateKey, byte[] wrappedKey);

    /**
     * Loads a key pair by alias from the server keystore ({@code water.keystore.file},
     * {@code water.keystore.password}, key password {@code water.private.key.password},
     * optional type {@code water.keystore.type}, default {@code JKS}).
     * Unlike {@link #getServerKeyPair()} the alias is chosen by the caller, so a key other than the server
     * (JWT) one can be kept in the same keystore.
     *
     * @param alias key entry alias, not blank
     * @return the key pair (private key + public key of the entry certificate)
     */
    KeyPair getKeyPairByAlias(String alias);

    /**
     * Loads a key pair by alias from a named keystore configured through application properties:
     * <ul>
     *     <li>{@code water.keystore.<keystoreName>.file} (mandatory, supports the {@code classpath:} prefix)</li>
     *     <li>{@code water.keystore.<keystoreName>.password} (mandatory)</li>
     *     <li>{@code water.keystore.<keystoreName>.key.password} (optional, defaults to the keystore password)</li>
     *     <li>{@code water.keystore.<keystoreName>.type} (optional, defaults to {@code PKCS12}, which unlike
     *     {@code JKS} can also hold secret key entries)</li>
     * </ul>
     * A null or blank {@code keystoreName} selects the server keystore, as {@link #getKeyPairByAlias(String)}
     * (type from {@code water.keystore.type}, default {@code JKS}).
     * Properties are read at every call, so a rotated keystore is picked up without restart.
     *
     * @param keystoreName logical keystore name: letters, digits, '-' and '_' only; null/blank = server keystore
     * @param alias        key entry alias, not blank
     * @return the key pair (private key + public key of the entry certificate)
     * @throws IllegalArgumentException if the alias is blank or the keystore name is invalid
     * @throws RuntimeException         (a WaterRuntimeException) if the keystore is not configured, or the alias
     *                                  is missing / not a private key entry / cannot be loaded
     */
    KeyPair getKeyPairByAlias(String keystoreName, String alias);

    /**
     * Loads the raw bytes of a secret (symmetric) key entry, e.g. an HMAC or AES sealing key, from a keystore.
     * The keystore is resolved exactly as in {@link #getKeyPairByAlias(String, String)}: named keystores default
     * to {@code PKCS12}; a null or blank {@code keystoreName} selects the server keystore, which must then be
     * configured with a type able to hold secret keys ({@code water.keystore.type=PKCS12} or {@code JCEKS}).
     * <p>
     * The key algorithm is not returned: the caller knows what the key is for and rebuilds it, e.g.
     * {@code new SecretKeySpec(bytes, "HmacSHA256")}, or passes it to {@link #sealAead(byte[], byte[], byte[])}.
     * The caller owns the returned array and should zero it after use.
     *
     * @param keystoreName logical keystore name: letters, digits, '-' and '_' only; null/blank = server keystore
     * @param alias        secret key entry alias, not blank
     * @return the encoded secret key bytes, never null nor empty
     * @throws IllegalArgumentException if the alias is blank or the keystore name is invalid
     * @throws RuntimeException         (a WaterRuntimeException) if the keystore is not configured, or the alias
     *                                  is missing / not a secret key entry / not extractable / cannot be loaded
     */
    byte[] getSecretKeyByAlias(String keystoreName, String alias);
}
