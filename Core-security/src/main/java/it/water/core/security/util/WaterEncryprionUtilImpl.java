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
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.WaterRuntimeException;
import lombok.Setter;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;


/**
 * @Author Aristide Cittadino.
 * This class helps developers to interact with encryption/decryption operations.
 */
@FrameworkComponent(services = {EncryptionUtil.class})
public class WaterEncryprionUtilImpl implements EncryptionUtil {
    private final Logger log = LoggerFactory.getLogger(WaterEncryprionUtilImpl.class.getName());
    private static final long MILLIS_PER_DAY = 86400000L;
    private static final String SHA_WITH_RSA_ENC_ALGORITHM = "SHA256withRSA";
    //AEAD (AES-256-GCM) constants: output = version(1) || iv(12) || ciphertext+tag
    private static final byte AEAD_VERSION_AES256_GCM = 0x01;
    private static final int AEAD_KEY_LENGTH = 32;
    private static final int AEAD_IV_LENGTH = 12;
    private static final int AEAD_TAG_BITS = 128;
    private static final int AEAD_HEADER_LENGTH = 1 + AEAD_IV_LENGTH;
    private static final int AEAD_MIN_SEALED_LENGTH = AEAD_HEADER_LENGTH + AEAD_TAG_BITS / 8;
    private static final String AEAD_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String AEAD_OPEN_ERROR = "Unable to open sealed data";
    //RSA-OAEP key wrapping with explicit SHA-256 / MGF1-SHA-256 parameters (provider independent)
    private static final String RSA_OAEP_TRANSFORMATION = "RSA/ECB/OAEPPadding";
    private static final String KEY_UNWRAP_ERROR = "Unable to unwrap key";
    //Named keystores: water.keystore.<name>.file|password|key.password|type
    private static final String KEYSTORE_PROPERTY_PREFIX = "water.keystore.";
    //PKCS12 by default: named keystores may also hold secret (symmetric) key entries, which JKS cannot store
    private static final String DEFAULT_NAMED_KEYSTORE_TYPE = "PKCS12";
    //Server keystore type: optional override, JKS kept as default for backward compatibility
    private static final String SERVER_KEYSTORE_TYPE_PROPERTY = "water.keystore.type";
    private static final String DEFAULT_SERVER_KEYSTORE_TYPE = "JKS";
    private static final String SERVER_KEYSTORE_LABEL = "server";
    private static final Pattern KEYSTORE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    @Inject
    @Setter
    private ApplicationProperties props;

