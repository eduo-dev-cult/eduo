import { useRef, useState } from "react";
import "./UploadBox.css";

/*
 * Handles different possible collection id field names.
 */
function getCollectionId(collection) {
  return collection?.id ?? collection?.collectionId;
}

/*
 * Handles different possible collection name field names.
 */
function getCollectionName(collection) {
  return (
    collection?.name ??
    collection?.collectionName ??
    "Unnamed collection"
  );
}

/*
 * Handles different possible backend field names
 * for material IDs.
 */
function getMaterialId(material) {
  return (
    material?.id ??
    material?.materialId ??
    material?.sourceMaterialId
  );
}

/*
 * Handles different possible backend field names
 * for material names.
 */
function getMaterialName(material) {
  return (
    material?.fileName ??
    material?.filename ??
    material?.name ??
    "Unnamed material"
  );
}

export default function UploadBox({
  selectedFiles,
  onFilesSelect,
  settings,
  setSettings,
  collections,
  isLoadingCollections,
  collectionsError,
  onCreateCollection,

  /*
   * Props related to previously uploaded materials
   * in the selected collection.
   *
   * Used for:
   * - displaying existing materials
   * - selecting materials for generation
   * - handling loading and error states
   */
    uploadStatus,
    existingMaterials,
    selectedExistingMaterialIds,
    setSelectedExistingMaterialIds,
    isLoadingExistingMaterials,
    existingMaterialsError,
  }) {
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  /*
   * File types the upload input accepts.
   */
  const allowedTypes = [".pdf", ".txt", ".docx", ".pptx"];

  /*
   * Makes sure selectedFiles is always treated as an array.
   */
  const files = selectedFiles || [];

  /*
   * Updates one field in the generation settings object.
   */
  const updateSetting = (key, value) => {
    setSettings((prev) => ({
      ...prev,
      [key]: value,
    }));
  };

  /*
   * Adds selected or dropped files to the current file list.
   */
  const handleFiles = (fileList) => {
    if (!fileList || fileList.length === 0) {
      return;
    }

    const newFiles = Array.from(fileList);

    /*
     * Keep already selected files and add new ones.
     */
    onFilesSelect([...files, ...newFiles]);

    /*
     * Reset input so the same file can be selected again later.
     */
    fileInputRef.current.value = "";
  };

  /*
   * Opens the hidden file input.
   */
  const openFilePicker = () => {
    fileInputRef.current.click();
  };

  /*
   * Handles files selected through the file picker.
   */
  const handleFileInputChange = (event) => {
    handleFiles(event.target.files);
  };

  /*
   * Enables drag styling while dragging files over the box.
   */
  const handleDragOver = (event) => {
    event.preventDefault();
    setIsDragging(true);
  };

  /*
   * Removes drag styling when leaving the upload box.
   */
  const handleDragLeave = () => {
    setIsDragging(false);
  };

  /*
   * Handles dropped files.
   */
  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragging(false);

    handleFiles(event.dataTransfer.files);
  };

  /*
   * Removes one selected file.
   */
  const removeFile = (fileIndex) => {
    const updatedFiles = files.filter(
      (_, index) => index !== fileIndex
    );

    onFilesSelect(updatedFiles);
  };

  /*
   * Removes all selected files.
   */
  const removeAllFiles = () => {
    onFilesSelect([]);

    fileInputRef.current.value = "";
  };

  /*
   * Only show upload feedback when there is
   * an actual status message to show.
   */
  const shouldShowUploadStatus =
    uploadStatus?.type && uploadStatus?.message;

  /*
   * Toggles selection of an existing material.
   */
  const toggleExistingMaterial =
    (materialId) => {

      setSelectedExistingMaterialIds(
        (prevSelected) =>

          prevSelected.includes(materialId)

            /*
            * Remove if already selected.
            */
            ? prevSelected.filter(
                (id) => id !== materialId
              )

            /*
            * Add if not selected.
            */
            : [
                ...prevSelected,
                materialId,
              ]
      );
    };

  return (
    <div className="upload-step-layout">
      {/* Collection selector at the top because it controls the material list below. */}
      <section className="collection-top-section">
        <div>
          <h2>Choose Collection</h2>

          <p>
            Choose a collection to see previously uploaded material and save your questions.
          </p>
        </div>

        <select
          value={settings.collectionId || ""}
          onChange={(event) =>
            updateSetting("collectionId", event.target.value)
          }
          disabled={
            isLoadingCollections ||
            collections.length === 0
          }
        >
          {collections.map((collection) => {
            const collectionId =
              getCollectionId(collection);

            const collectionName =
              getCollectionName(collection);

            return (
              <option
                key={collectionId}
                value={collectionId}
              >
                {collectionName}
              </option>
            );
          })}
        </select>

        <button
          className="create-collection-button"
          type="button"
          onClick={onCreateCollection}
        >
          + Create new Collection
        </button>
      </section>

      {collectionsError && (
        <p className="collection-error-message">
          {collectionsError}
        </p>
      )}

      <div className="material-selection-grid">
        <section className="upload-panel">
          <div
            className={`upload-box ${
              isDragging ? "dragging" : ""
            }`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept={allowedTypes.join(",")}
              className="file-input"
              onChange={handleFileInputChange}
            />

            <div className="file-icon">📄</div>

            {files.length > 0 ? (
              <>
                <p className="upload-title">
                  {files.length} file
                  {files.length > 1 ? "s" : ""} selected
                </p>

                <ul className="selected-files-list">
                  {files.map((file, index) => (
                    <li
                      className="selected-file-item"
                      key={`${file.name}-${index}`}
                    >
                      <div>
                        <span className="selected-file-name">
                          {file.name}
                        </span>

                        <span className="selected-file-size">
                          {(file.size / 1024 / 1024).toFixed(2)} MB
                        </span>
                      </div>

                      <button
                        type="button"
                        className="remove-single-file-button"
                        onClick={() => removeFile(index)}
                      >
                        Remove
                      </button>
                    </li>
                  ))}
                </ul>

                <div className="upload-actions">
                  <button
                    type="button"
                    className="select-file-button"
                    onClick={openFilePicker}
                  >
                    Add Files
                  </button>

                  <button
                    type="button"
                    className="remove-file-button"
                    onClick={removeAllFiles}
                  >
                    Remove All
                  </button>
                </div>
              </>
            ) : (
              <>
                <p className="upload-title">
                  Drag and drop files here
                </p>

                <p className="upload-or">or</p>

                <button
                  type="button"
                  className="select-file-button"
                  onClick={openFilePicker}
                >
                  Select Files
                </button>

                <p className="upload-help">
                  Accepted formats: PDF, TXT, DOCX, PPTX
                </p>
              </>
            )}

            {shouldShowUploadStatus && (
              <p
                className={`upload-status-message upload-status-${uploadStatus.type}`}
              >
                {uploadStatus.message}
              </p>
            )}
          </div>
        </section>

        <section className="existing-material-panel">
          <h2>
            Previously uploaded material in this collection
          </h2>

          <p>
            Select material to include in the generation.
          </p>

          {isLoadingExistingMaterials && (
            <p className="existing-material-muted">
              Loading materials...
            </p>
          )}

          {existingMaterialsError && (
            <p className="collection-error-message">
              {existingMaterialsError}
            </p>
          )}

          {!isLoadingExistingMaterials &&
            existingMaterials.length === 0 && (
              <p className="existing-material-muted">
                No material uploaded in this collection yet.
              </p>
            )}

          <div className="existing-material-list">
            {existingMaterials.map((material) => {
              const materialId =
                getMaterialId(material);

              const isSelected =
                selectedExistingMaterialIds.includes(
                  materialId
                );

              return (
                <label
                  className="existing-material-item"
                  key={materialId}
                >
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() =>
                      toggleExistingMaterial(materialId)
                    }
                  />

                  <span className="existing-material-icon">
                    📄
                  </span>

                  <span className="existing-material-name">
                    {getMaterialName(material)}
                  </span>
                </label>
              );
            })}
          </div>

          <p className="existing-material-count">
            {selectedExistingMaterialIds.length} items selected
          </p>
        </section>
      </div>
    </div>
  );
}