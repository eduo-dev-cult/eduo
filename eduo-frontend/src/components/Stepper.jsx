import "./Stepper.css";

export default function Stepper({ currentStep }) {
  return (
    <div className="stepper">
      
      {/* Step 1 */}
      <div className="step-group">
        <div className={`step ${currentStep === 1 ? "active-step" : ""}`}>
          1
        </div>
        <span className={`step-text ${currentStep === 1 ? "active" : ""}`}>
          Add material
        </span>
      </div>

      <div className="step-line" />

      {/* Step 2 */}
      <div className="step-group">
        <div className={`step ${currentStep === 2 ? "active-step" : ""}`}>
          2
        </div>
        <span
          className={`step-text ${
            currentStep === 2 ? "active" : "muted"
          }`}
        >
          Settings
        </span>
      </div>

      <div className="step-line" />

      {/* Step 3 */}
      <div className="step-group">
        <div className={`step ${currentStep === 3 ? "active-step" : ""}`}>
          3
        </div>
        <span
          className={`step-text ${
            currentStep === 3 ? "active" : "muted"
          }`}
        >
          Preview and Save
        </span>
      </div>

    </div>
  );
}