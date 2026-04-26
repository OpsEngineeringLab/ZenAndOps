package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateServiceCIUseCase;
import com.zenandops.cmdb.application.usecase.DeleteServiceCIUseCase;
import com.zenandops.cmdb.application.usecase.ListCIsByServiceUseCase;
import com.zenandops.cmdb.application.usecase.ListServicesByCIUseCase;
import com.zenandops.cmdb.domain.entity.ServiceCI;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateServiceCIRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.ServiceCIResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;

/**
 * REST resource exposing ServiceCI association endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/service-cis")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Service-CI Associations",
        description = "Service-CI association management for the CMDB")
public class ServiceCIResource {

    @Inject
    CreateServiceCIUseCase createServiceCIUseCase;

    @Inject
    DeleteServiceCIUseCase deleteServiceCIUseCase;

    @Inject
    ListCIsByServiceUseCase listCIsByServiceUseCase;

    @Inject
    ListServicesByCIUseCase listServicesByCIUseCase;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a service-CI association",
            description = "Associates a CI with a service")
    @RequestBody(description = "Service-CI association creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateServiceCIRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Association created successfully",
                    content = @Content(schema = @Schema(implementation = ServiceCIResponse.class))),
            @APIResponse(responseCode = "404", description = "Service or CI not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate association",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createAssociation(CreateServiceCIRequest request) {
        ServiceCI serviceCI = createServiceCIUseCase.execute(
                request.serviceId(), request.ciId());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(serviceCI))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List service-CI associations",
            description = "Returns associations filtered by serviceId or ciId query parameter")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Associations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServiceCIResponse[].class)))
    })
    public Response listAssociations(
            @Parameter(description = "Filter by service ID")
            @QueryParam("serviceId") String serviceId,
            @Parameter(description = "Filter by CI ID")
            @QueryParam("ciId") String ciId) {
        List<ServiceCI> results;
        if (serviceId != null && !serviceId.isBlank()) {
            results = listCIsByServiceUseCase.execute(serviceId);
        } else if (ciId != null && !ciId.isBlank()) {
            results = listServicesByCIUseCase.execute(ciId);
        } else {
            results = List.of();
        }
        List<ServiceCIResponse> items = results.stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete a service-CI association",
            description = "Deletes a service-CI association. Fails if this is the last association for the CI and the CI lacks the controlled exception flag.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Association deleted successfully"),
            @APIResponse(responseCode = "404", description = "Association not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Last service association cannot be removed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteAssociation(
            @Parameter(description = "Association identifier", required = true)
            @PathParam("id") String id) {
        deleteServiceCIUseCase.execute(id);
        return Response.noContent().build();
    }

    private ServiceCIResponse toResponse(ServiceCI serviceCI) {
        return new ServiceCIResponse(
                serviceCI.getId(),
                serviceCI.getServiceId(),
                serviceCI.getCiId(),
                serviceCI.getCreatedAt()
        );
    }
}
