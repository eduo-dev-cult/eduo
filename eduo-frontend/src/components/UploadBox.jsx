import { useRef, useState } from "react";
import "./UploadBox.css";

export default function UploadBox({ selectedFile, onFileSelect }) {
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const allowedTypes = [".pdf", ".txt", ".docx", ".pptx"];

  const handleFile = (file) => {
    if (!file) return;
    onFileSelect(file);
  };

  const openFilePicker = () => {
    fileInputRef.current.click();
  };

  const handleFileInputChange = (event) => {
    handleFile(event.target.files[0]);
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
    handleFile(event.dataTransfer.files[0]);
  };

  const removeFile = () => {
    onFileSelect(null);
    fileInputRef.current.value = "";
  };

  return (
    <div
      className={`upload-box ${isDragging ? "dragging" : ""}`}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      <input
        ref={fileInputRef}
        type="file"
        accept={allowedTypes.join(",")}
        className="file-input"
        onChange={handleFileInputChange}
      />

      <div className="file-icon">📄</div>

      {selectedFile ? (
        <>
          <p className="upload-title">{selectedFile.name}</p>
          <p className="upload-or">
            {(selectedFile.size / 1024 / 1024).toFixed(2)} MB selected
          </p>

          <div className="upload-actions">
            <button type="button" className="select-file-button" onClick={openFilePicker}>
              Change File
            </button>

            <button type="button" className="remove-file-button" onClick={removeFile}>
              Remove
            </button>
          </div>
        </>
      ) : (
        <>
          <p className="upload-title">Drag and drop file here</p>
          <p className="upload-or">or</p>

          <button type="button" className="select-file-button" onClick={openFilePicker}>
            Select File
          </button>
        </>
      )}

      <p className="upload-help">Supports PDF, TXT, DOCX and PPTX</p>
    </div>
  );
}