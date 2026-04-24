export default function UploadBox() {
  return (
    <div className="upload-box">
      <div className="file-icon">▯</div>

      <p className="upload-title">Drag and drop file here</p>
      <p className="upload-or">or</p>

      <button className="select-file-button">Select File</button>

      <p className="upload-help">Supports PDF, txt, examples</p>
    </div>
  );
}