package se.ltu.eduo.model.collection;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * The raw output of a generation attempt, stored as unstructured text.
 * Parsing the LLM response into structured questions and answers is deferred —
 * {@code rawContent} holds the full response until that work is done.
 *
 * <p>Always exactly one {@code Quiz} per {@link Generation}. The generation
 * is the owning side of this relationship.
 */
@Entity
@Table(name = "quiz", schema = "eduo")
@Getter
@Setter
@NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "quiz_id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_id", nullable = false, updatable = false)
    private Generation generation;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The complete, unparsed text returned by the AI provider for this
     * generation. Stored as PostgreSQL {@code TEXT} with no length cap.
     */
    @Column(name = "raw_content", nullable = false, columnDefinition = "TEXT")
    private String rawContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Quiz(Generation generation, String name, String rawContent) {
        this.generation = generation;
        this.name = name;
        this.rawContent = rawContent;
    }
}
