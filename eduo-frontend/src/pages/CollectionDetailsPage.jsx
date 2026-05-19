import { useEffect, useState } from "react";

import { uploadMaterials } from "../api/materialsApi";

import "./CollectionDetailsPage.css";

function formatDate(dateValue) {
  if (!dateValue) {
    return "Unknown";
  }

  return new Date(dateValue).toLocaleDateString("sv-SE", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function formatFileSize(sizeValue) {
  if (!sizeValue) {
    return "Unknown";
  }

  const sizeInBytes = Number(sizeValue);

  if (Number.isNaN(sizeInBytes)) {
    return "Unknown";
  }

  if (sizeInBytes < 1024) {
    return `${sizeInBytes} B`;
  }

  if (sizeInBytes < 1024 * 1024) {
    return `${(sizeInBytes / 1024).toFixed(1)} KB`;
  }

  return `${(sizeInBytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatFocusArea(focusArea) {
  if (focusArea === "ENTIRE_MATERIAL") {
    return "Entire material";
  }

  if (focusArea === "KEY_CONCEPTS") {
    return "Key concepts";
  }

  if (focusArea === "TOPICS") {
    return "Specific topics";
  }

  return focusArea || "Unknown focus area";
}

function getDifficulty(generation) {
  return [
    generation.easy ? "Easy" : null,
    generation.medium ? "Medium" : null,
    generation.hard ? "Hard" : null,
  ].filter(Boolean);
}

function getQuestionTypes(generation) {
  return [
    generation.multipleChoice ? "Multiple choice" : null,
    generation.openEnded ? "Open ended" : null,
    generation.trueFalse ? "True/False" : null,
  ].filter(Boolean);
}

export default function CollectionDetailsPage({
  collection,
  openUploadModalOnMount,
  onUploadModalOpened,
  onBack,
  onOpenGenerationPreview,
  onUseMaterialForGeneration,
  onGenerateFromCollection,
}) {
  const [
    selectedGeneration,
    setSelectedGeneration,
  ] = useState(null);

  const [
    selectedMaterial,
    setSelectedMaterial,
  ] = useState(null);

  const [
    isUploadModalOpen,
    setIsUploadModalOpen,
  ] = useState(false);

  const [uploadFiles, setUploadFiles] =
    useState([]);

  const [isUploading, setIsUploading] =
    useState(false);

  const [uploadError, setUploadError] =
    useState("");

  const [
    collectionMaterials,
    setCollectionMaterials,
  ] = useState([]);

  useEffect(() => {
    setCollectionMaterials(
      Array.isArray(collection?.sourceMaterials)
        ? collection.sourceMaterials
        : []
    );
  }, [collection]);

  useEffect(() => {
    if (openUploadModalOnMount) {
      setIsUploadModalOpen(true);
      onUploadModalOpened?.();
    }
  }, [openUploadModalOnMount, onUploadModalOpened]);

  if (!collection) {
    return (
      <main className="collection-details-page">
        <p>No collection selected.</p>

        <button
          className="secondary-button"
          onClick={onBack}
        >
          Back to Collections
        </button>
      </main>
    );
  }

  const sourceMaterials = collectionMaterials;

  const generations =
    Array.isArray(collection.generations)
      ? collection.generations
      : [];

  const updatedDate = formatDate(collection.updatedAt);

  const handleUploadMaterials = async () => {
    if (uploadFiles.length === 0) {
      return;
    }

    try {
      setIsUploading(true);
      setUploadError("");

      const uploadedMaterials =
        await uploadMaterials(
          collection.id,
          uploadFiles
        );

      setCollectionMaterials((previousMaterials) => [
        ...previousMaterials,
        ...uploadedMaterials,
      ]);

      setUploadFiles([]);
      setIsUploadModalOpen(false);
    } catch (error) {
      console.error(
        "Failed to upload materials:",
        error
      );

      setUploadError(error.message);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <main className="collection-details-page">
      <button
        className="collection-back-button"
        onClick={onBack}
      >
        ← Back to Collections
      </button>

      {/* Header */}
      <section className="collection-details-header">
        <div>
          <h1>{collection.name}</h1>

          <p>
            {collection.description ||
              "No description added yet."}
          </p>
        </div>
      </section>

      {/* Overview cards */}
      <section className="collection-overview-grid">
        <div className="collection-overview-card">
          <h3>Uploaded Files</h3>
          <span>{sourceMaterials.length}</span>
        </div>

        <div className="collection-overview-card">
          <h3>Generations</h3>
          <span>{generations.length}</span>
        </div>

        <div className="collection-overview-card">
          <h3>Last Updated</h3>
          <span>{updatedDate}</span>
        </div>
      </section>

      {/* Main content grid */}
      <div className="collection-details-content-grid">
        {/* Uploaded material */}
        <section className="collection-section">
          <div className="collection-section-header">
            <h2>Uploaded Material</h2>

            <button
              className="primary-button"
              onClick={() =>
                setIsUploadModalOpen(true)
              }
            >
              Upload
            </button>
          </div>

          {sourceMaterials.length === 0 ? (
            <p className="collection-empty-text">
              No material uploaded yet.
            </p>
          ) : (
            <div className="collection-list">
              {sourceMaterials.map((material) => (
                <button
                  key={material.id}
                  type="button"
                  className="collection-list-item material-list-item"
                  onClick={() =>
                    setSelectedMaterial(material)
                  }
                >
                  <div>
                    <h3>{material.filename}</h3>
                    <p>{material.fileType}</p>
                  </div>
                </button>
              ))}
            </div>
          )}
        </section>

        {/* Generations */}
        <section className="collection-section">
          <div className="collection-section-header">
            <h2>Generations</h2>

            <button
                className="primary-button"
                onClick={() =>
                    onGenerateFromCollection(
                    collection)
                }
                >
                Generate
                </button>
          </div>

          {generations.length === 0 ? (
            <p className="collection-empty-text">
              No generations yet.
            </p>
          ) : (
            <div className="collection-list">
              {generations.map((generation) => {
                const difficulty =
                  getDifficulty(generation);

                const questionTypes =
                  getQuestionTypes(generation);

                return (
                  <button
                    key={generation.id}
                    type="button"
                    className="collection-list-item generation-list-item"
                    onClick={() =>
                      setSelectedGeneration(generation)
                    }
                  >
                    <div>
                      <h3>
                        {generation.numOfQuestions} questions
                      </h3>

                      <p>
                        {generation.language ||
                          "Unknown language"}{" "}
                        ·{" "}
                        {formatFocusArea(
                          generation.focusArea
                        )}
                      </p>

                      <p>
                        Difficulty:{" "}
                        {difficulty.length > 0
                          ? difficulty.join(", ")
                          : "Unknown"}
                      </p>

                      <p>
                        Types:{" "}
                        {questionTypes.length > 0
                          ? questionTypes.join(", ")
                          : "Unknown"}
                      </p>

                      <p>
                        Created:{" "}
                        {formatDate(
                          generation.createdAt
                        )}
                      </p>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </section>
      </div>

      {/* Upload modal */}
      {isUploadModalOpen && (
        <div className="generation-modal-backdrop">
          <div className="generation-modal">
            <div className="generation-modal-header">
              <h2>Upload Material</h2>

              <button
                type="button"
                className="generation-modal-close"
                onClick={() => {
                  setIsUploadModalOpen(false);
                  setUploadFiles([]);
                  setUploadError("");
                }}
              >
                ×
              </button>
            </div>

            <input
              type="file"
              multiple
              onChange={(event) =>
                setUploadFiles(
                  Array.from(
                    event.target.files || []
                  )
                )
              }
            />

            {uploadFiles.length > 0 && (
              <div className="generation-questions-preview">
                <h3>Selected files</h3>

                <pre>
                  {uploadFiles
                    .map((file) => file.name)
                    .join("\n")}
                </pre>
              </div>
            )}

            {uploadError && (
              <p className="collection-empty-text">
                {uploadError}
              </p>
            )}

            <div className="generation-modal-actions">
              <button
                className="secondary-button"
                onClick={() => {
                  setIsUploadModalOpen(false);
                  setUploadFiles([]);
                  setUploadError("");
                }}
              >
                Cancel
              </button>

              <button
                className="primary-button"
                onClick={handleUploadMaterials}
                disabled={
                  uploadFiles.length === 0 ||
                  isUploading
                }
              >
                {isUploading
                  ? "Uploading..."
                  : "Upload"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Material modal */}
      {selectedMaterial && (
        <div className="generation-modal-backdrop">
          <div className="generation-modal">
            <div className="generation-modal-header">
              <h2>Material Details</h2>

              <button
                type="button"
                className="generation-modal-close"
                onClick={() =>
                  setSelectedMaterial(null)
                }
              >
                ×
              </button>
            </div>

            <div className="generation-details-grid">
              <span>Filename</span>
              <p>
                {selectedMaterial.filename ||
                  selectedMaterial.fileName ||
                  "Unknown file"}
              </p>

              <span>File type</span>
              <p>
                {selectedMaterial.fileType ||
                  "Unknown"}
              </p>

              <span>Size</span>
              <p>
                {formatFileSize(
                  selectedMaterial.fileSizeBytes ||
                    selectedMaterial.size
                )}
              </p>

              <span>Uploaded</span>
              <p>
                {formatDate(
                  selectedMaterial.uploadedAt ||
                    selectedMaterial.createdAt
                )}
              </p>
            </div>

            <div className="generation-modal-actions">
              <a
                href={`http://localhost:8080/collections/${collection.id}/materials/${selectedMaterial.id}`}
                className="secondary-button material-download-link"
              >
                Download
              </a>

              <button
                className="primary-button"
                onClick={() =>
                  onUseMaterialForGeneration(
                    selectedMaterial
                  )
                }
              >
                Use for Generation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Generation modal */}
      {selectedGeneration && (
        <div className="generation-modal-backdrop">
          <div className="generation-modal">
            <div className="generation-modal-header">
              <h2>Generation Details</h2>

              <button
                type="button"
                className="generation-modal-close"
                onClick={() =>
                  setSelectedGeneration(null)
                }
              >
                ×
              </button>
            </div>

            <div className="generation-modal-actions">
              <button
                className="primary-button"
                onClick={() =>
                  onOpenGenerationPreview(
                    selectedGeneration
                  )
                }
              >
                Open Full Preview
              </button>
            </div>

            <div className="generation-details-grid">
              <span>Questions</span>
              <p>{selectedGeneration.numOfQuestions}</p>

              <span>Language</span>
              <p>{selectedGeneration.language}</p>

              <span>Focus area</span>
              <p>
                {formatFocusArea(
                  selectedGeneration.focusArea
                )}
              </p>

              <span>Topics</span>
              <p>
                {selectedGeneration.topics ||
                  "No specific topics"}
              </p>

              <span>Difficulty</span>
              <p>
                {getDifficulty(
                  selectedGeneration
                ).join(", ") || "Unknown"}
              </p>

              <span>Question types</span>
              <p>
                {getQuestionTypes(
                  selectedGeneration
                ).join(", ") || "Unknown"}
              </p>

              <span>Created</span>
              <p>
                {formatDate(
                  selectedGeneration.createdAt
                )}
              </p>

              <span>Updated</span>
              <p>
                {formatDate(
                  selectedGeneration.updatedAt
                )}
              </p>
            </div>

            {selectedGeneration.quiz?.rawContent && (
              <div className="generation-questions-preview">
                <h3>Generated Questions</h3>

                <pre>
                  {selectedGeneration.quiz.rawContent}
                </pre>
              </div>
            )}
          </div>
        </div>
      )}
    </main>
  );
}