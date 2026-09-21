package dev.jacid.hrApplication.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;

/** Every query returns feedback newest first, with author and recipient loaded. */
public interface FeedbackRepository {
    List<Feedback> findByRecipientId(UUID recipientId);
    List<Feedback> findByAuthorId(UUID authorId);
    List<Feedback> search(FeedbackFilter filter);
    List<Feedback> findCreatedSince(Instant since);
    long count();
    /** Inserts new feedback; the id is generated. */
    Feedback save(Feedback feedback);
}
