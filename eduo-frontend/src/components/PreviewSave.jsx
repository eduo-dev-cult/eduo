import "./PreviewSave.css";

/*
 * Export options shown in the export card.
 * Only TXT export is connected right now.
 * PDF and WORD can be connected later.
 */
const exportOptions = [
  { id: "pdf", label: "PDF", icon: "📄" },
  { id: "txt", label: "TXT", icon: "📃" },
  { id: "word", label: "WORD", icon: "📘" },
];

/*
 * Formats arrays or single values into readable text.
 */
function formatList(values, formatter) {
  if (!values) return "Not selected";

  if (Array.isArray(values)) {
    return values.map(formatter).join(", ");
  }

  return formatter(values);
}

/*
 * Makes question type values easier to read in the UI.
 */
function formatQuestionType(type) {
  if (type === "multipleChoice") return "Multiple Choice";
  if (type === "openEnded") return "Open-Ended";
  if (type === "trueFalse") return "True/False";

  return type;
}

/*
 * Makes difficulty values easier to read in the UI.
 */
function formatDifficulty(level) {
  if (!level) return "Not selected";

  return level;
}

/*
 * Makes focus area values easier to read in the UI.
 */
function formatFocusArea(focusArea) {
  if (focusArea === "entireMaterial") {
    return "Entire Material";
  }

  if (focusArea === "keyConcepts") {
    return "Key Concepts Only";
  }

  if (focusArea === "specificTopics") {
    return "Specific Topics";
  }

  return focusArea ?? "Not selected";
}

/*
 * Formats the selected output content options.
 */
function formatOutputContent(outputContent) {
  if (!outputContent) return "Questions";

  const selected = [];

  if (outputContent.questions) {
    selected.push("Questions");
  }

  if (outputContent.correctAnswers) {
    selected.push("Correct Answers");
  }

  if (outputContent.answerExplanations) {
    selected.push("Answer Explanations");
  }

  return selected.join(", ");
}

/*
 * Formats the selected language.
 * Makes first letter uppercase.
 */
function formatLanguage(language) {
  if (!language) return "Not selected";

  return (
    language.charAt(0).toUpperCase() +
    language.slice(1)
  );
}

/*
 * Temporary collection formatter.
 *
 * This can later be replaced by passing the full
 * collection object/name from MainContent.
 */
function formatCollection(collectionId) {
  if (collectionId === "exam-prep") {
    return "Exam Prep";
  }

  if (collectionId === "lecture") {
    return "Lecture Questions";
  }

  return "My Collection";
}

export default function PreviewSave({
  generationResult,
  isGenerating,
  generationError,
  selectedFiles,
  settings,
  onRegenerate,
}) {
  /*
   * The generated backend output shown in the textarea.
   */
  const output = generationResult?.output ?? "";

  /*
   * Metadata about what files/materials the generation used.
   */
  const generatedFrom =
    generationResult?.generatedFrom;

  /*
   * Prefer backend/generated metadata.
   * Fall back to selected browser files.
   */
  const fileNames =
    generatedFrom?.fileNames ??
    selectedFiles?.map((file) => file.name) ??
    [generatedFrom?.fileName ?? "source/filename.filetype"];

  /*
   * Copies the generated output to clipboard.
   */
  const handleCopyOutput = async () => {
    if (!output) return;

    try {
      await navigator.clipboard.writeText(output);
    } catch (error) {
      console.error(
        "Could not copy output:",
        error
      );
    }
  };

  /*
   * Exports the generated output as a .txt file.
   */
  const handleExportTxt = () => {
    if (!output) return;

    const blob = new Blob([output], {
      type: "text/plain;charset=utf-8",
    });

    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");

    link.href = url;
    link.download =
      "generated-questions.txt";

    link.click();

    URL.revokeObjectURL(url);
  };

  /*
   * Routes export clicks.
   * TXT works now; PDF/WORD can be added later.
   */
  const handleExport = (
    fileTypeToExport
  ) => {
    if (fileTypeToExport === "txt") {
      handleExportTxt();
      return;
    }

    console.log(
      `Export as ${fileTypeToExport} will be connected later.`
    );
  };

  return (
    <div className="preview-save">
      <section className="generated-output-card">
        <div className="preview-card-header">
          <h2>Generated Output (raw)</h2>

          <div className="preview-header-actions">
            <button
              className="button secondary-button compact-button"
              onClick={onRegenerate}
              disabled={isGenerating}
            >
              ↻ Regenerate
            </button>

            <button
              className="button secondary-button compact-button"
              onClick={handleCopyOutput}
              disabled={
                !output || isGenerating
              }
            >
              ⧉ Copy Output
            </button>
          </div>
        </div>

        {generationError && (
          <div className="generation-message error-message">
            {generationError}
          </div>
        )}

        {isGenerating && (
          <div className="generation-message">
            Generating questions...
          </div>
        )}

        <textarea
          className="generated-output-textarea"
          value={output}
          readOnly
          placeholder="The generated output from the backend will appear here."
        />
      </section>

      <aside className="generation-info-panel">
        <section className="generation-side-card generated-from-card">
          <div className="generated-from-header">
            <span className="generated-from-icon">
              ⚙
            </span>

            <h3>Generated from</h3>
          </div>

          <div className="generated-summary-list">
            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">
                File
                {fileNames.length > 1
                  ? "s"
                  : ""}
              </span>

              <span className="summary-value">
                {fileNames.join(", ")}
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">
                Question types
              </span>

              <span className="summary-value">
                {formatList(
                  settings.questionTypes,
                  formatQuestionType
                )}
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">
                Difficulty
              </span>

              <span className="summary-value">
                {formatList(
                  settings.difficulty,
                  formatDifficulty
                )}
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">
                Questions
              </span>

              <span className="summary-value">
                {
                  settings.numberOfQuestions
                }
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">
                Language
              </span>

              <span className="summary-value">
                {formatLanguage(
                  settings.language
                )}
              </span>
            </div>

            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">
                Focus area
              </span>

              <span className="summary-value">
                {formatFocusArea(
                  settings.focusArea
                )}
              </span>
            </div>

            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">
                Output content
              </span>

              <span className="summary-value">
                {formatOutputContent(
                  settings.outputContent
                )}
              </span>
            </div>

            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">
                Collection
              </span>

              <span className="summary-value">
                {formatCollection(
                  settings.collectionId
                )}
              </span>
            </div>
          </div>
        </section>

        <section className="generation-side-card export-card">
          <h3>↥ Export</h3>

          <p>
            Export the full generated
            output
          </p>

          <div className="export-options">
            {exportOptions.map(
              (option) => (
                <button
                  key={option.id}
                  type="button"
                  className="export-option"
                  onClick={() =>
                    handleExport(
                      option.id
                    )
                  }
                  disabled={
                    !output ||
                    isGenerating
                  }
                >
                  <span className="export-icon">
                    {option.icon}
                  </span>

                  <span>
                    {option.label}
                  </span>
                </button>
              )
            )}
          </div>

          <button
            className="button secondary-button export-file-button"
            onClick={handleExportTxt}
            disabled={
              !output || isGenerating
            }
          >
            ⬇ Export file
          </button>
        </section>
      </aside>
    </div>
  );
}