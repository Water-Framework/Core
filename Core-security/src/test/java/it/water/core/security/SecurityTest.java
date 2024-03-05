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

import it.water.core.api.action.Action;
import it.water.core.api.action.ActionsManager;
import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionUtil;
import it.water.core.api.permission.Role;
import it.water.core.api.permission.RoleManager;
import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.security.EncryptionUtil;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.permission.action.CrudActions;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.security.model.context.BasicSecurityContext;
import it.water.core.security.model.principal.RolePrincipal;
import it.water.core.security.model.principal.UserPrincipal;
import it.water.core.security.service.TestEntityService;
import it.water.core.security.service.TestEntityService1;
import it.water.core.security.service.TestProtectedEntity;
import it.water.core.security.service.TestResourceService;
import it.water.core.testing.utils.api.TestPermissionManager;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import it.water.core.testing.utils.junit.WaterTestExtension;
import lombok.Setter;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500PrivateCredential;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityTest implements Service {
    private static TestRuntimeInitializer initializer = TestRuntimeInitializer.getInstance();
    @Inject
    @Setter
    private ActionsManager actionsManager;
    @Inject
    @Setter
    private ComponentRegistry componentRegistry;
    @Inject
    @Setter
    private ApplicationProperties applicationProperties;
    @Inject
    @Setter
    private BeforeMethodInterceptor beforeMethodInterceptor;

    @Inject
    @Setter
    //injecting test permission manager in order to perform some basic security tests
    private TestPermissionManager testPermissionManager;
    @Inject
    @Setter
    private RoleManager roleManager;

    @Inject
    @Setter
    private Runtime runtime;

    //User who has right permission
    private User userOk;
    //User who does not have permissions
    private User userKo;
    //Default role for protected resource
    private Role testRole;

    @BeforeAll
    void beforeAll() {
        this.userOk = testPermissionManager.addUser("usernameOk", "username", "username", "email@mail.com", true);
        this.userKo = testPermissionManager.addUser("usernameKo", "usernameKo", "usernameKo", "email1@mail.com", false);
        this.testRole = roleManager.getRole(TestProtectedResource.TEST_ROLE_NAME);
        Action saveAction = this.actionsManager.getActions().get(TestProtectedResource.class.getName()).getAction(CrudActions.SAVE);
        roleManager.addRole(this.userOk.getId(), testRole);
        testPermissionManager.addPermissionIfNotExists(testRole, TestProtectedResource.class, saveAction);
    }

    /**
     * Checking wether all framework components have been initialized correctly
     */
    @Test
    void checkComponents() {
        Assertions.assertNotNull(componentRegistry);
        Assertions.assertNotNull(applicationProperties);
        Assertions.assertNotNull(beforeMethodInterceptor);
        Assertions.assertNotNull(actionsManager);
        Assertions.assertNotNull(testPermissionManager);
    }

    /**
     * Test Permission Util with:
     * - usernameOk : should pass every test
     * - other : should fail
     * Every test is done with Protected entity, Protected Resource and not protected entity.
     * Plus each test is repeated setting the permission manager to null. In this case
     * test on protected resource should deny access when permission manager is null, not protected resource should
     * pass the security check.
     */
    @Test
    void testPermissionUtil() {
        TestProtectedEntity res = new TestProtectedEntity();
        PermissionUtil permissionUtil = getPermissionUtil();
        Assertions.assertTrue(this.actionsManager.getActions().containsKey(TestProtectedResource.class.getName()));
        Action saveAction = this.actionsManager.getActions().get(TestProtectedResource.class.getName()).getAction(CrudActions.SAVE);
        //pass
        initializer.impersonate(this.userOk, runtime);
        Assertions.assertTrue(permissionUtil.checkPermission(res, saveAction));
        Assertions.assertTrue(permissionUtil.checkPermission(res.getResourceName(), saveAction));
        Assertions.assertTrue(permissionUtil.checkPermissionAndOwnership(res, saveAction));
        Assertions.assertTrue(permissionUtil.checkPermissionAndOwnership(res.getResourceName(), saveAction));
        Assertions.assertTrue(permissionUtil.userHasRoles("usernameOk", new String[]{TestProtectedResource.TEST_ROLE_NAME}));
        //deny
        initializer.impersonate(this.userKo, runtime);
        Assertions.assertFalse(permissionUtil.checkPermission(res, saveAction));
        Assertions.assertFalse(permissionUtil.checkPermission(res.getResourceName(), saveAction));
        Assertions.assertFalse(permissionUtil.checkPermissionAndOwnership(res, saveAction));
        Assertions.assertFalse(permissionUtil.checkPermissionAndOwnership(res.getResourceName(), saveAction));
        Assertions.assertFalse(permissionUtil.userHasRoles("usernameKo", new String[]{TestProtectedResource.TEST_ROLE_NAME}));
        initializer.impersonate(this.userKo, runtime);
    }

    /**
     * Testing all permission annotations.
     * Checking positive case and negative case.
     */
    @Test
    void testPermissionAnnotationImplementations() {
        //testing entities
        initializer.impersonate(this.userOk, runtime);
        TestEntityService testService = initializer.getComponentRegistry().findComponent(TestEntityService.class, null);
        TestEntityService1 alternativeService = initializer.getComponentRegistry().findComponent(TestEntityService1.class, null);
        Assertions.assertNotNull(testService);
        Assertions.assertTrue(testService.genericPermissionMethod());
        Assertions.assertTrue(testService.genericPermissionMethodWithoutResourceName());
        Assertions.assertTrue(testService.allowRolesMethod());
        Assertions.assertTrue(testService.specificPermissionMethod(1));
        Assertions.assertTrue(testService.specificPermissionMethodWithoutIdIndex(new TestProtectedEntity()));
        Assertions.assertTrue(testService.specificPermissionMethodWithSystemApi(1));
        Assertions.assertTrue(alternativeService.alternativeSpecificPermissionMethod(1));
        Assertions.assertNotNull(testService.permissionOnReturnMethod());
        initializer.impersonate(this.userKo, runtime);
        TestProtectedEntity entity = new TestProtectedEntity();
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.genericPermissionMethod());
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.genericPermissionMethodWithoutResourceName());
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.allowRolesMethod());
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.specificPermissionMethod(1));
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.specificPermissionMethodWithoutIdIndex(entity));
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.specificPermissionMethodWithSystemApi(1));
        Assertions.assertThrows(UnauthorizedException.class, () -> testService.permissionOnReturnMethod());
        //testing protected resource that are not entities
        initializer.impersonate(this.userOk, runtime);
        TestResourceService testResourceService = initializer.getComponentRegistry().findComponent(TestResourceService.class, null);
        Assertions.assertNotNull(testResourceService);
        Assertions.assertTrue(testResourceService.genericPermissionMethod());
        Assertions.assertTrue(testResourceService.genericPermissionMethodWithResourceParamName("name"));
    }

    @Test
    void testBasicSecurityContext() {
        Set<Principal> principals = new HashSet<>();
        principals.add(new UserPrincipal("prova", true, 1, "user"));
        BasicSecurityContext basicSecurityContext = new BasicSecurityContext(principals);
        BasicSecurityContext basicSecurityContextWithPermissionImplementantion = new BasicSecurityContext(principals, null);
        Assertions.assertEquals("prova", basicSecurityContext.getLoggedUsername());
        Assertions.assertEquals("Basic", basicSecurityContext.getAuthenticationScheme());
        Assertions.assertEquals(false, basicSecurityContext.isSecure());
        Assertions.assertNotNull(basicSecurityContextWithPermissionImplementantion);
        Assertions.assertNull(basicSecurityContextWithPermissionImplementantion.getPermissionImplementation());
        //setting manually just to test
        basicSecurityContext.setComponentRegistry(componentRegistry);
        Assertions.assertNotNull(basicSecurityContext.getPermissionManager());
    }

    /**
     * Checking security context model.
     */
    @Test
    void testSecurityContext() {
        initializer.impersonate(this.userOk, runtime);
        SecurityContext context = componentRegistry.findComponent(Runtime.class, null).getSecurityContext();
        Assertions.assertNotNull(testPermissionManager);
        Assertions.assertEquals("usernameOk", context.getLoggedUsername());

        //Testing principals and Security Context general behaviour
        Principal user1Princpal = new UserPrincipal("user1", false, 1, "test");
        Principal userAdminPrincipal = new UserPrincipal("admin", true, 2, "test");
        RolePrincipal role1Principal = new RolePrincipal("role1");

        //Simulating security context for a normal user
        Set<Principal> principals = new HashSet<>();
        principals.add(user1Princpal);
        principals.add(role1Principal);
        ExampleSecurityContext exampleSecurityContext = new ExampleSecurityContext(testPermissionManager, principals);
        Assertions.assertNotNull(exampleSecurityContext.getLoggedPrincipals());
        Assertions.assertFalse(exampleSecurityContext.isSecure());
        Assertions.assertEquals("none", exampleSecurityContext.getAuthenticationScheme());
        Assertions.assertEquals("user1", exampleSecurityContext.getLoggedUsername());
        Assertions.assertEquals("test", exampleSecurityContext.getIssuerClassName());
        Assertions.assertEquals(1, exampleSecurityContext.getLoggedEntityId());
        Assertions.assertEquals("user1", exampleSecurityContext.getUserPrincipal().getName());
        Assertions.assertTrue(exampleSecurityContext.isLoggedIn());
        Assertions.assertFalse(exampleSecurityContext.isAdmin());
        Assertions.assertTrue(exampleSecurityContext.isUserInRole("role1"));
        Assertions.assertFalse(exampleSecurityContext.isUserInRole("role2"));

        //Simulating security context for an administrator
        principals = new HashSet<>();
        principals.add(userAdminPrincipal);
        exampleSecurityContext = new ExampleSecurityContext(testPermissionManager, principals);
        Assertions.assertEquals("admin", exampleSecurityContext.getLoggedUsername());
        Assertions.assertEquals(2, exampleSecurityContext.getLoggedEntityId());
        Assertions.assertEquals("admin", exampleSecurityContext.getUserPrincipal().getName());
        Assertions.assertTrue(exampleSecurityContext.isLoggedIn());
        Assertions.assertTrue(exampleSecurityContext.isAdmin());
        Assertions.assertFalse(exampleSecurityContext.isUserInRole("role1"));
        Assertions.assertFalse(exampleSecurityContext.isUserInRole("role2"));

        //Testing empty context
        exampleSecurityContext = new ExampleSecurityContext(testPermissionManager, null);
        Assertions.assertFalse(exampleSecurityContext.isLoggedIn());
        Assertions.assertNotNull(exampleSecurityContext.toString());
        exampleSecurityContext = new ExampleSecurityContext(null, "permissionImplementation", testPermissionManager);
        Assertions.assertFalse(exampleSecurityContext.isLoggedIn());
        Assertions.assertNotNull(exampleSecurityContext.toString());
        Assertions.assertEquals("permissionImplementation", exampleSecurityContext.getPermissionImplementation());
    }

    /**
     * Testing encryption utils for all RSA algorithms
     *
     * @throws OperatorCreationException
     */
    @Test
    void testEncryptionUtilsRSA() throws OperatorCreationException {
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        KeyPair keyPair = waterEncryptionUtil.generateSSLKeyPairValue(1024);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        String privateKeyString = waterEncryptionUtil.getPrivateKeyString(privateKey);
        String publicKeyString = waterEncryptionUtil.getPublicKeyString(publicKey);
        Assertions.assertNotNull(privateKeyString);
        Assertions.assertNotNull(publicKey);
        Assertions.assertNotNull(privateKey);
        String cipherText = "cipher Text!";
        Assertions.assertNotNull(waterEncryptionUtil.getEncryptionAlgorithm());
        byte[] encodedBase64 = waterEncryptionUtil.encodeMessageWithPrivateKey(cipherText.getBytes(), privateKeyString.getBytes(StandardCharsets.UTF_8));
        byte[] encprypted = Base64.getDecoder().decode(encodedBase64);
        byte[] decrypted = waterEncryptionUtil.decodeMessageWithPublicKey(encprypted, publicKey.getEncoded());
        Assertions.assertEquals(cipherText, new String(decrypted));

        encodedBase64 = waterEncryptionUtil.encodeMessageWithPublicKey(cipherText.getBytes(), publicKeyString.getBytes(StandardCharsets.UTF_8));
        encprypted = Base64.getDecoder().decode(encodedBase64);
        decrypted = waterEncryptionUtil.decodeMessageWithPrivateKey(privateKey, encprypted);
        Assertions.assertEquals(cipherText, new String(decrypted));
    }

    /**
     * Checking property loading
     */
    @Test
    void testProperties() {
        ApplicationProperties waterApplicationProperties = initializer.getComponentRegistry().findComponent(ApplicationProperties.class, null);
        Assertions.assertNotNull(waterApplicationProperties);
        Assertions.assertEquals("server-cert", waterApplicationProperties.getProperty("water.keystore.alias"));
    }

    /**
     * Testing encpryption made with server certificates
     *
     * @throws PEMException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    @Test
    void testServerCerts() throws PEMException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        ApplicationProperties waterApplicationProperties = initializer.getComponentRegistry().findComponent(ApplicationProperties.class, null);
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        Assertions.assertNotNull(waterApplicationProperties);
        Assertions.assertNotNull(waterEncryptionUtil.getServerRootCert());
        Assertions.assertNotNull(waterEncryptionUtil.getServerKeyPair());
        Assertions.assertEquals("server-cert", waterEncryptionUtil.getServerKeystoreAlias());
        KeyPair keyPair = waterEncryptionUtil.getServerKeyPair();
        String publicKeyString = waterEncryptionUtil.getPublicKeyString(keyPair.getPublic());
        String cipherText = "server signed cipher text!";
        byte[] encodedBase64 = waterEncryptionUtil.encodeMessageWithPublicKey(cipherText.getBytes(StandardCharsets.UTF_8), publicKeyString.getBytes(StandardCharsets.UTF_8));
        byte[] encprypted = Base64.getDecoder().decode(encodedBase64);
        byte[] decrypted = waterEncryptionUtil.decodeMessageWithServerPrivateKey(encprypted, waterEncryptionUtil.getCipherRSA(null));
        Assertions.assertEquals(cipherText, new String(decrypted));
    }

    /**
     * Testing generation of server signed certificates
     *
     * @throws PEMException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    @Test
    void testCertsGenerations() throws PEMException, NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        KeyPair keyPair = waterEncryptionUtil.generateSSLKeyPairValue(1024);
        X500PrivateCredential certificate = waterEncryptionUtil.createServerClientX509Cert("subject", 100, keyPair, waterEncryptionUtil.getServerRootCert());
        Assertions.assertNotNull(certificate);
        Assertions.assertNotNull(certificate.getCertificate());
        Assertions.assertNotNull(certificate.getPrivateKey());
        String cipherText = "cipher text";
        String publicKeyStr = waterEncryptionUtil.getPublicKeyString(certificate.getCertificate().getPublicKey());
        byte[] encodedBase64 = waterEncryptionUtil.encodeMessageWithPublicKey(cipherText.getBytes(StandardCharsets.UTF_8), publicKeyStr.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = Base64.getDecoder().decode(encodedBase64);
        byte[] decrypted = waterEncryptionUtil.decodeMessageWithPrivateKey(certificate.getPrivateKey(), encrypted, waterEncryptionUtil.getCipherRSA(null));
        Assertions.assertEquals(cipherText, new String(decrypted));
    }

    /**
     * Testing other encryption algorithms
     *
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     */
    @Test
    void testOtherAlgorithms() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException {
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        Assertions.assertNotNull(waterEncryptionUtil.getCipherRSAOAEPPAdding());
        Assertions.assertNotNull(waterEncryptionUtil.getCipherRSAECB(null));
        Assertions.assertNotNull(waterEncryptionUtil.getCipherRSAECB("PKCS1Padding"));
        Assertions.assertNotNull(waterEncryptionUtil.getCipherRSAPKCS1Padding(false));
    }

    /**
     * Testing signing data with server certificates
     */
    @Test
    void testSignDataWithServerCerts() {
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        String dataToSign = "data to sign by the server";
        String dataNotSigned = "data not signed";
        byte[] signedData = waterEncryptionUtil.signDataWithServerCert(dataToSign.getBytes(StandardCharsets.UTF_8), false);
        Assertions.assertTrue(waterEncryptionUtil.verifyDataSignedWithServerCert(dataToSign.getBytes(StandardCharsets.UTF_8), signedData, false));
        Assertions.assertFalse(waterEncryptionUtil.verifyDataSignedWithServerCert(dataNotSigned.getBytes(StandardCharsets.UTF_8), dataNotSigned.getBytes(StandardCharsets.UTF_8), false));
    }

    @Test
    void testTextChallenge() {
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        KeyPair keyPair = waterEncryptionUtil.getServerKeyPair();
        String privateKeyString = waterEncryptionUtil.getPrivateKeyString(keyPair.getPrivate());
        String plainChallenge = "challenge!";
        byte[] encryptedTextChallenge = waterEncryptionUtil.encodeMessageWithPrivateKey(plainChallenge.getBytes(), privateKeyString.getBytes(StandardCharsets.UTF_8));
        Assertions.assertTrue(waterEncryptionUtil.checkChallengeMessage(plainChallenge, new String(encryptedTextChallenge), keyPair.getPublic().getEncoded()));
    }

    /**
     * Test AES encpryption with iv , iv + salt modes
     *
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    @Test
    void testAESEncprytion() throws NoSuchPaddingException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        EncryptionUtil waterEncryptionUtil = initializer.getComponentRegistry().findComponent(EncryptionUtil.class, null);
        Assertions.assertNotNull(waterEncryptionUtil.getCipherAES());
        byte[] password = waterEncryptionUtil.generateRandomAESPassword();
        String cipherText = "cipher text!";
        Assertions.assertNotNull(waterEncryptionUtil.generateRandomAESInitVector());
        byte[] initVector = new byte[16];
        byte[] encrypted = waterEncryptionUtil.encryptWithAES(password, cipherText, waterEncryptionUtil.getCipherAES());
        System.arraycopy(Base64.getDecoder().decode(encrypted), 0, initVector, 0, initVector.length);
        byte[] decrypted = waterEncryptionUtil.decryptWithAES(password, initVector, new String(encrypted), waterEncryptionUtil.getCipherAES());
        byte[] finalString = new byte[(decrypted.length - initVector.length)];
        System.arraycopy(decrypted, initVector.length, finalString, 0, finalString.length);
        Assertions.assertEquals(cipherText, new String(finalString));

        byte[] salt = waterEncryptionUtil.generateRandomAESSalt(16);
        byte[] encryptedAndSal = waterEncryptionUtil.encryptWithAES(password, salt, cipherText, waterEncryptionUtil.getCipherAES());
        byte[] encrpted = new byte[encryptedAndSal.length - salt.length];
        System.arraycopy(encryptedAndSal, salt.length, encrpted, 0, encrpted.length);
        decrypted = waterEncryptionUtil.decryptWithAES(password, initVector, new String(encrypted), waterEncryptionUtil.getCipherAES());
        finalString = new byte[(decrypted.length - initVector.length)];
        System.arraycopy(decrypted, initVector.length, finalString, 0, finalString.length);
        Assertions.assertEquals(cipherText, new String(finalString));
        Assertions.assertNotNull(waterEncryptionUtil.getAESKeyFromPassword("password", "PBKDF2WithHmacSHA1", salt, 10000, 128));
    }

    private PermissionUtil getPermissionUtil() {
        return initializer.getComponentRegistry().findComponents(PermissionUtil.class, null).get(0);
    }
}
