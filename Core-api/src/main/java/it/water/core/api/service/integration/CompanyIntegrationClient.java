package it.water.core.api.service.integration;

/**
 * Location-transparent access to Company data needed by other Water capabilities.
 */
public interface CompanyIntegrationClient extends EntityIntegrationClient {

    Long findCompanyIdByVirtualHost(String virtualHost);
}
