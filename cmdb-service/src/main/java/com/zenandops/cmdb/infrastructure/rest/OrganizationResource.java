package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.dto.OrganizationTreeNode;
import com.zenandops.cmdb.application.usecase.CreateOrganizationUseCase;
import com.zenandops.cmdb.application.usecase.DeleteOrganizationUseCase;
import com.zenandops.cmdb.application.usecase.GetOrganizationTreeUseCase;
import com.zenandops.cmdb.application.usecase.GetOrganizationUseCase;
import com.zenandops.cmdb.application.usecase.ListOrganizationsUseCase;
import com.zenandops.cmdb.application.usecase.UpdateOrganizationUseCase;
import com.zenandops.cmdb.domain.entity.Organization;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateOrganizationRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.OrganizationResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.UpdateOrganizationRequest;
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
 * REST resource exposing Organization CRUD and tree endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Organizations",
        description = "Organization hierarchy management for the CMDB")
public class OrganizationResource {

    @Inject
    CreateOrganizationUseCase createOrganizationUseCase;

    @Inject
    GetOrganizationUseCase getOrganizationUseCase;

    @Inject
    UpdateOrganizationUseCase updateOrganizationUseCase;

    @Inject
    ListOrganizationsUseCase listOrganizationsUseCase;

    @Inject
    DeleteOrganizationUseCase deleteOrganizationUseCase;

    @Inject
    GetOrganizationTreeUseCase getOrganizationTreeUseCase;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create an organization",
            description = "Creates a new organization in the hierarchy")
    @RequestBody(description = "Organization creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateOrganizationRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Organization created successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate ROOT or sibling name conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Parent organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createOrganization(CreateOrganizationRequest request) {
        Organization org = createOrganizationUseCase.execute(
                request.name(), request.type(), request.parentId(),
                request.responsiblePerson(), request.costCenter());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(org))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List all organizations",
            description = "Retrieves all organizations")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Organizations retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse[].class)))
    })
    public Response listOrganizations() {
        List<OrganizationResponse> items = listOrganizationsUseCase.execute().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get an organization by ID",
            description = "Retrieves a single organization by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Organization retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @APIResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getOrganization(
            @Parameter(description = "Organization identifier", required = true)
            @PathParam("id") String id) {
        Organization org = getOrganizationUseCase.execute(id);
        return Response.ok(toResponse(org)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Update an organization",
            description = "Updates the name, responsible person, and cost center of an existing organization")
    @RequestBody(description = "Organization update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateOrganizationRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Organization updated successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationResponse.class))),
            @APIResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate sibling name conflict",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateOrganization(
            @Parameter(description = "Organization identifier", required = true)
            @PathParam("id") String id,
            UpdateOrganizationRequest request) {
        Organization org = updateOrganizationUseCase.execute(
                id, request.name(), request.responsiblePerson(), request.costCenter());
        return Response.ok(toResponse(org)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete an organization",
            description = "Deletes an organization. Fails if the organization has children, services, or assets.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Organization deleted successfully"),
            @APIResponse(responseCode = "404", description = "Organization not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Organization is in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteOrganization(
            @Parameter(description = "Organization identifier", required = true)
            @PathParam("id") String id) {
        deleteOrganizationUseCase.execute(id);
        return Response.noContent().build();
    }

    @GET
    @Path("/tree")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get the full organizational tree",
            description = "Returns the complete organizational hierarchy starting from ROOT")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Organizational tree retrieved successfully",
                    content = @Content(schema = @Schema(implementation = OrganizationTreeNode[].class)))
    })
    public Response getOrganizationTree() {
        List<OrganizationTreeNode> tree = getOrganizationTreeUseCase.execute();
        return Response.ok(tree).build();
    }

    private OrganizationResponse toResponse(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getType(),
                org.getParentId(),
                org.getResponsiblePerson(),
                org.getCostCenter(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
