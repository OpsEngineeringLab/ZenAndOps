package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.ImportFileUseCase;
import com.zenandops.cmdb.application.usecase.ListFileImportsUseCase;
import com.zenandops.cmdb.domain.entity.FileImportRecord;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.FileImportResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.Map;

/**
 * REST resource exposing file import endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/imports")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "File Imports",
        description = "File import operations for the CMDB")
public class FileImportResource {

    @Inject
    ImportFileUseCase importFileUseCase;

    @Inject
    ListFileImportsUseCase listFileImportsUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Upload and process a file",
            description = "Imports asset and CI data from a structured file (CSV, JSON, XML)")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "File processed successfully",
                    content = @Content(schema = @Schema(implementation = FileImportResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid file format",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SuppressWarnings("unchecked")
    public Response importFile(Map<String, Object> request) {
        String fileName = (String) request.get("fileName");
        String fileFormat = (String) request.get("fileFormat");
        List<Map<String, Object>> records = (List<Map<String, Object>>) request.get("records");

        if (fileName == null || fileFormat == null || records == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("CMDB_VALIDATION_ERROR",
                            "Missing required fields: fileName, fileFormat, records",
                            java.time.Instant.now()))
                    .build();
        }

        String userId = securityContext.getUserPrincipal().getName();
        FileImportRecord result = importFileUseCase.execute(fileName, fileFormat, records, userId);
        return Response.ok(toResponse(result)).build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List import history",
            description = "Retrieves the history of file import operations")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Import history retrieved successfully",
                    content = @Content(schema = @Schema(implementation = FileImportResponse[].class)))
    })
    public Response listImports() {
        List<FileImportResponse> items = listFileImportsUseCase.execute().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    private FileImportResponse toResponse(FileImportRecord record) {
        List<FileImportResponse.ImportErrorResponse> errorResponses = record.getErrors().stream()
                .map(e -> new FileImportResponse.ImportErrorResponse(
                        e.getRecordIndex(), e.getField(), e.getMessage()))
                .toList();

        return new FileImportResponse(
                record.getId(),
                record.getFileName(),
                record.getFileFormat(),
                record.getDataSourceId(),
                record.getStatus(),
                record.getTotalRecords(),
                record.getSuccessCount(),
                record.getFailureCount(),
                errorResponses,
                record.getImportedBy(),
                record.getCreatedAt()
        );
    }
}
