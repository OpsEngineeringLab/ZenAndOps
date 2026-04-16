package com.zenandops.auth.infrastructure.rest;

import com.zenandops.auth.application.usecase.LoginUseCase;
import com.zenandops.auth.application.usecase.LogoffUseCase;
import com.zenandops.auth.application.usecase.RefreshTokenUseCase;
import com.zenandops.auth.application.usecase.TokenPair;
import com.zenandops.auth.infrastructure.rest.dto.ErrorResponse;
import com.zenandops.auth.infrastructure.rest.dto.LoginRequest;
import com.zenandops.auth.infrastructure.rest.dto.RefreshRequest;
import com.zenandops.auth.infrastructure.rest.dto.TokenResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST resource exposing authentication endpoints.
 */
@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "User authentication, token issuance, and session management")
public class AuthResource {

    @Inject
    LoginUseCase loginUseCase;

    @Inject
    RefreshTokenUseCase refreshTokenUseCase;

    @Inject
    LogoffUseCase logoffUseCase;

    @POST
    @Path("/login")
    @Operation(summary = "User login", description = "Authenticates a user with login and password, returning access and refresh tokens")
    @RequestBody(description = "User credentials", required = true,
            content = @Content(schema = @Schema(implementation = LoginRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @APIResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response login(LoginRequest request) {
        TokenPair tokenPair = loginUseCase.execute(request.login(), request.password());
        return Response.ok(new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken()))
                .build();
    }

    @POST
    @Path("/refresh")
    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    @RequestBody(description = "Refresh token", required = true,
            content = @Content(schema = @Schema(implementation = RefreshRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @APIResponse(responseCode = "401", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response refresh(RefreshRequest request) {
        TokenPair tokenPair = refreshTokenUseCase.execute(request.refreshToken());
        return Response.ok(new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken()))
                .build();
    }

    @POST
    @Path("/logoff")
    @Authenticated
    @Operation(summary = "User logoff", description = "Invalidates the refresh token, ending the user session")
    @SecurityRequirement(name = "bearerAuth")
    @RequestBody(description = "Refresh token to invalidate", required = true,
            content = @Content(schema = @Schema(implementation = RefreshRequest.class)))
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Logoff successful"),
            @APIResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Response logoff(RefreshRequest request) {
        logoffUseCase.execute(request.refreshToken());
        return Response.noContent().build();
    }
}
