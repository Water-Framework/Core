package it.water.core.api.model;

/**
 * Transport-safe read model for a Water company.
 *
 * <p>Persistence implementations may add fields and behavior, but consumers of inter-module
 * contracts must depend only on this stable representation.</p>
 */
public interface WaterCompany {

    long getId();

    String getBusinessName();

    String getInvoiceAddress();

    String getCity();

    String getPostalCode();

    String getNation();

    String getVatNumber();

    String getVirtualHost();
}
