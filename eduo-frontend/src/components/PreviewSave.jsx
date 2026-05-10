import "./PreviewSave.css";

const exportOptions = [
  { id: "pdf", label: "PDF", icon: "📄" },
  { id: "txt", label: "TXT", icon: "📃" },
  { id: "word", label: "WORD", icon: "📘" },
];

function formatList(values, formatter) {
  if (!values) return "Not selected";

  if (Array.isArray(values)) {
    return values.map(formatter).join(", ");
  }

  return formatter(values);
}

function formatQuestionType(type) {
  if (type === "multipleChoice") return "Multiple Choice";
  if (type === "openEnded") return "Open-Ended";
  if (type === "trueFalse") return "True/False";
  return type;
}

function formatDifficulty(level) {
  if (!level) return "Not selected";
  return level;
}

function formatFocusArea(focusArea) {
  if (focusArea === "entireMaterial") return "Entire Material";
  if (focusArea === "keyConcepts") return "Key Concepts Only";
  if (focusArea === "specificTopics") return "Specific Topics";
  return focusArea ?? "Not selected";
}

function formatOutputContent(outputContent) {
  if (!outputContent) return "Questions";

  const selected = [];

  if (outputContent.questions) selected.push("Questions");
  if (outputContent.correctAnswers) selected.push("Correct Answers");
  if (outputContent.answerExplanations) selected.push("Answer Explanations");

  return selected.join(", ");
}

function formatCollection(collectionId) {
  if (collectionId === "exam-prep") return "Exam Prep";
  if (collectionId === "lecture") return "Lecture Questions";
  return "My Collection";
}

export default function PreviewSave({
  generationResult,
  isGenerating,
  generationError,
  selectedFile,
  settings,
  onRegenerate,
}) {
  const output = generationResult?.output ?? "";
  const generatedFrom = generationResult?.generatedFrom;

  const fileName =
    generatedFrom?.fileName ??
    selectedFile?.name ??
    "source/filename.filetype";

  const handleCopyOutput = async () => {
    if (!output) return;

    try {
      await navigator.clipboard.writeText(output);
    } catch (error) {
      console.error("Could not copy output:", error);
    }
  };

  const handleExportTxt = () => {
    if (!output) return;

    const blob = new Blob([output], { type: "text/plain;charset=utf-8" });
    const url = URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = url;
    link.download = "generated-questions.txt";
    link.click();

    URL.revokeObjectURL(url);
  };

  const handleExport = (fileTypeToExport) => {
    if (fileTypeToExport === "txt") {
      handleExportTxt();
      return;
    }

    console.log(`Export as ${fileTypeToExport} will be connected later.`);
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
              disabled={!output || isGenerating}
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
          <div className="generation-message">Generating questions...</div>
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
            <span className="generated-from-icon">⚙</span>
            <h3>Generated from</h3>
          </div>

          <div className="generated-summary-list">
            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">File</span>
              <span className="summary-value">{fileName}</span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">Question types</span>
              <span className="summary-value">
                {formatList(settings.questionTypes, formatQuestionType)}
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">Difficulty</span>
              <span className="summary-value">
                {formatList(settings.difficulty, formatDifficulty)}
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">Questions</span>
              <span className="summary-value">
                {settings.numberOfQuestions}
              </span>
            </div>

            <div className="generated-summary-item">
              <span className="summary-label">Focus area</span>
              <span className="summary-value">
                {formatFocusArea(settings.focusArea)}
              </span>
            </div>

            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">Output content</span>
              <span className="summary-value">
                {formatOutputContent(settings.outputContent)}
              </span>
            </div>

            <div className="generated-summary-item wide-summary-item">
              <span className="summary-label">Collection</span>
              <span className="summary-value">
                {formatCollection(settings.collectionId)}
              </span>
            </div>
          </div>
        </section>

        <section className="generation-side-card export-card">
          <h3>↥ Export</h3>
          <p>Export the full generated output</p>

          <div className="export-options">
            {exportOptions.map((option) => (
              <button
                key={option.id}
                type="button"
                className="export-option"
                onClick={() => handleExport(option.id)}
                disabled={!output || isGenerating}
              >
                <span className="export-icon">{option.icon}</span>
                <span>{option.label}</span>
              </button>
            ))}
          </div>

          <button
            className="button secondary-button export-file-button"
            onClick={handleExportTxt}
            disabled={!output || isGenerating}
          >
            ⬇ Export file
          </button>
        </section>
      </aside>
    </div>
  );
}