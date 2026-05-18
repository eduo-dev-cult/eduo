import "./Stepper.css";

export default function Stepper({ currentStep, onStepClick }) {
  const steps = [
    { number: 1, label: "Add Material" },
    { number: 2, label: "Settings" },
    { number: 3, label: "Preview & Save" },
  ];

  return (
    <div className="stepper">
      {steps.map((step, index) => {
        const isActive = currentStep === step.number;
        const isCompleted = currentStep > step.number;

        return (
          <div className="stepper-item" key={step.number}>
            <button
              type="button"
              className={`step-group ${isActive ? "active" : ""}`}
              onClick={() => onStepClick(step.number)}
            >
              <span
                className={`step ${
                  isActive || isCompleted ? "active-step" : ""
                }`}
              >
                {step.number}
              </span>

              <span
                className={`step-text ${
                  isActive ? "active" : !isCompleted ? "muted" : ""
                }`}
              >
                {step.label}
              </span>
            </button>

            {index < steps.length - 1 && <div className="step-line" />}
          </div>
        );
      })}
    </div>
  );
}