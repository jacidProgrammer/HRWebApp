package dev.jacid.hrApplication.adapter.in.http.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.dto.ErrorResponse;
import dev.jacid.hrApplication.adapter.in.http.dto.StatsOverviewDTO;
import dev.jacid.hrApplication.adapter.in.http.mappers.StatsDtoMapper;
import dev.jacid.hrApplication.application.port.in.StatsUseCases;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/stats")
@Tag(name = "Insights", description = "Aggregated figures for managers; no message content.")
public class StatsController {

    private final StatsUseCases statsUseCases;
    private final StatsDtoMapper mapper;

    public StatsController(StatsUseCases statsUseCases, StatsDtoMapper mapper) {
        this.statsUseCases = statsUseCases;
        this.mapper = mapper;
    }

    @GetMapping("/overview")
    @Operation(summary = "Dashboard overview (managers)", description = """
            Headcount, feedback volume, sentiment share and monthly trend, recognised company values, the most \
            recognised employees of the last 90 days and aggregated alerts when the share of positive feedback about \
            someone drops. All months are UTC calendar months.""")
    @ApiResponse(responseCode = "200", description = "The dashboard figures")
    @ApiResponse(responseCode = "400", description = "months is outside 1..12",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PreAuthorize("hasRole('MANAGER')")
    public StatsOverviewDTO getOverview(
            @Parameter(description = "Length of the trend period in calendar months (1..12)")
            @RequestParam(defaultValue = "" + StatsUseCases.DEFAULT_MONTHS) int months) {
        return mapper.toDto(statsUseCases.getOverview(months));
    }
}
