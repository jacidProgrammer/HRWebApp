package dev.jacid.hrApplication.adapter.in.http.mappers;

import java.time.YearMonth;

import org.mapstruct.Mapper;

import dev.jacid.hrApplication.adapter.in.http.dto.StatsOverviewDTO;
import dev.jacid.hrApplication.domain.model.stats.StatsOverview;

@Mapper(componentModel = "spring")
public interface StatsDtoMapper {

    StatsOverviewDTO toDto(StatsOverview overview);

    /** {@code 2026-04}. */
    default String month(YearMonth month) {
        return month == null ? null : month.toString();
    }
}
