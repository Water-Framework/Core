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
     */
    Cipher getCipherRSAECB(String padding) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * @param ecb
     * @return
     */
    Cipher getCipherRSAPKCS1Padding(boolean ecb);

    /**
     * @return
     */
    Cipher getCipherRSAOAEPPAdding();

    /**
     * @return Default cipher CBC/PKCS5PADDING
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
}
