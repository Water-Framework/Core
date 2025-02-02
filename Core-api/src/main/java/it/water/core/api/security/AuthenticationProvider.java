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
     * Used to expose multiple authentication provider with different Issuers;
     *
     * @return
     */
    Collection<String> issuersNames();
}
