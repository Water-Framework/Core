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

    User createUserForCompany(String name, String lastname, String username, String email,
                              String password, long companyId, boolean primary, boolean active);

    List<Long> findCompanyIdsByUser(long userId);

    boolean hasMembership(long userId, long companyId);

    Long findPrimaryCompanyId(long userId);

    List<Long> findUserIdsByCompany(long companyId);

    List<Long> findPrimaryUserIdsByCompany(long companyId);

    /**
     * Removes every user-company membership bound to the given company, leaving the
     * associated users untouched. Idempotent: a company with no memberships is a no-op.
     * Trusted inter-service operation: callers must enforce permissions beforehand.
     *
     * @param companyId opaque company id whose memberships must be removed
     */
    void removeMembershipsByCompany(long companyId);
}
