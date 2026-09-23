package it.water.core.api.service.integration;

import it.water.core.api.model.WaterCompany;

/**
 * Location-transparent access to Company data needed by other Water capabilities.
 */
public interface CompanyIntegrationClient extends EntityIntegrationClient {

    /** Reads a Company through the permission-aware inter-service contract. */
    WaterCompany findCompany(long companyId);

    /** Creates a Company through the permission-aware inter-service contract. */
    WaterCompany createCompany(String businessName, String invoiceAddress, String city, String postalCode,
                               String nation, String vatNumber, String virtualHost);

    Long findCompanyIdByVirtualHost(String virtualHost);

    /**
     * Checks whether the Company identified by {@code companyId} exists.
     *
     * <p>This is a trusted inter-service lookup. Callers remain responsible for
     * enforcing their own authorization before using the result.</p>
     */
    boolean existsCompany(long companyId);

    /**
     * Removes a Company through the trusted inter-service boundary.
     *
     * <p>This operation does not enforce permissions. Callers must enforce
     * authorization and complete dependent cleanup before invoking it.</p>
     */
    void removeCompany(long companyId);
}
