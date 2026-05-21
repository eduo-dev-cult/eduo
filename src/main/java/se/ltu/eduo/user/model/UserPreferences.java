package se.ltu.eduo.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import se.ltu.eduo.collection.model.GenerationFocusArea;
import se.ltu.eduo.collection.model.GenerationLanguage;

@Getter
@Setter
@Entity
@Table(name = "user_preferences", schema = "eduo")
public class UserPreferences {
    @Id
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "locale", length = Integer.MAX_VALUE)
    private String locale;

    //settings
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

    // difficulty
    @Column(name = "easy", nullable = false)
    private boolean easy;

    @Column(name = "medium", nullable = false)
    private boolean medium;

    @Column(name = "hard", nullable = false)
    private boolean hard;

    // question types
    @Column(name = "multiple_choice", nullable = false)
    private boolean multipleChoice;

    @Column(name = "open_ended", nullable = false)
    private boolean openEnded;

    @Column(name = "true_false", nullable = false)
    private boolean trueFalse;

    // output content
    @Column(name = "questions", nullable = false)
    private boolean questions;

    @Column(name = "correct_answers", nullable = false)
    private boolean correctAnswers;

    @Column(name = "explanations", nullable = false)
    private boolean explanations;

    @Column(name = "description", nullable = false)
    private boolean description;

}