import "./SettingsPanel.css";

export default function SettingsPanel({ settings, setSettings }) {
  const updateSetting = (key, value) => {
    setSettings((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  const toggleQuestionType = (type) => {
    setSettings((prev) => {
      const selected = prev.questionTypes.includes(type);

      const updatedTypes = selected
        ? prev.questionTypes.filter((item) => item !== type)
        : [...prev.questionTypes, type];

      return {
        ...prev,
        questionTypes: updatedTypes.length > 0 ? updatedTypes : prev.questionTypes,
      };
    });
  };

  const toggleDifficulty = (level) => {
    setSettings((prev) => {
      const selected = prev.difficulty.includes(level);

      const updatedDifficulty = selected
        ? prev.difficulty.filter((item) => item !== level)
        : [...prev.difficulty, level];

      return {
        ...prev,
        difficulty:
          updatedDifficulty.length > 0 ? updatedDifficulty : prev.difficulty,
      };
    });
  };

  const toggleOutputContent = (key) => {
    if (key === "questions") return;

    setSettings((prev) => ({
      ...prev,
      outputContent: {
        ...prev.outputContent,
        [key]: !prev.outputContent[key],
        questions: true,
      },
    }));
  };

  return (
    <div className="settings-panel">
      <div className="settings-grid">
        <div className="settings-column">
          <section className="settings-section">
            <h2>1. Question Type</h2>

            <div className="settings-box">
              <p>Choose the types of questions you want to generate</p>

              <div className="question-type-options">
                <button
                  type="button"
                  className={`question-type-card ${
                    settings.questionTypes.includes("multipleChoice")
                      ? "selected"
                      : ""
                  }`}
                  onClick={() => toggleQuestionType("multipleChoice")}
                >
                  <span className="question-type-icon">☰</span>
                  <span>
                    Multiple
                    <br />
                    Choice
                  </span>
                </button>

                <button
                  type="button"
                  className={`question-type-card ${
                    settings.questionTypes.includes("openEnded") ? "selected" : ""
                  }`}
                  onClick={() => toggleQuestionType("openEnded")}
                >
                  <span className="question-type-icon">✎</span>
                  <span>
                    Open-
                    <br />
                    Ended
                  </span>
                </button>

                <button
                  type="button"
                  className={`question-type-card ${
                    settings.questionTypes.includes("trueFalse") ? "selected" : ""
                  }`}
                  onClick={() => toggleQuestionType("trueFalse")}
                >
                  <span className="question-type-icon">✓</span>
                  <span>
                    True/
                    <br />
                    False
                  </span>
                </button>
              </div>
            </div>
          </section>

          <section className="settings-section">
            <h2>2. Number of Questions</h2>

            <div className="settings-box">
              <p>Select how many questions to generate</p>

              <div className="question-counter">
                <button
                  type="button"
                  onClick={() =>
                    updateSetting(
                      "numberOfQuestions",
                      Math.max(1, Number(settings.numberOfQuestions || 1) - 1)
                    )
                  }
                >
                  -
                </button>

                <input
                  className="question-counter-input"
                  type="number"
                  min="1"
                  max="50"
                  value={settings.numberOfQuestions}
                  onChange={(e) => {
                    const value = e.target.value;

                    if (value === "") {
                      updateSetting("numberOfQuestions", "");
                      return;
                    }

                    updateSetting(
                      "numberOfQuestions",
                      Math.min(50, Math.max(1, Number(value)))
                    );
                  }}
                  onBlur={() => {
                    if (settings.numberOfQuestions === "") {
                      updateSetting("numberOfQuestions", 1);
                    }
                  }}
                />

                <button
                  type="button"
                  onClick={() =>
                    updateSetting(
                      "numberOfQuestions",
                      Math.min(50, Number(settings.numberOfQuestions || 1) + 1)
                    )
                  }
                >
                  +
                </button>
              </div>

              <small>Maximum 50 questions</small>
            </div>
          </section>

          <section className="settings-section">
            <h2>3. Difficulty Level</h2>

            <div className="settings-box">
              <p>Choose one or more difficulty levels</p>

              <div className="difficulty-options">
                {["Easy", "Medium", "Hard"].map((level) => (
                  <button
                    key={level}
                    type="button"
                    className={`difficulty-button ${
                      settings.difficulty.includes(level) ? "selected" : ""
                    }`}
                    onClick={() => toggleDifficulty(level)}
                  >
                    {level}
                  </button>
                ))}
              </div>

              <small>At least one difficulty level must be selected</small>
            </div>
          </section>
        </div>

        <div className="settings-column">
          <section className="settings-section">
            <h2>4. Choose Collection</h2>

            <div className="settings-box">
              <p>Choose a Collection to save questions in</p>

              <select
                value={settings.collectionId}
                onChange={(e) => updateSetting("collectionId", e.target.value)}
              >
                <option value="default">My Collection</option>
                <option value="exam-prep">Exam Prep</option>
                <option value="lecture">Lecture Questions</option>
              </select>

              <button className="create-collection-button" type="button">
                + Create new Collection
              </button>
            </div>
          </section>

          <section className="settings-section">
            <h2>5. Focus Area</h2>

            <div className="settings-box">
              <p>Choose what the questions should focus on</p>

              <label className="radio-option">
                <input
                  type="radio"
                  checked={settings.focusArea === "entireMaterial"}
                  onChange={() => updateSetting("focusArea", "entireMaterial")}
                />
                <div>
                  <strong>Entire material</strong>
                  <span>Generate questions from all the material</span>
                </div>
              </label>

              <label className="radio-option">
                <input
                  type="radio"
                  checked={settings.focusArea === "keyConcepts"}
                  onChange={() => updateSetting("focusArea", "keyConcepts")}
                />
                <div>
                  <strong>Key concepts only</strong>
                  <span>Focus on the main concepts and important info</span>
                </div>
              </label>

              <label className="radio-option">
                <input
                  type="radio"
                  checked={settings.focusArea === "specificTopics"}
                  onChange={() => updateSetting("focusArea", "specificTopics")}
                />
                <div>
                  <strong>Specific topics</strong>
                  <span>Enter topics to focus on, separated by comma</span>
                </div>
              </label>

              <input
                className="topics-input"
                type="text"
                placeholder="e.g. recursion, inheritance, algorithms"
                value={settings.specificTopics}
                disabled={settings.focusArea !== "specificTopics"}
                onChange={(e) => updateSetting("specificTopics", e.target.value)}
              />
            </div>
          </section>

          <section className="settings-section">
            <h2>6. Output Content</h2>

            <div className="settings-box output-content-box">
              <p>Choose what the AI should generate</p>

              <label className="checkbox-option">
                <input type="checkbox" checked disabled />
                <span>Questions</span>
              </label>

              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={settings.outputContent.correctAnswers}
                  onChange={() => toggleOutputContent("correctAnswers")}
                />
                <span>Correct Answers</span>
              </label>

              <label className="checkbox-option">
                <input
                  type="checkbox"
                  checked={settings.outputContent.answerExplanations}
                  onChange={() => toggleOutputContent("answerExplanations")}
                />
                <span>Answer Explanations</span>
              </label>

              <small>Questions are always included</small>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}