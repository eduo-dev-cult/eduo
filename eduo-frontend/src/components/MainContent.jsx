import { useState } from "react";

import Stepper from "./Stepper";
import UploadBox from "./UploadBox";
import SettingsPanel from "./SettingsPanel";
import PreviewSave from "./PreviewSave";
import "./MainContent.css";

export default function MainContent({ activePage }) {
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedFile, setSelectedFile] = useState(null);

  const [generationSettings, setGenerationSettings] = useState({
    questionTypes: ["multipleChoice"],
    numberOfQuestions: 10,
    collectionId: "default",
    focusArea: "entireMaterial",
    specificTopics: "",
  });

  const getSubtitle = () => {
    switch (currentStep) {
      case 1:
        return "Upload or paste material to generate questions from";
      case 2:
        return "Configure how your questions should be generated";
      case 3:
        return "Preview and save your generated questions";
      default:
        return "";
    }
  };

  const getStepContentClass = () => {
    if (currentStep === 1) return "step-content step-content-start";
    return "step-content step-content-center";
  };

  const goToNextStep = () => {
    if (currentStep === 1 && !selectedFile) return;
    setCurrentStep((prevStep) => Math.min(prevStep + 1, 3));
  };

  const goToPreviousStep = () => {
    setCurrentStep((prevStep) => Math.max(prevStep - 1, 1));
  };

  const handleGenerate = () => {
    const payload = {
      fileName: selectedFile?.name,
      questionTypes: generationSettings.questionTypes,
      numberOfQuestions: Number(generationSettings.numberOfQuestions),
      collectionId: generationSettings.collectionId,
      focusArea: generationSettings.focusArea,
      specificTopics:
        generationSettings.focusArea === "specificTopics"
          ? generationSettings.specificTopics
              .split(",")
              .map((topic) => topic.trim())
              .filter(Boolean)
          : [],
    };

    console.log("Generation payload:", payload);

    setCurrentStep(3);
  };

  const getButtonText = () => {
    if (currentStep === 1) return "Continue";
    if (currentStep === 2) return "Generate";
    return "Save";
  };

  const handleMainButtonClick = () => {
    if (currentStep === 2) {
      handleGenerate();
      return;
    }

    goToNextStep();
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
          <h1>Create questions from material</h1>
          <p>{getSubtitle()}</p>
        </div>

        <Stepper currentStep={currentStep} onStepClick={setCurrentStep} />

        <div className={getStepContentClass()}>
          {currentStep === 1 && (
            <UploadBox selectedFile={selectedFile} onFileSelect={setSelectedFile} />
          )}

          {currentStep === 2 && (
            <SettingsPanel
              settings={generationSettings}
              setSettings={setGenerationSettings}
            />
          )}

          {currentStep === 3 && <PreviewSave settings={generationSettings} />}
        </div>

        <div className="actions">
          {currentStep > 1 && (
            <button className="button secondary-button" onClick={goToPreviousStep}>
              Back
            </button>
          )}

          <button
            className="button primary-button"
            onClick={handleMainButtonClick}
            disabled={currentStep === 1 && !selectedFile}
          >
            {getButtonText()}
          </button>
        </div>
      </section>
    </main>
  );
}