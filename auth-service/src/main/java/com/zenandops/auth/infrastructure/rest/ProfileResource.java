package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.port.TagRepository;
import com.zenandops.auth.application.usecase.ChangePasswordUseCase;
import com.zenandops.auth.application.usecase.GetProfileUseCase;
import com.zenandops.auth.application.usecase.UpdateProfileUseCase;
import com.zenandops.auth.domain.entity.Tag;
import com.zenandops.auth.domain.entity.User;
import com.zenandops.auth.infrastructure.rest.dto.ChangePasswordRequest;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.ProfileResponse;
import com.zenandops.auth.infrastructure.rest.dto.TagResponse;
import com.zenandops.auth.infrastructure.rest.dto.UpdateProfileRequest;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;

/**
 * REST resource exposing self-service profile endpoints. Any authenticated user can access.
 */
@Path("/api/v1/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearerAuth")
@org.eclipse.microprofile.openapi.annotations.tags.Tag(name = "Profile", description = "Self-service profile operations")
public class ProfileResource {

    @Inject
    GetProfileUseCase getProfileUseCase;

    @Inject
    UpdateProfileUseCase updateProfileUseCase;

    @Inject
    ChangePasswordUseCase changePasswordUseCase;

    @Inject
    TagRepository tagRepository;

    @Context
    SecurityContext securityContext;

    @GET
    @Operation(summary = "Get current user profile", description = "Retrieves the profile of the currently authenticated user")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @APIResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response getProfile() {
        String login = securityContext.getUserPrincipal().getName();
        User user = getProfileUseCase.execute(login);
        return Response.ok(toProfileResponse(user)).build();
    }

    @PUT
    @Operation(summary = "Update current user profile", description = "Updates the name and email of the currently authenticated user")
    @RequestBody(description = "Profile update data", required = true,
            content = @Content(schema = @Schema(implementation = UpdateProfileRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))),
            @APIResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response updateProfile(UpdateProfileRequest request) {
        String login = securityContext.getUserPrincipal().getName();
        User user = updateProfileUseCase.execute(login, request.name(), request.email());
        return Response.ok(toProfileResponse(user)).build();
    }

    @POST
    @Path("/password")
    @Operation(summary = "Change password", description = "Changes the password of the currently authenticated user")
    @RequestBody(description = "Password change data", required = true,
            content = @Content(schema = @Schema(implementation = ChangePasswordRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Password changed successfully"),
            @APIResponse(responseCode = "401", description = "Current password is incorrect",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response changePassword(ChangePasswordRequest request) {
        String login = securityContext.getUserPrincipal().getName();
        changePasswordUseCase.execute(login, request.currentPassword(), request.newPassword());
        return Response.noContent().build();
    }

    private ProfileResponse toProfileResponse(User user) {
        List<TagResponse> tags = resolveTags(user.getTagIds());
        return new ProfileResponse(user.getLogin(), user.getName(), user.getEmail(),
                user.getRoles(), tags);
    }

    private List<TagResponse> resolveTags(List<String> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<Tag> tags = tagRepository.findAllByIds(tagIds);
        return tags.stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getKey(), tag.getValue(),
                        tag.getDescription(), tag.getCreatedAt(), tag.getUpdatedAt()))
                .toList();
    }
}
