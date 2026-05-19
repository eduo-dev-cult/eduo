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

    //general settings
    @Column(name = "num_of_questions", nullable = false)
    private int numOfQuestions = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false)
    private GenerationLanguage language = GenerationLanguage.ENGLISH;

    @Enumerated(EnumType.STRING)
    @Column(name = "focus_area", nullable = false)
    private GenerationFocusArea focusArea = GenerationFocusArea.ENTIRE_MATERIAL;

    @Column(name = "topics")
    private String topics;

    // diffuculty settings, with default values (easy = true)
    @Column(name = "default_easy", nullable = false)
    private boolean defaultEasy = true;

    @Column(name = "default_medium", nullable = false)
    private boolean defaultMedium = false;

    @Column(name = "default_hard", nullable = false)
    private boolean defaultHard = false;

    //Default question types (multiple choice = true)
    @Column(name = "default_multiple_choice", nullable = false)
    private boolean defaultMultipleChoice = true;

    @Column(name = "default_open_ended", nullable = false)
    private boolean defaultOpenEnded = false;

    @Column(name = "default_true_false", nullable = false)
    private boolean defaultTrueFalse = false;

    //extra generationoptions, only questions is set as true.
    @Column(name = "default_questions", nullable = false)
    private boolean defaultQuestions = true;

    @Column(name = "default_correct_answers", nullable = false)
    private boolean defaultCorrectAnswers = false;

    @Column(name = "default_explanations", nullable = false)
    private boolean defaultExplanations = false;

    @Column(name = "default_description", nullable = false)
    private boolean defaultDescription = false;

}