    public WaterEncryprionUtilImpl() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }


    /**
     * @return
     */
    public String getEncryptionAlgorithm() {
        return SHA_WITH_RSA_ENC_ALGORITHM;
    }

    /**
     * Generates KeyPair value with 2048 bytes
     *
     * @return
     */

    public String getServerKeystoreFilePath() {
        return resolveKeystoreFilePath(props.getProperty("water.keystore.file").toString());
    }

    /**
     * Resolves a keystore location, supporting the "classpath:" prefix.
     *
     * @param filePath configured location
     * @return the file system path, or empty string if not configured / not found on the classpath
     */
    private String resolveKeystoreFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        if (filePath.toLowerCase().startsWith("classpath:")) {
            filePath = filePath.substring("classpath:".length());
            URL fileUrl = Thread.currentThread().getContextClassLoader().getResource(filePath);
            if (fileUrl == null) return "";
            return fileUrl.getPath();
        }
        return filePath;
    }


    public String getServerKeystorePassword() {
        return props.getProperty("water.keystore.password").toString();
    }


    public String getServerKeyPassword() {
        return props.getProperty("water.private.key.password").toString();
    }


    public String getServerKeystoreAlias() {
        return props.getProperty("water.keystore.alias").toString();
    }

    /**
     * Single resolution point of the server keystore type.
     *
     * @return the value of {@code water.keystore.type}, or JKS when not set / blank (backward compatible default)
     */
    private String getServerKeystoreType() {
        return resolveKeystoreType(props.getPropertyOrDefault(SERVER_KEYSTORE_TYPE_PROPERTY, DEFAULT_SERVER_KEYSTORE_TYPE), DEFAULT_SERVER_KEYSTORE_TYPE);
    }

    private String resolveKeystoreType(String configuredType, String defaultType) {
        if (configuredType == null || configuredType.isBlank()) return defaultType;
        return configuredType.trim();
    }

    /**
     * @param keySize
     * @return
     */
    public KeyPair generateSSLKeyPairValue(int keySize) {
        try {
            //#41 - use the platform default CSPRNG instead of forcing the legacy SHA1PRNG/SUN provider
            SecureRandom randomGenerator = new SecureRandom();
            final KeyPairGenerator rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
            rsaKeyPairGenerator.initialize(keySize, randomGenerator);
            return rsaKeyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param pair
     * @param subjectString
     * @return
     * @throws Exception
     */
    public PKCS10CertificationRequest generateCertificationRequest(KeyPair pair, String subjectString) throws OperatorCreationException {
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(new X500Principal("CN=" + subjectString), pair.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(SHA_WITH_RSA_ENC_ALGORITHM);
        ContentSigner signer = csBuilder.build(pair.getPrivate());
        return p10Builder.build(signer);
    }

    /**
     * @param subjectStr
     * @param validDays
     * @param keyPair
     * @param caCert
     * @return
     */
    public X500PrivateCredential createServerClientX509Cert(String subjectStr, int validDays, KeyPair keyPair, Certificate caCert) {

        try {
            PKCS10CertificationRequest request = generateCertificationRequest(keyPair, subjectStr);
            BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
            PrivateKey privateKey = this.getServerKeyPair().getPrivate(); // The CA's private key
            Date issuedDate = new Date();
            Date expiryDate = new Date(System.currentTimeMillis() + validDays * MILLIS_PER_DAY); //MILLIS_PER_DAY=86400000l
            JcaPKCS10CertificationRequest jcaRequest = new JcaPKCS10CertificationRequest(request);
            X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder((X509Certificate) caCert, serialNumber, issuedDate, expiryDate, jcaRequest.getSubject(), jcaRequest.getPublicKey());
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCert.getPublicKey())).addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(jcaRequest.getPublicKey())).addExtension(Extension.basicConstraints, true, new BasicConstraints(0));
            ContentSigner signer = new JcaContentSignerBuilder(SHA_WITH_RSA_ENC_ALGORITHM).setProvider("BC").build(privateKey);
            X509Certificate signedCert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateBuilder.build(signer));
            return new X500PrivateCredential(signedCert, keyPair.getPrivate());
        } catch (Exception e) {
            throw new WaterRuntimeException("Error generating certificate", e);
        }

    }

    /**
     * @return
     * @throws PEMException
     */
    public Certificate getServerRootCert() throws PEMException {
        try (FileInputStream fis = new FileInputStream(getServerKeystoreFilePath())) {
            KeyStore keystore = KeyStore.getInstance(getServerKeystoreType());
            keystore.load(fis, getServerKeystorePassword().toCharArray());
            return keystore.getCertificate(getServerKeystoreAlias());
        } catch (Exception e) {
            throw new PEMException("unable to convert key pair: " + e.getMessage(), e);
        }
    }

    /**
     * @return the key Pair associated with the current instance of this server
     */
    public KeyPair getServerKeyPair() {
        try (FileInputStream fis = new FileInputStream(getServerKeystoreFilePath())) {
            KeyStore keystore = KeyStore.getInstance(getServerKeystoreType());
            keystore.load(fis, getServerKeystorePassword().toCharArray());
            String alias = getServerKeystoreAlias();
            Key key = keystore.getKey(alias, getServerKeyPassword().toCharArray());
            if (key instanceof PrivateKey privateKey) {
                // Get certificate of public key
                Certificate cert = keystore.getCertificate(alias);

                // Get public key
                PublicKey publicKey = cert.getPublicKey();

                // Return a key pair
                return new KeyPair(publicKey, privateKey);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        throw new WaterRuntimeException("No server keypair found, or error while loading it");
    }

    /**
     * @param publicKey current publick key
     * @return String rapresentation of the publick key
     */
    public String getPublicKeyString(PublicKey publicKey) {
        try {
            StringWriter writer = new StringWriter();
            PemWriter pemWriter = new PemWriter(writer);
            pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public String getPrivateKeyString(PrivateKey privateKey) {
        try {
            StringWriter writer = new StringWriter();
            PemWriter pemWriter = new PemWriter(writer);
            pemWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Accepts PKCS8 Keys only
     *
     * @param key
     * @return
     */
    public PublicKey getPublicKeyFromString(String key) {
        try {
            String publicKeyPEM = key;
            publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
            publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
            publicKeyPEM = publicKeyPEM.replace("\r", "");
            publicKeyPEM = publicKeyPEM.replace("\n", "");
            byte[] byteKey = Base64.getDecoder().decode(publicKeyPEM.getBytes(StandardCharsets.UTF_8));
            X509EncodedKeySpec x509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(x509publicKey);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Accepts PKCS8 Keys only
     *
     * @param key
     * @return
     */
    public PrivateKey getPrivateKeyFromString(String key) {
        try {
            String privateKeyPEM = key;
            privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
            privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
            privateKeyPEM = privateKeyPEM.replace("\r", "");
            privateKeyPEM = privateKeyPEM.replace("\n", "");
            byte[] byteKey = Base64.getDecoder().decode(privateKeyPEM.getBytes(StandardCharsets.UTF_8));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param padding
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public Cipher getCipherRSA(String padding) {
        try {
            if (padding == null) {
                return Cipher.getInstance("RSA/None/OAEPWITHSHA-256ANDMGF1PADDING", "BC");
            } else {
                return Cipher.getInstance("RSA/NONE/" + padding, "BC");
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | NoSuchProviderException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param padding
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @deprecated Exposes RSA in ECB mode (and PKCS#1 v1.5 padding-oracle surface).
     * Prefer the OAEP-SHA256 default {@link #getCipherRSAOAEPPAdding()}. Scheduled for removal.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Cipher getCipherRSAECB(String padding) throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {

        if (padding == null) {
            return Cipher.getInstance("RSA/None/OAEPWITHSHA-256ANDMGF1PADDING", "BC");
        } else {
            return Cipher.getInstance("RSA/ECB/" + padding, "BC");
        }
    }

    /**
     * @param ecb
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @deprecated Uses RSA PKCS#1 v1.5 padding, vulnerable to Bleichenbacher padding-oracle
     * attacks. Prefer the OAEP-SHA256 default {@link #getCipherRSAOAEPPAdding()}. Scheduled for removal.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public Cipher getCipherRSAPKCS1Padding(boolean ecb) {
        try {
            if (ecb) return getCipherRSAECB("PKCS1PADDING");
            return getCipherRSA("PKCS1PADDING");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    public Cipher getCipherRSAOAEPPAdding() {
        return getCipherRSA("OAEPPadding");
    }

    /**
     * @return Default cipher CBC/PKCS5PADDING
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public Cipher getCipherAES() {
        return getCipherAES("PKCS5PADDING");
    }

    /**
     * @param padding
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    public Cipher getCipherAES(String padding) {
        try {
            return Cipher.getInstance("AES/CBC/" + padding, "BC");
        } catch (NoSuchProviderException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param plainTextMessage
     * @param publicKeyBytes   Bytes of the key String of the pem file
     * @return
     */
    public byte[] encodeMessageWithPublicKey(byte[] plainTextMessage, byte[] publicKeyBytes) {
        PublicKey pk = getPublicKeyFromString(new String(publicKeyBytes));
        Cipher rsaCipher = getCipherRSA(null);
        if (rsaCipher != null)
            return encryptText(pk, plainTextMessage, true, rsaCipher);
        return new byte[]{};
    }

    /**
     * @param plainTextMessage
     * @param privateKeyBytes  Bytes of the key String of the pem file
     * @return
     */
    public byte[] encodeMessageWithPrivateKey(byte[] plainTextMessage, byte[] privateKeyBytes) {
        Cipher rsaCipher = getCipherRSA(null);
        PrivateKey pk = getPrivateKeyFromString(new String(privateKeyBytes));
        if (rsaCipher != null)
            return encryptText(pk, plainTextMessage, true, rsaCipher);
        return new byte[]{};
    }

    /**
     * @param cipherText     Encrypted Text
     * @param publicKeyBytes Public Key encoded bytes
     * @return decoded String message
     */
    public byte[] decodeMessageWithPublicKey(byte[] cipherText, byte[] publicKeyBytes) {
        try {
            // asume, that publicKeyBytes contains a byte array representing
            // your public key
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            Cipher asymmetricCipher = getCipherRSA(null);
            KeyFactory keyFactory;
            keyFactory = KeyFactory.getInstance(publicKeySpec.getFormat());
            Key key = keyFactory.generatePublic(publicKeySpec);
            // initialize your cipher
            asymmetricCipher.init(Cipher.DECRYPT_MODE, key);
            // asuming, cipherText is a byte array containing your encrypted message
            return asymmetricCipher.doFinal(cipherText);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new byte[]{};
    }

    /**
     * @param cipherText Encrypted Text
     * @return Decoded String message using the current private key loaded form the keystore
     */
    public byte[] decodeMessageWithServerPrivateKey(byte[] cipherText, Cipher asymmetricCipher) {
        return decodeMessageWithPrivateKey(this.getServerKeyPair().getPrivate(), cipherText, asymmetricCipher);
    }

    /**
     * @param cipherText Encrypted Text
     * @return Decoded String message using the current private key loaded form the keystore
     */
    public byte[] decodeMessageWithPrivateKey(PrivateKey key, byte[] cipherText) {
        Cipher cipherRSA = getCipherRSA(null);
        if (cipherRSA != null)
            return decodeMessageWithPrivateKey(key, cipherText, cipherRSA);
        return new byte[]{};
    }

    /**
     * @param cipherText Encrypted Text
     * @return Decoded String message using the current private key loaded form the keystore
     */
    public byte[] decodeMessageWithPrivateKey(PrivateKey key, byte[] cipherText, Cipher asymmetricCipher) {
        try {
            // initialize your cipher
            asymmetricCipher.init(Cipher.DECRYPT_MODE, key);
            // asuming, cipherText is a byte array containing your encrypted message
            return asymmetricCipher.doFinal(cipherText);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new byte[]{};
    }

    /**
     * Create a signature of the input string with the server private certificate.
     *
     * @param data         data to be signed
     * @param encodeBase64 true if you want the result be encoded in base64
     * @return
     */
    public byte[] signDataWithServerCert(byte[] data, boolean encodeBase64) {
        try {
            Signature sig = Signature.getInstance("SHA256WithRSA");
            sig.initSign(getServerKeyPair().getPrivate());
            sig.update(data);
            byte[] signatureBytes = sig.sign();
            if (encodeBase64) return Base64.getEncoder().encode(signatureBytes);
            return signatureBytes;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new byte[]{};
    }

    /**
     * Verifies signed data with server cert
     *
     * @param inputData
     * @param signedData
     * @param decodeSignedDataFromBase64
     * @return
     */
    public boolean verifyDataSignedWithServerCert(byte[] inputData, byte[] signedData, boolean decodeSignedDataFromBase64) {
        try {
            Signature sig = Signature.getInstance("SHA256WithRSA");
            sig.initVerify(getServerKeyPair().getPublic());
            sig.update(inputData);
            byte[] signatureBytes = (decodeSignedDataFromBase64) ? Base64.getDecoder().decode(signedData) : signedData;
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * @param plainTextMessage plain text message
     * @param cipherText       encrypted challenge text
     * @param publicKeyBytes   public key
     * @return true if plain text message and decrypted cipherText are equal
     */
    public boolean checkChallengeMessage(String plainTextMessage, String cipherText, byte[] publicKeyBytes) {
        String decodedCipherText = new String(decodeMessageWithPublicKey(Base64.getDecoder().decode(cipherText.getBytes()), publicKeyBytes));
        return decodedCipherText.equals(plainTextMessage);
    }

    /**
     * @param pk   Private Key
     * @param text String to encrypt
     * @return encrypted text
     */
    public byte[] encryptText(PrivateKey pk, byte[] text, boolean encodeInBase64, Cipher asymmetricCipher) {
        try {
            asymmetricCipher.init(Cipher.ENCRYPT_MODE, pk);
            if (encodeInBase64) {
                return Base64.getEncoder().encode(asymmetricCipher.doFinal(text));
            }
            return asymmetricCipher.doFinal(text);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new byte[]{};
    }

    /**
     * @param pk   Publick key
     * @param text String to encrypt
     * @return encrypted text
     */
    public byte[] encryptText(PublicKey pk, byte[] text, boolean encodeInBase64, Cipher asymmetricCipher) {
        try {
            asymmetricCipher.init(Cipher.ENCRYPT_MODE, pk);
            if (encodeInBase64) {
                return Base64.getEncoder().encode(asymmetricCipher.doFinal(text));
            }
            return asymmetricCipher.doFinal(text);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new byte[]{};
    }

    /**
     * @return Random 32 byte length password
     */
    public byte[] generateRandomAESPassword() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        int keyBitSize = 256;
        keyGen.init(keyBitSize, secureRandom);
        return keyGen.generateKey().getEncoded();
    }

    /**
     * @return Random byte init vector
     */
    public IvParameterSpec generateRandomAESInitVector() {
        Cipher c = getCipherAES();
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] iv = new byte[c.getBlockSize()];
        randomSecureRandom.nextBytes(iv);
        return new IvParameterSpec(iv);
    }

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
    public byte[] getAESKeyFromPassword(String password, String hashMethod, byte[] salt, int numIterations, int keyBitSize) throws InvalidKeySpecException, NoSuchAlgorithmException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(hashMethod);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, numIterations, keyBitSize);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        return secret.getEncoded();
    }


    /**
     * @param aesPassword secret aes key
     * @param content     Content to encrypt
     * @param aesCipher   AES Cipher
     * @return Encrypted text as a String encoded in base 64 returns ivBytes+encryptedBytes
     */
    public byte[] encryptWithAES(byte[] aesPassword, String content, Cipher aesCipher) {
        try {
            IvParameterSpec ivBytes = generateRandomAESInitVector();
            SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivBytes);
            byte[] encrypted = aesCipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            byte[] toEncode = new byte[ivBytes.getIV().length + encrypted.length];
            System.arraycopy(ivBytes.getIV(), 0, toEncode, 0, ivBytes.getIV().length);
            System.arraycopy(encrypted, 0, toEncode, ivBytes.getIV().length, encrypted.length);
            return Base64.getEncoder().encode(toEncode);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return new byte[]{};
    }

    /**
     * @param aesPassword secret aes key
     * @param salt        salt
     * @param content     Content to encrypt
     * @param aesCipher   AES Cipher
     * @return Encrypted text as a String encoded in base 64 returns saltBytes+ivBytes+encryptedBytes
     */
    public byte[] encryptWithAES(byte[] aesPassword, byte[] salt, String content, Cipher aesCipher) {
        try {
            IvParameterSpec iv = generateRandomAESInitVector();
            SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            byte[] encrypted = aesCipher.doFinal(content.getBytes(StandardCharsets.UTF_8));
            byte[] toEncode = new byte[iv.getIV().length + salt.length + encrypted.length];
            System.arraycopy(salt, 0, toEncode, 0, salt.length);
            System.arraycopy(iv.getIV(), 0, toEncode, salt.length, iv.getIV().length);
            System.arraycopy(encrypted, 0, toEncode, (iv.getIV().length + salt.length), encrypted.length);
            return Base64.getEncoder().encode(toEncode);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
        return new byte[]{};
    }

    /**
     * @param aesPassword secret aes key
     * @param content     Content to decrypt
     * @return Decrypted text as a String
     * @throws InvalidKeyException      Invalid key exception
     * @throws NoSuchPaddingException   No Such padding
     * @throws NoSuchAlgorithmException No Such Algotithm
     */
    public byte[] decryptWithAES(byte[] aesPassword, byte[] initVector, String content, Cipher aesCipher) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        SecretKeySpec skeySpec = new SecretKeySpec(aesPassword, "AES");
        IvParameterSpec iv = new IvParameterSpec(initVector);
        aesCipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
        return aesCipher.doFinal(Base64.getDecoder().decode(content));
    }

    /**
     * @param password generates password hash.
     *                 Default algorithm is PBKDF2.
     * @return
     */
    @Override
    public byte[] hashPassword(byte[] salt, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 128);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Generates random Salt
     *
     * @param dim dimension of salt
     * @return
     */
    public byte[] generateSalt(int dim) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[dim];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * Generates 16 bytes random Salt
     *
     * @return
     */
    public byte[] generate16BytesSalt() {
        return generateSalt(16);
    }

    /**
     * Generates a cryptographically secure random password using {@link SecureRandom}
     * over a wide alphabet (upper/lower case letters, digits and a few symbols).
     *
     * @param length exact number of characters of the generated password
     * @return the generated password
     */
    @Override
    public String generateRandomPassword(int length) {
        if (length <= 0)
            throw new IllegalArgumentException("Password length must be greater than 0");
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    /**
     * AES-256-GCM authenticated encryption.
     * Output: version(1 byte) || iv(12 bytes) || ciphertext+tag. The version byte is authenticated as a prefix of the AAD.
     *
     * @param key       32 bytes AES key
     * @param plaintext data to protect
     * @param aad       additional authenticated data (null = empty)
     * @return sealed bytes
     */
    @Override
    public byte[] sealAead(byte[] key, byte[] plaintext, byte[] aad) {
        validateAeadKey(key);
        if (plaintext == null)
            throw new IllegalArgumentException("Plaintext must not be null");
        byte[] iv = new byte[AEAD_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(AEAD_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(AEAD_TAG_BITS, iv));
            updateAeadAad(cipher, AEAD_VERSION_AES256_GCM, aad);
            byte[] cipherTextAndTag = cipher.doFinal(plaintext);
            byte[] sealed = new byte[AEAD_HEADER_LENGTH + cipherTextAndTag.length];
            sealed[0] = AEAD_VERSION_AES256_GCM;
            System.arraycopy(iv, 0, sealed, 1, AEAD_IV_LENGTH);
            System.arraycopy(cipherTextAndTag, 0, sealed, AEAD_HEADER_LENGTH, cipherTextAndTag.length);
            return sealed;
        } catch (GeneralSecurityException e) {
            throw new WaterRuntimeException("Unable to seal data", e);
        }
    }

    /**
     * Verifies and decrypts data produced by {@link #sealAead(byte[], byte[], byte[])}.
     * Every failure on the sealed data produces the same generic message (no oracle on the failure reason).
     *
     * @param key    32 bytes AES key
     * @param sealed sealed bytes
     * @param aad    additional authenticated data used when sealing (null = empty)
     * @return plaintext
     */
    @Override
    public byte[] openAead(byte[] key, byte[] sealed, byte[] aad) {
        validateAeadKey(key);
        if (sealed == null)
            throw new IllegalArgumentException("Sealed data must not be null");
        if (sealed.length < AEAD_MIN_SEALED_LENGTH || sealed[0] != AEAD_VERSION_AES256_GCM)
            throw new WaterRuntimeException(AEAD_OPEN_ERROR);
        try {
            Cipher cipher = Cipher.getInstance(AEAD_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(AEAD_TAG_BITS, sealed, 1, AEAD_IV_LENGTH));
            updateAeadAad(cipher, sealed[0], aad);
            return cipher.doFinal(sealed, AEAD_HEADER_LENGTH, sealed.length - AEAD_HEADER_LENGTH);
        } catch (AEADBadTagException e) {
            //wrong key, wrong aad or tampered data: no cause attached, nothing to leak
            throw new WaterRuntimeException(AEAD_OPEN_ERROR);
        } catch (GeneralSecurityException e) {
            throw new WaterRuntimeException(AEAD_OPEN_ERROR, e);
        }
    }

    /**
     * Wraps key material with RSA-OAEP (SHA-256, MGF1-SHA-256).
     *
     * @param publicKey   RSA public key
     * @param keyMaterial key bytes to wrap
     * @return wrapped key
     */
    @Override
    public byte[] wrapKeyWithRSAOAEP(PublicKey publicKey, byte[] keyMaterial) {
        if (publicKey == null)
            throw new IllegalArgumentException("Public key must not be null");
        if (keyMaterial == null || keyMaterial.length == 0)
            throw new IllegalArgumentException("Key material must not be empty");
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, rsaOaepSha256Parameters());
            return cipher.doFinal(keyMaterial);
        } catch (GeneralSecurityException e) {
            throw new WaterRuntimeException("Unable to wrap key", e);
        }
    }

    /**
     * Unwraps key material wrapped with {@link #wrapKeyWithRSAOAEP(PublicKey, byte[])}.
     *
     * @param privateKey RSA private key
     * @param wrappedKey wrapped key bytes
     * @return raw key material
     */
    @Override
    public byte[] unwrapKeyWithRSAOAEP(PrivateKey privateKey, byte[] wrappedKey) {
        if (privateKey == null)
            throw new IllegalArgumentException("Private key must not be null");
        if (wrappedKey == null || wrappedKey.length == 0)
            throw new IllegalArgumentException("Wrapped key must not be empty");
        try {
            Cipher cipher = Cipher.getInstance(RSA_OAEP_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, rsaOaepSha256Parameters());
            return cipher.doFinal(wrappedKey);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            //wrong private key or tampered data: no cause attached (no padding oracle details)
            throw new WaterRuntimeException(KEY_UNWRAP_ERROR);
        } catch (GeneralSecurityException e) {
            throw new WaterRuntimeException(KEY_UNWRAP_ERROR, e);
        }
    }

    /**
     * @param alias key alias inside the server keystore
     * @return key pair
     */
    @Override
    public KeyPair getKeyPairByAlias(String alias) {
        return getKeyPairByAlias(null, alias);
    }

    /**
     * @param keystoreName logical keystore name (null/blank = server keystore)
     * @param alias        key alias
     * @return key pair
     */
    @Override
    public KeyPair getKeyPairByAlias(String keystoreName, String alias) {
        validateKeyAlias(alias);
        KeystoreConfig config = resolveKeystoreConfig(keystoreName);
        KeyPair keyPair = loadKeyEntry(config, alias, (keystore, entryAlias, keyPwd) -> {
            Key key = keystore.getKey(entryAlias, keyPwd);
            Certificate cert = keystore.getCertificate(entryAlias);
            if (key instanceof PrivateKey privateKey && cert != null)
                return new KeyPair(cert.getPublicKey(), privateKey);
            return null;
        });
        if (keyPair == null)
            throw new WaterRuntimeException("No key pair found for alias '" + alias + "' in keystore '" + config.label() + "', or error while loading it");
        return keyPair;
    }

    /**
     * @param keystoreName logical keystore name (null/blank = server keystore)
     * @param alias        secret key entry alias
     * @return raw bytes of the secret key, owned by the caller
     */
    @Override
    public byte[] getSecretKeyByAlias(String keystoreName, String alias) {
        validateKeyAlias(alias);
        KeystoreConfig config = resolveKeystoreConfig(keystoreName);
        byte[] secret = loadKeyEntry(config, alias, (keystore, entryAlias, keyPwd) -> {
            Key key = keystore.getKey(entryAlias, keyPwd);
            if (key instanceof SecretKey secretKey) {
                //getEncoded may be null for non-extractable keys (e.g. hardware backed)
                byte[] encoded = secretKey.getEncoded();
                if (encoded != null && encoded.length > 0)
                    return encoded;
            }
            return null;
        });
        if (secret == null)
            throw new WaterRuntimeException("No secret key found for alias '" + alias + "' in keystore '" + config.label() + "', or error while loading it");
        return secret;
    }

    private void validateKeyAlias(String alias) {
        if (alias == null || alias.isBlank())
            throw new IllegalArgumentException("Key alias must not be blank");
    }

    /**
     * Resolves the configuration of the server keystore (null/blank name) or of a named keystore.
     * Properties are read at every call so a rotated keystore is picked up without restart.
     */
    private KeystoreConfig resolveKeystoreConfig(String keystoreName) {
        KeystoreConfig config;
        if (keystoreName == null || keystoreName.isBlank()) {
            config = new KeystoreConfig(SERVER_KEYSTORE_LABEL, getServerKeystoreFilePath(), getServerKeystorePassword(), getServerKeyPassword(), getServerKeystoreType());
        } else {
            if (!KEYSTORE_NAME_PATTERN.matcher(keystoreName).matches())
                throw new IllegalArgumentException("Invalid keystore name");
            String prefix = KEYSTORE_PROPERTY_PREFIX + keystoreName + ".";
            String keystorePassword = props.getPropertyOrDefault(prefix + "password", (String) null);
            config = new KeystoreConfig(keystoreName,
                    resolveKeystoreFilePath(props.getPropertyOrDefault(prefix + "file", (String) null)),
                    keystorePassword,
                    props.getPropertyOrDefault(prefix + "key.password", keystorePassword),
                    resolveKeystoreType(props.getPropertyOrDefault(prefix + "type", DEFAULT_NAMED_KEYSTORE_TYPE), DEFAULT_NAMED_KEYSTORE_TYPE));
        }
        if (config.filePath() == null || config.filePath().isEmpty() || config.keystorePassword() == null || config.keyPassword() == null)
            throw new WaterRuntimeException("Keystore '" + config.label() + "' is not configured");
        return config;
    }

    /**
     * Opens the keystore and lets the extractor read the requested entry.
     * Password buffers are always zeroed; paths and passwords are never logged.
     *
     * @return the extracted value, or null if the entry is missing / of the wrong kind / the keystore cannot be loaded
     */
    private <T> T loadKeyEntry(KeystoreConfig config, String alias, KeyEntryExtractor<T> extractor) {
        char[] keystorePwd = config.keystorePassword().toCharArray();
        char[] keyPwd = config.keyPassword().toCharArray();
        try (FileInputStream fis = new FileInputStream(config.filePath())) {
            KeyStore keystore = KeyStore.getInstance(config.type());
            keystore.load(fis, keystorePwd);
            return extractor.extract(keystore, alias, keyPwd);
        } catch (Exception e) {
            //never log paths or passwords, only keystore label and alias
            log.error("Error while loading key '{}' from keystore '{}': {}", alias, config.label(), e.getClass().getSimpleName());
        } finally {
            Arrays.fill(keystorePwd, '\0');
            Arrays.fill(keyPwd, '\0');
        }
        return null;
    }

    @FunctionalInterface
    private interface KeyEntryExtractor<T> {
        T extract(KeyStore keystore, String alias, char[] keyPassword) throws GeneralSecurityException;
    }

    /**
     * Resolved keystore coordinates. toString is overridden so that path and passwords can never end up in a log.
     */
    private record KeystoreConfig(String label, String filePath, String keystorePassword, String keyPassword, String type) {
        @Override
        public String toString() {
            return "KeystoreConfig[label=" + label + ", type=" + type + "]";
        }
    }

    private void validateAeadKey(byte[] key) {
        if (key == null || key.length != AEAD_KEY_LENGTH)
            throw new IllegalArgumentException("AEAD key must be " + AEAD_KEY_LENGTH + " bytes long (AES-256)");
    }

    private void updateAeadAad(Cipher cipher, byte version, byte[] aad) {
        cipher.updateAAD(new byte[]{version});
        if (aad != null && aad.length > 0)
            cipher.updateAAD(aad);
    }

    private OAEPParameterSpec rsaOaepSha256Parameters() {
        return new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    }
}


