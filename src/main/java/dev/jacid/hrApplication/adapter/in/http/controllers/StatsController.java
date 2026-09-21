package dev.jacid.hrApplication.adapter.in.http.controllers;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.jacid.hrApplication.adapter.in.http.dto.StatsOverviewDTO;
import dev.jacid.hrApplication.adapter.in.http.mappers.StatsDtoMapper;
import dev.jacid.hrApplication.application.port.in.StatsUseCases;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final StatsUseCases statsUseCases;
    private final StatsDtoMapper mapper;

    public StatsController(StatsUseCases statsUseCases, StatsDtoMapper mapper) {
        this.statsUseCases = statsUseCases;
        this.mapper = mapper;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('MANAGER')")
    public StatsOverviewDTO getOverview(@RequestParam(defaultValue = "" + StatsUseCases.DEFAULT_MONTHS) int months) {
        return mapper.toDto(statsUseCases.getOverview(months));
    }
}
