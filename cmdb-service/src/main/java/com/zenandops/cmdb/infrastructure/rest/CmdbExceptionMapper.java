package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.domain.exception.AssetInUseException;
import com.zenandops.cmdb.domain.exception.AssetNotFoundException;
import com.zenandops.cmdb.domain.exception.CIInUseException;
import com.zenandops.cmdb.domain.exception.CINotFoundException;
import com.zenandops.cmdb.domain.exception.DataSourceInUseException;
import com.zenandops.cmdb.domain.exception.DataSourceNotFoundException;
import com.zenandops.cmdb.domain.exception.DuplicateDataSourceNameException;
import com.zenandops.cmdb.domain.exception.DuplicateDependencyException;
import com.zenandops.cmdb.domain.exception.DuplicateRootOrganizationException;
import com.zenandops.cmdb.domain.exception.DuplicateServiceCIException;
import com.zenandops.cmdb.domain.exception.DuplicateSiblingNameException;
import com.zenandops.cmdb.domain.exception.ImmutableVersionException;
import com.zenandops.cmdb.domain.exception.InvalidFileFormatException;
import com.zenandops.cmdb.domain.exception.InvalidReliabilityRatingException;
import com.zenandops.cmdb.domain.exception.LastServiceAssociationException;
import com.zenandops.cmdb.domain.exception.OrganizationInUseException;
import com.zenandops.cmdb.domain.exception.OrganizationNotFoundException;
import com.zenandops.cmdb.domain.exception.SelfReferenceException;
import com.zenandops.cmdb.domain.exception.ServiceInUseException;
import com.zenandops.cmdb.domain.exception.ServiceNotFoundException;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;

/**
 * JAX-RS exception mapper that converts domain exceptions to appropriate HTTP responses.
 * Maps all CMDB domain exceptions to the correct HTTP status codes and error codes.
 */
@Provider
public class CmdbExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        // Not Found exceptions -> 404
        if (exception instanceof OrganizationNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "CMDB_ORGANIZATION_NOT_FOUND", exception.getMessage());
        }
        if (exception instanceof ServiceNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "CMDB_SERVICE_NOT_FOUND", exception.getMessage());
        }
        if (exception instanceof AssetNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "CMDB_ASSET_NOT_FOUND", exception.getMessage());
        }
        if (exception instanceof CINotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "CMDB_CI_NOT_FOUND", exception.getMessage());
        }
        if (exception instanceof DataSourceNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "CMDB_DATASOURCE_NOT_FOUND", exception.getMessage());
        }
        if (exception instanceof NotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND,
                    "CMDB_NOT_FOUND", exception.getMessage());
        }

        // In-Use exceptions -> 409
        if (exception instanceof OrganizationInUseException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_ORGANIZATION_IN_USE", exception.getMessage());
        }
        if (exception instanceof ServiceInUseException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_SERVICE_IN_USE", exception.getMessage());
        }
        if (exception instanceof AssetInUseException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_ASSET_IN_USE", exception.getMessage());
        }
        if (exception instanceof CIInUseException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_CI_IN_USE", exception.getMessage());
        }
        if (exception instanceof DataSourceInUseException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_DATASOURCE_IN_USE", exception.getMessage());
        }

        // Duplicate exceptions -> 409
        if (exception instanceof DuplicateRootOrganizationException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_DUPLICATE_ROOT", exception.getMessage());
        }
        if (exception instanceof DuplicateSiblingNameException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_DUPLICATE_SIBLING_NAME", exception.getMessage());
        }
        if (exception instanceof DuplicateDependencyException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_DUPLICATE_DEPENDENCY", exception.getMessage());
        }
        if (exception instanceof DuplicateDataSourceNameException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_DUPLICATE_DATASOURCE_NAME", exception.getMessage());
        }
        if (exception instanceof DuplicateServiceCIException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_DUPLICATE_SERVICE_CI", exception.getMessage());
        }
        if (exception instanceof LastServiceAssociationException) {
            return buildResponse(Response.Status.CONFLICT,
                    "CMDB_LAST_SERVICE_ASSOCIATION", exception.getMessage());
        }

        // Bad Request exceptions -> 400
        if (exception instanceof SelfReferenceException) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "CMDB_SELF_REFERENCE", exception.getMessage());
        }
        if (exception instanceof ImmutableVersionException) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "CMDB_IMMUTABLE_VERSION", exception.getMessage());
        }
        if (exception instanceof InvalidReliabilityRatingException) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "CMDB_INVALID_RELIABILITY_RATING", exception.getMessage());
        }
        if (exception instanceof InvalidFileFormatException) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "CMDB_INVALID_FILE_FORMAT", exception.getMessage());
        }
        if (exception instanceof IllegalArgumentException) {
            return buildResponse(Response.Status.BAD_REQUEST,
                    "CMDB_VALIDATION_ERROR", exception.getMessage());
        }

        // Let other exceptions propagate
        throw exception;
    }

    private Response buildResponse(Response.Status status, String code, String message) {
        ErrorResponse error = new ErrorResponse(code, message, Instant.now());
        return Response.status(status)
                .entity(Map.of("error", error))
                .build();
    }
}
