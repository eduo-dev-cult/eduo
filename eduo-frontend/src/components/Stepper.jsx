import "./Stepper.css";

export default function Stepper({ currentStep, onStepClick }) {
  const steps = [
    { number: 1, label: "Add material" },
    { number: 2, label: "Settings" },
    { number: 3, label: "Preview and Save" },
  ];

  return (
    <div className="stepper">
      {steps.map((step, index) => {
        const isActive = currentStep === step.number;
        const isClickable = step.number <= currentStep;

        return (
          <div className="stepper-item" key={step.number}>
            <button
              type="button"
              className={`step-group ${isClickable ? "clickable" : ""}`}
              onClick={() => {
                if (isClickable) {
                  onStepClick(step.number);
                }
              }}
              disabled={!isClickable}
            >
              <div className={`step ${isActive ? "active-step" : ""}`}>
                {step.number}
              </div>

              <span
                className={`step-text ${
                  isActive ? "active" : step.number > currentStep ? "muted" : ""
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