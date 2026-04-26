import Stepper from "./Stepper";
import UploadBox from "./UploadBox";
import { useState } from "react";

export default function MainContent() {
  const [currentStep, setCurrentStep] = useState(1); // State to track the current step

  /** Function to go to the next step, ensuring it doesn't exceed step 3 */
  const goToNextStep = () => {
    setCurrentStep((prevStep) => Math.min(prevStep + 1, 3));
  };

  return (
    <main className="main-content">
      <section className="content-card">
        <div className="title-section">
          <h1>Create questions from material</h1>
          <p>Upload or paste material and let Eduo create questions based on it</p>
        </div>

        <Stepper currentStep={currentStep} onStepClick={setCurrentStep} />

        <div className="upload-section">
          <UploadBox />

          <div className="actions">
            <button className="continue-button" onClick={goToNextStep}>
              Continue
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}