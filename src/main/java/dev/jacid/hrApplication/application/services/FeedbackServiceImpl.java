package dev.jacid.hrApplication.application.services;

import java.util.List;

import org.springframework.stereotype.Service;

import dev.jacid.hrApplication.adapter.out.persistence.EmployeeJpaEntity;
import dev.jacid.hrApplication.adapter.out.persistence.FeedbackJpaEntity;
import dev.jacid.hrApplication.application.mappers.FeedbackMapper;
import dev.jacid.hrApplication.application.port.in.FeedbackUseCases;
import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.infrastructure.security.AuthenticatedUser;
import dev.jacid.hrApplication.domain.model.dto.FeedbackDTO;
import dev.jacid.hrApplication.domain.model.dto.HuggingFaceResultDTO;
import jakarta.transaction.Transactional;
import reactor.core.publisher.Mono;

@Service
public class FeedbackServiceImpl implements FeedbackUseCases {

    private final EmployeeRepository employeeRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackMapper feedbackMapper;
    private final AuthenticatedUser authenticatedUser;
    private final HuggingFaceServiceImpl huggingFaceServiceImpl;

    public FeedbackServiceImpl(EmployeeRepository employeeRepository,
                               FeedbackRepository feedbackRepository,
                               FeedbackMapper feedbackMapper,
                               HuggingFaceServiceImpl huggingFaceServiceImpl,
                               AuthenticatedUser auth) {
        this.employeeRepository = employeeRepository;
        this.feedbackRepository = feedbackRepository;
        this.feedbackMapper = feedbackMapper;
        this.huggingFaceServiceImpl = huggingFaceServiceImpl;
        this.authenticatedUser = auth;
    }
    
    @Override
    public List<FeedbackDTO> getAllFeedbacks() {
        return feedbackRepository.findAll().stream().map(feedbackMapper::toDto).toList();
    }

    @Override
    public List<FeedbackDTO> getFeedbackByEmployeeName(String name) {
        List<FeedbackJpaEntity> feedbackEntity = feedbackRepository.findByEmployeeName(name);
        if(feedbackEntity == null) {
            throw new IllegalArgumentException("Feedback with this name doesn't exist");
        }
        return feedbackEntity.stream().map(feedbackMapper::toDto).toList();
    }

    @Override
    @Transactional
    public FeedbackDTO sendFeedback(FeedbackDTO feedbackDTO) {
        EmployeeJpaEntity employeeJpaEntity = employeeRepository.findByName(feedbackDTO.name());
        if(employeeJpaEntity == null) {
            throw new IllegalArgumentException("Employee with this name doesn't exists");
        }

        EmployeeJpaEntity reporter = employeeRepository.findByName(authenticatedUser.getUserName());
        if(reporter == null) {
            throw new IllegalArgumentException("Reporter with this name doesn't exists");
        }
        
        FeedbackJpaEntity feedbackJpaEntity = feedbackMapper.toEntity(feedbackDTO);
        feedbackJpaEntity.setReporter(reporter);
        feedbackJpaEntity.setEmployee(employeeJpaEntity);

        Mono<List<HuggingFaceResultDTO>> result = huggingFaceServiceImpl.analyzeSentiment(feedbackDTO.message());
        if(result != null) {
            result.blockOptional().stream().findFirst().ifPresent(huggingFaceResult -> {
                HuggingFaceResultDTO response = huggingFaceResult.getFirst();
                feedbackJpaEntity.setLabel(response.label());
                feedbackJpaEntity.setScore(response.score());
            });
        }
        
        feedbackRepository.save(feedbackJpaEntity);
        return feedbackMapper.toDto(feedbackJpaEntity);
    }
}
