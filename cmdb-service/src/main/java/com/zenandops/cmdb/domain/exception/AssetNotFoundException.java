package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an Asset with the given identifier does not exist.
 */
public class AssetNotFoundException extends RuntimeException {

    public AssetNotFoundException() {
        super("Asset not found");
    }

    public AssetNotFoundException(String message) {
        super(message);
    }
}
