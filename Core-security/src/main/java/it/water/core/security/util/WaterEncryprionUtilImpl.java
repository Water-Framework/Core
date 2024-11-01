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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;


/**
 * @Author Aristide Cittadino.
 * This class helps developers to interact with encryption/decryption operations.
 */
@FrameworkComponent(services = {EncryptionUtil.class})
public class WaterEncryprionUtilImpl implements EncryptionUtil {
    private final Logger log = LoggerFactory.getLogger(WaterEncryprionUtilImpl.class.getName());
    private static final long MILLIS_PER_DAY = 86400000L;
    private static final String SHA_WITH_RSA_ENC_ALGORITHM = "SHA256withRSA";
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
        return props.getProperty("water.keystore.file").toString();
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
     * @param keySize
     * @return
     */
    public KeyPair generateSSLKeyPairValue(int keySize) {
        try {
            SecureRandom randomGenerator = SecureRandom.getInstance("SHA1PRNG", "SUN");
            final KeyPairGenerator rsaKeyPairGenerator = KeyPairGenerator.getInstance("RSA");
            rsaKeyPairGenerator.initialize(keySize, randomGenerator);
            return rsaKeyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
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
            ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(privateKey);
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
            KeyStore keystore = KeyStore.getInstance("JKS");
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
            KeyStore keystore = KeyStore.getInstance("JKS");
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
     */
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
     */
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
}


