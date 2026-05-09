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
  return collection?.name ?? collection?.collectionName ?? "Unnamed collection";
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
}) {
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const allowedTypes = [".pdf", ".txt", ".docx", ".pptx"];
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
    if (!fileList || fileList.length === 0) return;

    const newFiles = Array.from(fileList);
    onFilesSelect([...files, ...newFiles]);

    fileInputRef.current.value = "";
  };

  const openFilePicker = () => {
    fileInputRef.current.click();
  };

  const handleFileInputChange = (event) => {
    handleFiles(event.target.files);
  };

  const handleDragOver = (event) => {
    event.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragging(false);
    handleFiles(event.dataTransfer.files);
  };

  const removeFile = (fileIndex) => {
    const updatedFiles = files.filter((_, index) => index !== fileIndex);
    onFilesSelect(updatedFiles);
  };

  const removeAllFiles = () => {
    onFilesSelect([]);
    fileInputRef.current.value = "";
  };

  return (
    <div className="upload-step-layout">
      <div
        className={`upload-box ${isDragging ? "dragging" : ""}`}
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
              {files.length} file{files.length > 1 ? "s" : ""} selected
            </p>

            <ul className="selected-files-list">
              {files.map((file, index) => (
                <li
                  className="selected-file-item"
                  key={`${file.name}-${index}`}
                >
                  <div>
                    <span className="selected-file-name">{file.name}</span>

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
            <p className="upload-title">Drag and drop files here</p>
            <p className="upload-or">or</p>

            <button
              type="button"
              className="select-file-button"
              onClick={openFilePicker}
            >
              Select Files
            </button>
          </>
        )}

        <p className="upload-help">Supports PDF, TXT, DOCX and PPTX</p>
      </div>

      <section className="upload-collection-section">
        <h2>Choose Collection</h2>

        <div className="upload-collection-box">
          <p>Choose a Collection to save questions in</p>

          <select
            value={settings.collectionId || ""}
            onChange={(event) =>
              updateSetting("collectionId", event.target.value)
            }
            disabled={isLoadingCollections || collections.length === 0}
          >
            {isLoadingCollections && (
              <option value="">Loading collections...</option>
            )}

            {!isLoadingCollections && collections.length === 0 && (
              <option value="">No collections available</option>
            )}

            {!isLoadingCollections &&
              collections.map((collection) => {
                const collectionId = getCollectionId(collection);
                const collectionName = getCollectionName(collection);

                return (
                  <option key={collectionId} value={collectionId}>
                    {collectionName}
                  </option>
                );
              })}
          </select>

          {collectionsError && (
            <p className="collection-error-message">
              {collectionsError}
            </p>
          )}

          <button
            className="create-collection-button"
            type="button"
            onClick={onCreateCollection}
          >
            + Create new Collection
          </button>
        </div>
      </section>
    </div>
  );
}