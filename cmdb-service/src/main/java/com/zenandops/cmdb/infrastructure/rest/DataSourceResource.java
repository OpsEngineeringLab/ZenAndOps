package com.zenandops.cmdb.infrastructure.rest;

import com.zenandops.cmdb.application.usecase.CreateDataSourceUseCase;
import com.zenandops.cmdb.application.usecase.DeleteDataSourceUseCase;
import com.zenandops.cmdb.application.usecase.GetDataSourceUseCase;
import com.zenandops.cmdb.application.usecase.ListDataSourcesUseCase;
import com.zenandops.cmdb.application.usecase.UpdateDataSourceUseCase;
import com.zenandops.cmdb.domain.entity.DataSource;
import com.zenandops.cmdb.infrastructure.rest.dto.CreateDataSourceRequest;
import com.zenandops.cmdb.infrastructure.rest.dto.DataSourceResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.cmdb.infrastructure.rest.dto.UpdateDataSourceRequest;
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
 * REST resource exposing DataSource CRUD endpoints.
 * Write operations require ADMIN or OPERATOR role; read operations require authentication.
 */
@Path("/api/v1/cmdb/data-sources")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Data Sources",
        description = "Data source management for the CMDB")
public class DataSourceResource {

    @Inject
    CreateDataSourceUseCase createDataSourceUseCase;

    @Inject
    GetDataSourceUseCase getDataSourceUseCase;

    @Inject
    UpdateDataSourceUseCase updateDataSourceUseCase;

    @Inject
    ListDataSourcesUseCase listDataSourcesUseCase;

    @Inject
    DeleteDataSourceUseCase deleteDataSourceUseCase;

    @POST
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Create a data source",
            description = "Creates a new data source in the CMDB")
    @RequestBody(description = "Data source creation data", required = true,
            content = @Content(schema = @Schema(implementation = CreateDataSourceRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Data source created successfully",
                    content = @Content(schema = @Schema(implementation = DataSourceResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid reliability rating",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate data source name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response createDataSource(CreateDataSourceRequest request) {
        DataSource dataSource = createDataSourceUseCase.execute(
                request.name(), request.type(), request.reliabilityRating());
        return Response.status(Response.Status.CREATED)
                .entity(toResponse(dataSource))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "List data sources",
            description = "Retrieves all data sources")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Data sources retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DataSourceResponse[].class)))
    })
    public Response listDataSources() {
        List<DataSourceResponse> items = listDataSourcesUseCase.execute().stream()
                .map(this::toResponse)
                .toList();
        return Response.ok(items).build();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR", "USER"})
    @Operation(summary = "Get a data source by ID",
            description = "Retrieves a single data source by its identifier")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Data source retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DataSourceResponse.class))),
            @APIResponse(responseCode = "404", description = "Data source not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getDataSource(
            @Parameter(description = "Data source identifier", required = true)
            @PathParam("id") String id) {
        DataSource dataSource = getDataSourceUseCase.execute(id);
        return Response.ok(toResponse(dataSource)).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Update a data source",
            description = "Updates the mutable fields of an existing data source")
    @RequestBody(description = "Data source update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateDataSourceRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Data source updated successfully",
                    content = @Content(schema = @Schema(implementation = DataSourceResponse.class))),
            @APIResponse(responseCode = "400", description = "Invalid reliability rating",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "404", description = "Data source not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Duplicate data source name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateDataSource(
            @Parameter(description = "Data source identifier", required = true)
            @PathParam("id") String id,
            UpdateDataSourceRequest request) {
        DataSource dataSource = updateDataSourceUseCase.execute(
                id, request.name(), request.reliabilityRating());
        return Response.ok(toResponse(dataSource)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "OPERATOR"})
    @Operation(summary = "Delete a data source",
            description = "Deletes a data source. Fails if referenced by any version.")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Data source deleted successfully"),
            @APIResponse(responseCode = "404", description = "Data source not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @APIResponse(responseCode = "409", description = "Data source is in use",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response deleteDataSource(
            @Parameter(description = "Data source identifier", required = true)
            @PathParam("id") String id) {
        deleteDataSourceUseCase.execute(id);
        return Response.noContent().build();
    }

    private DataSourceResponse toResponse(DataSource dataSource) {
        return new DataSourceResponse(
                dataSource.getId(),
                dataSource.getName(),
                dataSource.getType(),
                dataSource.getReliabilityRating(),
                dataSource.getCreatedAt(),
                dataSource.getUpdatedAt()
        );
    }
}
