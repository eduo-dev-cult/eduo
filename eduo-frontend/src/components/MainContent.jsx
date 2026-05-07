import { useEffect, useState } from "react";

import Stepper from "./Stepper";
import UploadBox from "./UploadBox";
import SettingsPanel from "./SettingsPanel";
import PreviewSave from "./PreviewSave";
import GenerationPreferences from "./GenerationPreferences";
import "./MainContent.css";

const USE_MOCK_GENERATION = true;
const API_BASE_URL = "http://localhost:8080";
const GENERATE_ENDPOINT = `${API_BASE_URL}/api/generations`;

const STORAGE_KEY = "eduo_generation_preferences";

const defaultGenerationSettings = {
  questionTypes: ["multipleChoice"],
  numberOfQuestions: 10,
  collectionId: "default",
  focusArea: "entireMaterial",
  specificTopics: "",
  difficulty: ["Medium"],
  outputContent: {
    questions: true,
    correctAnswers: true,
    answerExplanations: false,
  },
};

function getPreferences() {
  const saved = localStorage.getItem(STORAGE_KEY);

  if (!saved) return defaultGenerationSettings;

  try {
    const parsed = JSON.parse(saved);

    return {
      ...defaultGenerationSettings,
      ...parsed,
      outputContent: {
        ...defaultGenerationSettings.outputContent,
        ...parsed.outputContent,
        questions: true,
      },
    };
  } catch {
    return defaultGenerationSettings;
  }
}

const mockGenerationResult = {
  id: "mock-generation-1",
  output: `Here are 10 multiple choice questions based on the provided material:

---

1. What is the primary function of chlorophyll in plants?

A) To absorb water from the soil  
B) To transport oxygen through the plant  
C) To absorb light energy for photosynthesis  
D) To store glucose in the roots  

Correct answer: C

---

2. Which of the following best describes photosynthesis?

A) The process of breaking down glucose for energy  
B) The conversion of light energy into chemical energy  
C) The absorption of minerals from the soil  
D) The release of oxygen during respiration  

Correct answer: B

---

3. What is the main role of the mitochondria in a cell?

A) Protein synthesis  
B) Energy production (ATP)  
C) DNA storage  
D) Cell division  

Correct answer: B

---

4. Which gas is primarily taken in by plants during photosynthesis?

A) Oxygen  
B) Nitrogen  
C) Carbon dioxide  
D) Hydrogen  

Correct answer: C

---

5. What is the chemical formula for water?

A) CO₂  
B) H₂O  
C) O₂  
D) NaCl  

Correct answer: B

---

6. Which part of the plant is mainly responsible for photosynthesis?

A) Roots  
B) Stem  
C) Leaves  
D) Flowers  

Correct answer: C

---

7. What happens during cellular respiration?

A) Light energy is converted into chemical energy  
B) Glucose is broken down to release energy  
C) Oxygen is produced from carbon dioxide  
D) Water is split into hydrogen and oxygen  

Correct answer: B

---

8. Which of the following is NOT a product of photosynthesis?

A) Oxygen  
B) Glucose  
C) Carbon dioxide  
D) Energy stored in glucose  

Correct answer: C

---

9. What is ATP primarily used for in cells?

A) Storing genetic information  
B) Providing energy for cellular processes  
C) Transporting oxygen  
D) Building cell walls  

Correct answer: B

---

10. Which organelle contains chlorophyll?

A) Nucleus  
B) Mitochondria  
C) Chloroplast  
D) Ribosome  

Correct answer: C`,
  generatedFrom: {
    fileName: "source/filename.filetype",
    fileType: "filetype",
  },
  settings: {
    questionTypes: ["multipleChoice"],
    numberOfQuestions: 10,
    focusArea: "entireMaterial",
    specificTopics: [],
    difficulty: ["Medium"],
    outputContent: {
      questions: true,
      correctAnswers: true,
      answerExplanations: false,
    },
  },
  createdAt: new Date().toISOString(),
};

function normalizeGenerationResponse(data, selectedFile, settings) {
  return {
    id: data.id ?? data.generationId ?? null,
    output: data.output ?? data.generatedOutput ?? data.text ?? "",
    generatedFrom: {
      fileName:
        data.generatedFrom?.fileName ??
        data.fileName ??
        selectedFile?.name ??
        "Unknown source",
      fileType:
        data.generatedFrom?.fileType ??
        data.fileType ??
        selectedFile?.type ??
        "Unknown file type",
    },
    settings: data.settings ?? settings,
    createdAt: data.createdAt ?? new Date().toISOString(),
  };
}

