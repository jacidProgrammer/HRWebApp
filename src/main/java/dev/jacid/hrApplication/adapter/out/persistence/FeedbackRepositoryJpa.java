package dev.jacid.hrApplication.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Every finder fetches recipient and author in the same query. */
public interface FeedbackRepositoryJpa extends JpaRepository<FeedbackJpaEntity, UUID>,
        JpaSpecificationExecutor<FeedbackJpaEntity> {

    @EntityGraph(attributePaths = {"recipient", "author"})
    List<FeedbackJpaEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    @EntityGraph(attributePaths = {"recipient", "author"})
    List<FeedbackJpaEntity> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    @EntityGraph(attributePaths = {"recipient", "author"})
    List<FeedbackJpaEntity> findByCreatedAtGreaterThanEqualOrderByCreatedAtDesc(Instant since);

    @Override
    @EntityGraph(attributePaths = {"recipient", "author"})
    List<FeedbackJpaEntity> findAll(Specification<FeedbackJpaEntity> spec, Sort sort);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from FeedbackJpaEntity f where f.recipient.id = :employeeId")
    int deleteByRecipientId(@Param("employeeId") UUID employeeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update FeedbackJpaEntity f set f.author = null where f.author.id = :employeeId")
    int detachAuthor(@Param("employeeId") UUID employeeId);
}
