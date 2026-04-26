package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.dto.ServiceTreeNode;
import com.zenandops.cmdb.application.usecase.CreateServiceUseCase;
import com.zenandops.cmdb.application.usecase.DeleteServiceUseCase;
import com.zenandops.cmdb.application.usecase.GetServiceTreeUseCase;
import com.zenandops.cmdb.application.usecase.GetServiceUseCase;
import com.zenandops.cmdb.application.usecase.ListServicesUseCase;
import com.zenandops.cmdb.application.usecase.UpdateServiceUseCase;
import com.zenandops.cmdb.domain.entity.Service;
import com.zenandops.cmdb.domain.vo.CriticalityLevel;
import com.zenandops.cmdb.domain.vo.ServiceStatus;
import com.zenandops.cmdb.domain.vo.ServiceType;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateServiceRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.ServiceResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.UpdateServiceRequest;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
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
 * REST resource exposing Service CRUD, filtering, and tree endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Services",
        description = "Service hierarchy and lifecycle management for the CMDB")
public class ServiceResource {

    @Inject
    CreateServiceUseCase createServiceUseCase;

    @Inject
    GetServiceUseCase getServiceUseCase;

    @Inject
    UpdateServiceUseCase updateServiceUseCase;

    @Inject
    ListServicesUseCase listServicesUseCase;

    @Inject
    DeleteServiceUseCase deleteServiceUseCase;

    @Inject
    GetServiceTreeUseCase getServiceTreeUseCase;

    @Context
    SecurityContext securityContext;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a service",
            description = "Creates a new service in the hierarchy")
    @RequestBody(description = "Service creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateServiceRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Service created successfully",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @APIResponse(responseCode = "404", description = "Organization or parent service not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid request (missing owners)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createService(CreateServiceRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        Service service = createServiceUseCase.execute(
                request.name(), request.description(), request.type(), request.parentId(),
                request.organizationId(), request.businessOwner(), request.technicalOwner(),
                request.criticality(), request.status(), userId);
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(service))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List services",
            description = "Retrieves services with optional filtering by organizationId, type, criticality, and status")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Services retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServiceResponse[].class)))
    })
    public Response listServices(
            @Parameter(description = "Filter by organization ID")
            @QueryParam("organizationId") String organizationId,
            @Parameter(description = "Filter by service type")
            @QueryParam("type") ServiceType type,
            @Parameter(description = "Filter by criticality level")
            @QueryParam("criticality") CriticalityLevel criticality,
            @Parameter(description = "Filter by service status")
            @QueryParam("status") ServiceStatus status) {
        List<ServiceResponse> items = listServicesUseCase.execute(
                organizationId, type, criticality, status).stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get a service by ID",
            description = "Retrieves a single service by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Service retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @APIResponse(responseCode = "404", description = "Service not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getService(
            @Parameter(description = "Service identifier", required = true)
            @PathParam("id") String id) {
        Service service = getServiceUseCase.execute(id);
        return Response.ok(toResponse(service)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Update a service",
            description = "Updates the mutable fields of an existing service")
    @RequestBody(description = "Service update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateServiceRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Service updated successfully",
                    content = @Content(schema = @Schema(implementation = ServiceResponse.class))),
            @APIResponse(responseCode = "404", description = "Service not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateService(
            @Parameter(description = "Service identifier", required = true)
            @PathParam("id") String id,
            UpdateServiceRequest request) {
        String userId = securityContext.getUserPrincipal().getName();
        Service service = updateServiceUseCase.execute(
                id, request.name(), request.description(),
                request.businessOwner(), request.technicalOwner(),
                request.criticality(), request.status(), userId);
        return Response.ok(toResponse(service)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete a service",
            description = "Deletes a service. Fails if the service has children, dependencies, or CI associations.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Service deleted successfully"),
            @APIResponse(responseCode = "404", description = "Service not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Service is in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteService(
            @Parameter(description = "Service identifier", required = true)
            @PathParam("id") String id) {
        deleteServiceUseCase.execute(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/tree")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get the service hierarchy tree",
            description = "Returns the complete service hierarchy starting from root-level services")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Service tree retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServiceTreeNode[].class)))
    })
    public Response getServiceTree() {
        List<ServiceTreeNode> tree = getServiceTreeUseCase.execute();
        return Response.ok(tree).build();
    }

    private ServiceResponse toResponse(Service service) {
        return new ServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getType(),
                service.getParentId(),
                service.getOrganizationId(),
                service.getBusinessOwner(),
                service.getTechnicalOwner(),
                service.getCriticality(),
                service.getStatus(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
