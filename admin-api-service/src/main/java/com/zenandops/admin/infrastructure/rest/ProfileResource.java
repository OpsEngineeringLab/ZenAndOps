package com.zenandops.admin.infrastructure.rest;

import com.zenandops.admin.application.usecase.GetProfileUseCase;
import com.zenandops.admin.application.usecase.GetUserRolesUseCase;
import com.zenandops.admin.application.usecase.ResetPasswordUseCase;
import com.zenandops.admin.application.usecase.UpdateProfileUseCase;
import com.zenandops.admin.domain.exception.ForbiddenException;
import com.zenandops.admin.infrastructure.adapter.keycloak.UserResponseTranslator;
import com.zenandops.admin.infrastructure.rest.dto.PasswordChangeRequest;
import com.zenandops.admin.infrastructure.rest.dto.UpdateUserRequest;
import com.zenandops.admin.infrastructure.rest.dto.UserResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resource for the authenticated user's own profile management.
 * Uses the {@code sub} claim from the JWT to identify the current user.
 * Delegates to application-layer use cases which orchestrate calls through
 * port interfaces, keeping this resource decoupled from infrastructure adapters.
 */
@Path("/api/v1/profile")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class ProfileResource {

    @Inject
    GetProfileUseCase getProfileUseCase;

    @Inject
    UpdateProfileUseCase updateProfileUseCase;

    @Inject
    ResetPasswordUseCase resetPasswordUseCase;

    @Inject
    GetUserRolesUseCase getUserRolesUseCase;

    @Inject
    JsonWebToken jwt;

    @GET
    public UserResponse getProfile() {
        requirePermission("profile:read");
        String userId = getCurrentUserId();
        Map<String, Object> user = getProfileUseCase.execute(userId);

        List<Map<String, Object>> roles = getUserRolesUseCase.execute(userId);
        List<String> roleNames = roles.stream()
                .map(r -> String.valueOf(r.get("name")))
                .toList();

        return UserResponseTranslator.toUserResponse(user, roleNames, List.of());
    }

    @PUT
    public Response updateProfile(UpdateUserRequest request) {
        requirePermission("profile:write");
        String userId = getCurrentUserId();
        Map<String, Object> keycloakUpdate = UserResponseTranslator.toKeycloakUserUpdate(request);
        updateProfileUseCase.execute(userId, keycloakUpdate);
        return Response.noContent().build();
    }

    @POST
    @Path("/password")
    public Response changePassword(PasswordChangeRequest request) {
        requirePermission("profile:write");
        String userId = getCurrentUserId();
        resetPasswordUseCase.execute(userId, request.newPassword(), false);
        return Response.noContent().build();
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private String getCurrentUserId() {
        // The 'sub' claim in Keycloak tokens contains the user ID
        return jwt.getSubject();
    }

    @SuppressWarnings("unchecked")
    private void requirePermission(String permission) {
        Object permissionsClaim = jwt.getClaim("permissions");
        Set<String> permissions;
        if (permissionsClaim instanceof Set<?> s) {
            permissions = (Set<String>) s;
        } else if (permissionsClaim instanceof List<?> l) {
            permissions = new java.util.HashSet<>((List<String>) (List<?>) l);
        } else {
            permissions = Set.of();
        }
        if (!permissions.contains(permission)) {
            throw new ForbiddenException("Insufficient permissions");
        }
    }
}
