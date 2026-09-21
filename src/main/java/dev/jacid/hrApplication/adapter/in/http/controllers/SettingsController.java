package dev.jacid.hrApplication.adapter.in.http.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.dto.ErrorResponse;
import dev.jacid.hrApplication.adapter.in.http.dto.SettingsDTO;
import dev.jacid.hrApplication.adapter.in.http.dto.SettingsRequestDTO;
import dev.jacid.hrApplication.application.port.in.SettingsStatus;
import dev.jacid.hrApplication.application.port.in.SettingsUseCases;
import dev.jacid.hrApplication.domain.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/settings")
@Tag(name = "Settings", description = "Runtime switches managers control, such as the AI sentiment analysis.")
public class SettingsController {

    private final SettingsUseCases settingsUseCases;

    public SettingsController(SettingsUseCases settingsUseCases) {
        this.settingsUseCases = settingsUseCases;
    }

    @GetMapping
    @Operation(summary = "Get the settings",
            description = "Whether managers enabled the sentiment analysis and whether it is configured at all.")
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public SettingsDTO getSettings() {
        return toDto(settingsUseCases.getSettings());
    }

    @PutMapping
    @Operation(summary = "Update the settings (managers)",
            description = "While the analysis is disabled nothing is sent to the external model.")
    @ApiResponse(responseCode = "200", description = "The updated settings")
    @ApiResponse(responseCode = "400", description = "sentimentAnalysisEnabled is missing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('MANAGER')")
    public SettingsDTO updateSettings(@RequestBody SettingsRequestDTO request) {
        if (request.sentimentAnalysisEnabled() == null) {
            throw new InvalidRequestException("sentimentAnalysisEnabled is required");
        }
        return toDto(settingsUseCases.updateSettings(request.sentimentAnalysisEnabled()));
    }

    private static SettingsDTO toDto(SettingsStatus status) {
        return new SettingsDTO(status.sentimentAnalysisEnabled(), status.sentimentAnalysisAvailable());
    }
}
