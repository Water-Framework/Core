package it.water.core.api.security;

import it.water.core.api.service.Service;

import java.util.Collection;

/**
 * @Author Aristide Cittadino
 * This interfaces maps the concept of Authentication Provider.
 * It's possibile to attach it to you service in order to expose authentication services in water applications.
 * <p>
 * User authentication is provided by User Module.
 */
public interface AuthenticationProvider extends Service {
    /**
     * Service to log in
     *
     * @param username
     * @param password
     * @return
     */
    Authenticable login(String username, String password);

    /**
     * Multi-tenant aware login overload. Additive and backward compatible: providers that do not
     * support tenancy inherit this default, which ignores companyId and falls back to the 2-arg login.
     * Tenant-aware providers override this to resolve/validate the active company for the session.
     *
     * @param username
     * @param password
     * @param companyId requested active company (may be null for single-tenant/legacy callers)
     * @return
     */
    default Authenticable login(String username, String password, Long companyId) {
        return login(username, password);
    }

    /**
     * Mints an Authenticable that carries the identity of a target user, on behalf of a caller
     * (user-level impersonation). Additive and backward compatible: providers that do not support
     * impersonation inherit this default, which fails fast. Tenant/impersonation-aware providers
     * (e.g. UserAuthenticationProvider) override it to permission-gate the caller, load the target
     * without a password, resolve the target's active company, and mark the session as impersonated.
     *
     * @param targetUsername username of the user to impersonate
     * @param callerUsername username of the authorized caller performing the impersonation
     * @param companyId      requested active company for the target (may be null → target's primary)
     * @return the target Authenticable with its active company resolved and impersonatedBy set to the caller
     */
    default Authenticable impersonate(String targetUsername, String callerUsername, Long companyId) {
        throw new UnsupportedOperationException("Impersonation not supported by this provider");
    }

    /**
     * Used to expose multiple authentication provider with different Issuers;
     *
     * @return
     */
    Collection<String> issuersNames();
}
