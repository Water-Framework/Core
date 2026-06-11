package it.water.core.api.security;

import it.water.core.api.service.Service;

/**
 * @Author Aristide Cittadino
 * Hashes and verifies user passwords in PHC string format
 * ({@code $id$param=value,...$base64salt$base64hash}). The salt is embedded in the PHC string,
 * so callers must NOT pre-salt the clear text. Legacy salted digests are still verifiable via
 * {@link #matches(char[], String, String)}; {@link #needsRehash(String)} signals when a hash
 * should be upgraded on the next successful login.
 */
public interface PasswordHashService extends Service {

    /**
     * Hashes the clear-text password with a freshly generated embedded salt.
     *
     * @return a self-describing PHC string, safe to persist
     */
    String hash(char[] clearTextPassword);

    /**
     * Verifies a clear-text password against a stored PHC hash (constant-time).
     */
    boolean matches(char[] clearTextPassword, String storedPhcHash);

    /**
     * Backward-compatible verification: {@code legacySalt} (Base64) is used only when
     * {@code storedHash} is a legacy digest, ignored for PHC hashes.
     */
    boolean matches(char[] clearTextPassword, String storedHash, String legacySalt);

    /**
     * @return true if the stored hash is legacy or uses stale cost parameters and should be
     * re-hashed on the next successful authentication
     */
    boolean needsRehash(String storedHash);
}
