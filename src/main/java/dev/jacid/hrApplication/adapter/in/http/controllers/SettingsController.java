package dev.jacid.hrApplication.adapter.in.http.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.dto.SettingsDTO;
import dev.jacid.hrApplication.adapter.in.http.dto.SettingsRequestDTO;
import dev.jacid.hrApplication.application.port.in.SettingsStatus;
import dev.jacid.hrApplication.application.port.in.SettingsUseCases;
import dev.jacid.hrApplication.domain.exception.InvalidRequestException;

@RestController
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsUseCases settingsUseCases;

    public SettingsController(SettingsUseCases settingsUseCases) {
        this.settingsUseCases = settingsUseCases;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'EMPLOYEE')")
    public SettingsDTO getSettings() {
        return toDto(settingsUseCases.getSettings());
    }

    @PutMapping
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
