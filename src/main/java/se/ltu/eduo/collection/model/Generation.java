package se.ltu.eduo.collection.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Records a single generation attempt within a project. Acts as the anchor
 * for the output type hierarchy: It is designed to accommodate other output types (flashcards,
 * etc.) as sibling one-to-one relationships on this entity.
 */
@Entity
@Table(name = "generation")
@Getter
@Setter
@NoArgsConstructor
public class Generation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "generation_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Collection collection;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GenerationSourceMaterial> sourceMaterials = new ArrayList<>();

    /**
     * The quiz produced by this generation attempt. Null until the generation
     * completes successfully.
     */
    @OneToOne(mappedBy = "generation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Quiz quiz;

    @Column(name = "num_of_questions", nullable = false)
    private int numOfQuestions;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private GenerationLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_area", nullable = false)
    private GenerationFocusArea focusArea;

    @Column(name = "topics")
    private String topics;

    @Column(name = "easy", nullable = false)
    private boolean easy;

    @Column(name = "medium", nullable = false)
    private boolean medium;

    @Column(name = "hard", nullable = false)
    private boolean hard;

    @Column(name = "multiple_choice", nullable = false)
    private boolean multipleChoice;

    @Column(name = "open_ended", nullable = false)
    private boolean openEnded;

    @Column(name = "true_false", nullable = false)
    private boolean trueFalse;

    @Column(name = "questions", nullable = false)
    private boolean questions;

    //TODO clearer column name?
    @Column(name = "correct_answers", nullable = false)
    private boolean correctAnswers;

    //TODO clearer column name?
    @Column(name = "explanations", nullable = false)
    private boolean explanations;

    //TODO clearer column name?
    @Column(name = "description", nullable = false)
    private boolean description;

    public Generation(Collection collection) {
        this.collection = collection;
    }
}
