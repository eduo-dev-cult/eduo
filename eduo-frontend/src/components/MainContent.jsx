import { useState } from "react";

import Stepper from "./Stepper";
import UploadBox from "./UploadBox";
import SettingsPanel from "./SettingsPanel";
import PreviewSave from "./PreviewSave";

export default function MainContent({ activePage }) {
  const [currentStep, setCurrentStep] = useState(1);

  const goToNextStep = () => {
    setCurrentStep((prevStep) => Math.min(prevStep + 1, 3));
  };

  const getButtonText = () => {
    if (currentStep === 1) return "Continue";
    if (currentStep === 2) return "Generate";
    return "Save";
  };

  if (activePage !== "generate") {
    return (
      <main className="main-content">
        <section className="content-card">
          <div className="step-placeholder">
            <h1>
              {activePage === "collections" && "My Collections"}
              {activePage === "material" && "Material"}
              {activePage === "settings" && "Settings"}
            </h1>
            <p>This page will be implemented later.</p>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="main-content">
      <section className="content-card">
        <div className="title-section">
          <h1>Generate questions from material</h1>
          <p>Upload or paste material and let Eduo create questions based on it</p>
        </div>

        <Stepper currentStep={currentStep} onStepClick={setCurrentStep} />

        <div className="step-content">
          {currentStep === 1 && <UploadBox />}
          {currentStep === 2 && <SettingsPanel />}
          {currentStep === 3 && <PreviewSave />}
        </div>

        <div className="actions">
          <button className="continue-button" onClick={goToNextStep}>
            {getButtonText()}
          </button>
        </div>
      </section>
    </main>
  );
}