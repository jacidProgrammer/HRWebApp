package dev.jacid.hrApplication.application.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.application.port.in.StatsUseCases;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.application.port.out.TimeProvider;
import dev.jacid.hrApplication.domain.exception.InvalidRequestException;
import dev.jacid.hrApplication.domain.model.stats.StatsOverview;
import dev.jacid.hrApplication.domain.service.StatsCalculator;

/**
 * Loads the employees and the recent feedback (only what the figures need) and delegates the
 * aggregation to {@link StatsCalculator}.
 */
@Service
public class StatsServiceImpl implements StatsUseCases {

    private final EmployeeRepository employeeRepository;
    private final FeedbackRepository feedbackRepository;
    private final TimeProvider timeProvider;

    public StatsServiceImpl(EmployeeRepository employeeRepository, FeedbackRepository feedbackRepository,
                            TimeProvider timeProvider) {
        this.employeeRepository = employeeRepository;
        this.feedbackRepository = feedbackRepository;
        this.timeProvider = timeProvider;
    }

    @Override
    public StatsOverview getOverview(int months) {
        if (months < 1 || months > MAX_MONTHS) {
            throw new InvalidRequestException("months must be between 1 and " + MAX_MONTHS);
        }
        Instant now = timeProvider.now();
        return StatsCalculator.calculate(
                employeeRepository.findAll(),
                feedbackRepository.findCreatedSince(StatsCalculator.earliestRelevant(now, months)),
                feedbackRepository.count(),
                now,
                months);
    }
}
