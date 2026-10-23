package controllers;

import services.SessionService;
import entities.SessionEntity;
import dto.request.RevokeSessionRequest;
import dto.response.SessionCheckResponse;
import config.OpenApiConfig;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@Tag(name = "Sessions (internal)")  // description and display order: OpenApiConfig
@RestController
@RequestMapping("/internal/sessions")
public class InternalSessionController {
    static final int MAX_REASON_LENGTH = 64;

    private SessionService sessionService;
    public InternalSessionController(SessionService sessionService){
        this.sessionService = sessionService;
    }

    @Operation(summary = "Check a session before a token refresh",
        description = "Succeeds only while the session is live (not revoked, not past its cap, not idle), and then counts "
            + "as activity. The auth service calls this before it rotates a refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Session is live; expiresAt is its absolute end"),
        @ApiResponse(responseCode = "401", description = "error is session_invalid, session_revoked, session_expired or session_idle",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"error\":\"session_idle\",\"message\":\"Session ended after a period of inactivity, please log in again\"}"))),
        @ApiResponse(responseCode = "503", description = "The session store could not be read",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"error\":\"session_store_unavailable\"}")))
    })
    @SecurityRequirement(name = OpenApiConfig.INTERNAL_KEY_SCHEME)
    @PostMapping("/{sid}/check")
    public SessionCheckResponse check(@PathVariable String sid){
        SessionEntity session = sessionService.checkAndTouch(sid);
        return new SessionCheckResponse(session.getExpiresAt().toString());
    }

    @Operation(summary = "End a session",
        description = "Revokes the session, so its access tokens stop working on the next request. Idempotent: an unknown "
            + "or already revoked sid also answers 204, and a second revoke keeps the first reason.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session is revoked (or never existed)"),
        @ApiResponse(responseCode = "503", description = "The session store could not be written", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.INTERNAL_KEY_SCHEME)
    @PostMapping("/{sid}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable String sid, @RequestBody(required = false) RevokeSessionRequest request){
        String reason = request == null || request.reason() == null || request.reason().isBlank() ? "logout" : request.reason().trim();
        if (reason.length() > MAX_REASON_LENGTH) {
            reason = reason.substring(0, MAX_REASON_LENGTH);
        }
        sessionService.revoke(sid, reason);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> storeUnavailable(DataAccessException ex){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "session_store_unavailable"));
    }
}