export default function MainContent({ activePage }) {
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedFile, setSelectedFile] = useState(null);

  const [generationSettings, setGenerationSettings] = useState(() =>
    getPreferences()
  );

  const [generationResult, setGenerationResult] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generationError, setGenerationError] = useState("");

  useEffect(() => {
    if (activePage === "generate" && currentStep === 1 && !selectedFile) {
      setGenerationSettings(getPreferences());
    }
  }, [activePage, currentStep, selectedFile]);

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
    if (currentStep === 3) return "step-content step-content-preview";
    return "step-content step-content-center";
  };

  const goToNextStep = () => {
    if (currentStep === 1 && !selectedFile) return;
    setCurrentStep((prevStep) => Math.min(prevStep + 1, 3));
  };

  const goToPreviousStep = () => {
    setCurrentStep((prevStep) => Math.max(prevStep - 1, 1));
  };

  const startNewGeneration = () => {
    setCurrentStep(1);
    setSelectedFile(null);
    setGenerationSettings(getPreferences());
    setGenerationResult(null);
    setGenerationError("");
    setIsGenerating(false);
  };

  const buildGenerationPayload = () => {
    return {
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
      difficulty: generationSettings.difficulty,
      outputContent: {
        ...generationSettings.outputContent,
        questions: true,
      },
    };
  };

  const handleGenerate = async () => {
    setIsGenerating(true);
    setGenerationError("");
    setCurrentStep(3);

    const payload = buildGenerationPayload();

    try {
      if (USE_MOCK_GENERATION) {
        await new Promise((resolve) => setTimeout(resolve, 700));

        setGenerationResult({
          ...mockGenerationResult,
          generatedFrom: {
            fileName:
              selectedFile?.name ?? mockGenerationResult.generatedFrom.fileName,
            fileType:
              selectedFile?.type ||
              selectedFile?.name?.split(".").pop() ||
              "file",
          },
          settings: payload,
        });

        return;
      }

      const formData = new FormData();

      if (selectedFile) {
        formData.append("file", selectedFile);
      }

      formData.append(
        "request",
        new Blob([JSON.stringify(payload)], {
          type: "application/json",
        })
      );

      const response = await fetch(GENERATE_ENDPOINT, {
        method: "POST",
        body: formData,
      });

      if (!response.ok) {
        throw new Error("Could not generate questions.");
      }

      const data = await response.json();

      setGenerationResult(
        normalizeGenerationResponse(data, selectedFile, payload)
      );
    } catch (error) {
      setGenerationError(error.message);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSave = () => {
    const savePayload = {
      generationId: generationResult?.id,
      collectionId: generationSettings.collectionId,
      output: generationResult?.output,
      generatedFrom: generationResult?.generatedFrom,
      settings: generationResult?.settings,
    };

    console.log("Save payload:", savePayload);
  };

  const getButtonText = () => {
    if (currentStep === 1) return "Continue";
    if (currentStep === 2) return isGenerating ? "Generating..." : "Generate";
    return "Save to Collection";
  };

  const handleMainButtonClick = () => {
    if (currentStep === 2) {
      handleGenerate();
      return;
    }

    if (currentStep === 3) {
      handleSave();
      return;
    }

    goToNextStep();
  };

  if (activePage === "preferences") {
    return (
      <main className="main-content">
        <section className="content-card">
          <GenerationPreferences />
        </section>
      </main>
    );
  }

  if (activePage !== "generate") {
    return (
      <main className="main-content">
        <section className="content-card">
          <div className="step-placeholder">
            <h1>
              {activePage === "collections" && "My Collections"}
              {activePage === "material" && "Material"}
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
            <UploadBox
              selectedFile={selectedFile}
              onFileSelect={setSelectedFile}
            />
          )}

          {currentStep === 2 && (
            <SettingsPanel
              settings={generationSettings}
              setSettings={setGenerationSettings}
            />
          )}

          {currentStep === 3 && (
            <PreviewSave
              generationResult={generationResult}
              isGenerating={isGenerating}
              generationError={generationError}
              selectedFile={selectedFile}
              settings={generationSettings}
              setSettings={setGenerationSettings}
              onRegenerate={handleGenerate}
            />
          )}
        </div>

        <div className="actions">
          {currentStep > 1 && (
            <button
              className="button secondary-button"
              onClick={goToPreviousStep}
              disabled={isGenerating}
            >
              Back
            </button>
          )}

          <button
            className="button primary-button"
            onClick={handleMainButtonClick}
            disabled={
              (currentStep === 1 && !selectedFile) ||
              isGenerating ||
              (currentStep === 3 && !generationResult)
            }
          >
            {getButtonText()}
          </button>

          {currentStep === 3 && (
            <button
              className="button primary-button"
              onClick={startNewGeneration}
              disabled={isGenerating}
            >
              Discard
            </button>
          )}
        </div>
      </section>
    </main>
  );
}