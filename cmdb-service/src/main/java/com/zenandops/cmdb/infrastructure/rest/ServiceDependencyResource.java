package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateServiceDependencyUseCase;
import com.zenandops.cmdb.application.usecase.DeleteServiceDependencyUseCase;
import com.zenandops.cmdb.application.usecase.ListServiceDependenciesUseCase;
import com.zenandops.cmdb.domain.entity.ServiceDependency;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateServiceDependencyRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.PaginatedResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.ServiceDependencyResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * REST resource exposing ServiceDependency CRUD endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/service-dependencies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Service Dependencies",
        description = "Service dependency management for the CMDB")
public class ServiceDependencyResource {

    @Inject
    CreateServiceDependencyUseCase createServiceDependencyUseCase;

    @Inject
    DeleteServiceDependencyUseCase deleteServiceDependencyUseCase;

    @Inject
    ListServiceDependenciesUseCase listServiceDependenciesUseCase;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a service dependency",
            description = "Creates a directed dependency between two services")
    @RequestBody(description = "Service dependency creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateServiceDependencyRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Dependency created successfully",
                    content = @Content(schema = @Schema(implementation = ServiceDependencyResponse.class))),
            @APIResponse(responseCode = "404", description = "Source or target service not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "400", description = "Self-reference not allowed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate dependency",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createDependency(CreateServiceDependencyRequest request) {
        ServiceDependency dependency = createServiceDependencyUseCase.execute(
                request.sourceServiceId(), request.targetServiceId(), request.dependencyType());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(dependency))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List service dependencies",
            description = "Returns all dependencies (upstream and downstream) for a given service")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Dependencies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ServiceDependencyResponse[].class)))
    })
    public Response listDependencies(
            @Parameter(description = "Page number (zero-based)")
            @QueryParam("page") @DefaultValue("0") int page,
            @Parameter(description = "Page size")
            @QueryParam("size") @DefaultValue("50") int size,
            @Parameter(description = "Service ID to list dependencies for", required = true)
            @QueryParam("serviceId") String serviceId) {
        if (page < 0 || size < 1 || size > 200) {
            return Response.status(400)
                    .entity(Map.of("error", new ErrorResponse("CMDB_VALIDATION_ERROR",
                            "page must be >= 0, size must be between 1 and 200",
                            Instant.now())))
                    .build();
        }
        var result = listServiceDependenciesUseCase.execute(serviceId, page, size);
        List<ServiceDependencyResponse> items = result.items().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(PaginatedResponse.of(items, page, size, result.totalItems())).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete a service dependency",
            description = "Deletes a service dependency by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Dependency deleted successfully"),
            @APIResponse(responseCode = "404", description = "Dependency not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteDependency(
            @Parameter(description = "Dependency identifier", required = true)
            @PathParam("id") String id) {
        deleteServiceDependencyUseCase.execute(id);
        return Response.noContent().build();
    }

    private ServiceDependencyResponse toResponse(ServiceDependency dependency) {
        return new ServiceDependencyResponse(
                dependency.getId(),
                dependency.getSourceServiceId(),
                dependency.getTargetServiceId(),
                dependency.getDependencyType(),
                dependency.getCreatedAt()
        );
    }
}
