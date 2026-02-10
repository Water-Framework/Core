package it.water.core.api.asset;

import it.water.core.api.model.AssetCategoryResource;
import it.water.core.api.service.Service;

/**
 * Interface for managing asset category associations.
 * Implemented by AssetCategory module, used by any entity that wants category support.
 */
public interface AssetCategoryManager extends Service {

    /**
     * Find an AssetCategoryResource by its parameters
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoryId the category id
     * @return the found AssetCategoryResource or null
     */
    AssetCategoryResource findAssetCategoryResource(String resourceName, long resourceId, long categoryId);

    /**
     * Add a category to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param categoryId the category id to associate
     */
    void addAssetCategory(String resourceName, long resourceId, long categoryId);

    /**
     *
     * @param resourceName
     * @param resourceId
     * @param categoriesId
     */
     void addAssetCategories(String resourceName, long resourceId, long[] categoriesId);

    /**
     *
     * @param resourceName
     * @param resourceId
     * @return
     */
     long[] findAssetCategories(String resourceName, long resourceId);

    /**
     *
     * @param resourceName
     * @param resourceId
     * @param categoryId
     */
     void removeAssetCategory(String resourceName, long resourceId, long categoryId);

    /**
     *
     * @param resourceName
     * @param resourceId
     * @param categoriesId
     */
     void removeAssetCategories(String resourceName, long resourceId, long[] categoriesId);
}