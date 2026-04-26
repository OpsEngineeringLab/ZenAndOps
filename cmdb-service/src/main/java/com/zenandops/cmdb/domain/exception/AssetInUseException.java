package com.zenandops.cmdb.domain.exception;

/**
 * Thrown when an Asset deletion is attempted while it has CIs or active versions.
 */
public class AssetInUseException extends RuntimeException {

    public AssetInUseException() {
        super("Asset is in use");
    }

    public AssetInUseException(String message) {
        super(message);
    }
}
