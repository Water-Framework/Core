package it.water.core.api.service.integration;

import it.water.core.api.model.User;

import java.util.List;

/**
 * Location-transparent contract for user provisioning within a company and
 * user-company membership lookup.
 *
 * This is a trusted inter-service boundary. Caller-facing APIs must enforce
 * permissions before invoking mutating operations.
 */
public interface UserCompanyIntegrationClient extends EntityIntegrationClient {

    User provisionUserForCompany(String name, String lastname, String username, String email,
                                 String password, long companyId, boolean primary, boolean active);

    List<Long> findCompanyIdsByUser(long userId);

    boolean hasMembership(long userId, long companyId);

    Long findPrimaryCompanyId(long userId);

    List<Long> findUserIdsByCompany(long companyId);

    List<Long> findPrimaryUserIdsByCompany(long companyId);
}
