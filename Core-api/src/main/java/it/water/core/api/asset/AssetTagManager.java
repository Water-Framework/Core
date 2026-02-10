package it.water.core.api.asset;

import it.water.core.api.model.AssetTagResource;
import it.water.core.api.service.Service;

/**
 * Interface for managing asset tag associations.
 * Implemented by AssetTag module, used by any entity that wants tag support.
 */
public interface AssetTagManager extends Service {

    /**
     * Find an AssetTagResource by its parameters
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagId the tag id
     * @return the found AssetTagResource or null
     */
    AssetTagResource findAssetTagResource(String resourceName, long resourceId, long tagId);

    /**
     * Add a tag to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagId the tag id to associate
     */
    void addAssetTag(String resourceName, long resourceId, long tagId);

    /**
     * Add multiple tags to a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagsId array of tag ids to associate
     */
    void addAssetTags(String resourceName, long resourceId, long[] tagsId);

    /**
     * Find all tags associated with a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @return array of tag ids
     */
    long[] findAssetTags(String resourceName, long resourceId);

    /**
     * Remove a tag from a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagId the tag id to remove
     */
    void removeAssetTag(String resourceName, long resourceId, long tagId);

    /**
     * Remove multiple tags from a resource
     * @param resourceName the resource class name
     * @param resourceId the resource primary key
     * @param tagsId array of tag ids to remove
     */
    void removeAssetTags(String resourceName, long resourceId, long[] tagsId);
}