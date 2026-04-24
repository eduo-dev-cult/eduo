export default function Stepper() {
  return (
    <div className="stepper">
      <div className="step-group">
        <div className="step active-step">1</div>
        <span>Add material</span>
      </div>

      <div className="step-line"></div>

      <div className="step-group muted">
        <div className="step">2</div>
        <span>Settings</span>
      </div>

      <div className="step-line"></div>

      <div className="step-group muted">
        <div className="step">3</div>
        <span>Preview and Save</span>
      </div>
    </div>
  );
}