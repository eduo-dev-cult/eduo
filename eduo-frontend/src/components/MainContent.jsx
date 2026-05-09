import { useEffect, useState } from "react";
import {
  getCollections,
  createCollection,
} from "../api/collectionsApi";

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
  collectionId: "",
  focusArea: "entireMaterial",
  specificTopics: "",
  difficulty: ["Medium"],

  outputContent: {
    questions: true,
    correctAnswers: true,
    answerExplanations: false,
  },
};

/*
 * Handles different possible collection id field names.
 * This makes the frontend safer if backend DTO names change slightly.
 */
function getCollectionId(collection) {
  return collection?.id ?? collection?.collectionId;
}

/*
 * Handles different possible collection name field names.
 */
function getCollectionName(collection) {
  return collection?.name ?? collection?.collectionName ?? "Unnamed collection";
}

/*
 * Handles different possible user id field names.
 */
function getUserId(user) {
  return user?.id ?? user?.userId;
}

/*
 * Loads saved generation preferences from localStorage.
 */
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

        // Questions should always be included.
        questions: true,
      },
    };
  } catch {
    return defaultGenerationSettings;
  }
}

/*
 * Temporary mock result while generation backend is not fully connected.
 */
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

2. Which gas do plants take in during photosynthesis?

A) Oxygen
B) Carbon dioxide
C) Nitrogen
D) Hydrogen

Correct answer: B

---

3. What is produced as a by-product of photosynthesis?

A) Oxygen
B) Carbon dioxide
C) Salt
D) Protein

Correct answer: A

---

4. Where in the plant cell does photosynthesis mainly take place?

A) Nucleus
B) Mitochondria
C) Chloroplasts
D) Cell membrane

Correct answer: C

---

5. What are the main inputs needed for photosynthesis?

A) Oxygen, glucose, and soil
B) Carbon dioxide, water, and light energy
C) Protein, oxygen, and minerals
D) Nitrogen, glucose, and darkness

Correct answer: B

---

6. What is glucose used for in plants?

A) As an energy source and building material
B) To absorb sunlight directly
C) To remove oxygen from the air
D) As a replacement for water

Correct answer: A

---

7. Why is sunlight important in photosynthesis?

A) It keeps the plant warm enough to grow
B) It provides the energy needed to make glucose
C) It helps the roots absorb minerals
D) It turns oxygen into carbon dioxide

Correct answer: B

---

8. Which part of the plant usually absorbs most sunlight?

A) Roots
B) Stem
C) Leaves
D) Flowers

Correct answer: C

---

9. Why is photosynthesis important for animals and humans?

A) It removes all water from the environment
B) It produces oxygen and forms the base of many food chains
C) It prevents plants from growing too quickly
D) It creates minerals in the soil

Correct answer: B

---

10. What happens to water during photosynthesis?

A) It is used together with carbon dioxide to help produce glucose
B) It is changed directly into soil
C) It blocks sunlight from entering the leaf
D) It is released as the main energy source

Correct answer: A`,

  generatedFrom: {
    fileName: "source/filename.filetype",
    fileType: "filetype",
  },

  settings: defaultGenerationSettings,

  createdAt: new Date().toISOString(),
};

/*
 * Normalizes backend generation responses into the structure
 * PreviewSave expects.
 */
function normalizeGenerationResponse(data, selectedFiles, settings) {
  const firstFile = selectedFiles[0];

  return {
    id: data.id ?? data.generationId ?? null,

    output:
      data.output ??
      data.generatedOutput ??
      data.text ??
      "",

    generatedFrom: {
      fileName:
        data.generatedFrom?.fileName ??
        data.fileName ??
        firstFile?.name ??
        "Unknown source",

      fileType:
        data.generatedFrom?.fileType ??
        data.fileType ??
        firstFile?.type ??
        "Unknown file type",

      fileNames: selectedFiles.map((file) => file.name),
    },

    settings: data.settings ?? settings,

    createdAt:
      data.createdAt ??
      new Date().toISOString(),
  };
}

export default function MainContent({
  activePage,
  currentUser,
}) {
  const [currentStep, setCurrentStep] = useState(1);
  const [selectedFiles, setSelectedFiles] = useState([]);

  // Collections belonging to the currently logged-in user.
  const [collections, setCollections] = useState([]);
  const [isLoadingCollections, setIsLoadingCollections] = useState(false);
  const [collectionsError, setCollectionsError] = useState("");

  const [generationSettings, setGenerationSettings] =
    useState(() => getPreferences());

  const [generationResult, setGenerationResult] = useState(null);
  const [isGenerating, setIsGenerating] = useState(false);
  const [generationError, setGenerationError] = useState("");

  /*
   * Loads collections for the logged-in user.
   * This is reused both on startup and after creating a new collection.
   */
  const loadCollections = async (userId) => {
    try {
      setIsLoadingCollections(true);
      setCollectionsError("");

      const loadedCollections = await getCollections(userId);

      console.log("Loaded collections:", loadedCollections);

      setCollections(loadedCollections);

      if (loadedCollections.length > 0) {
        const firstCollectionId = getCollectionId(loadedCollections[0]);

        setGenerationSettings((prevSettings) => ({
          ...prevSettings,
          collectionId:
            prevSettings.collectionId ||
            firstCollectionId ||
            "",
        }));
      }
    } catch (error) {
      console.error("Failed to load collections:", error);
      setCollectionsError(error.message);
    } finally {
      setIsLoadingCollections(false);
    }
  };

  /*
   * Loads collections when currentUser becomes available from App.jsx.
   */
  useEffect(() => {
    const userId = getUserId(currentUser);

    console.log("Current user in MainContent:", currentUser);

    if (!userId) return;

    loadCollections(userId);
  }, [currentUser]);

  /*
   * Creates a new collection for the current user.
   * For now this uses prompt, which is simple for the prototype.
   */
  const handleCreateCollection = async () => {
    const userId = getUserId(currentUser);

    if (!userId) {
      setCollectionsError("No logged-in user found.");
      return;
    }

    const name = window.prompt("Collection name:");

    if (!name || name.trim() === "") {
      return;
    }

    try {
      setIsLoadingCollections(true);
      setCollectionsError("");

      const newCollection = await createCollection({
        userId,
        name: name.trim(),
      });

      const newCollectionId = getCollectionId(newCollection);

      setCollections((prevCollections) => [
        ...prevCollections,
        newCollection,
      ]);

      setGenerationSettings((prevSettings) => ({
        ...prevSettings,
        collectionId: newCollectionId,
      }));
    } catch (error) {
      console.error("Failed to create collection:", error);
      setCollectionsError(error.message);
    } finally {
      setIsLoadingCollections(false);
    }
  };

  /*
   * Resets generation settings when returning to step 1
   * without selected files.
   */
  useEffect(() => {
    if (
      activePage === "generate" &&
      currentStep === 1 &&
      selectedFiles.length === 0
    ) {
      setGenerationSettings((prevSettings) => ({
        ...getPreferences(),

        // Keep selected collection when resetting other preferences.
        collectionId: prevSettings.collectionId,
      }));
    }
  }, [
    activePage,
    currentStep,
    selectedFiles.length,
  ]);

  const getSubtitle = () => {
    switch (currentStep) {
      case 1:
        return "Upload material and choose where the questions should be saved";
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
    if (currentStep === 1 && selectedFiles.length === 0) return;

    setCurrentStep((prevStep) =>
      Math.min(prevStep + 1, 3)
    );
  };

  const goToPreviousStep = () => {
    setCurrentStep((prevStep) =>
      Math.max(prevStep - 1, 1)
    );
  };

  const startNewGeneration = () => {
    setCurrentStep(1);
    setSelectedFiles([]);

    setGenerationSettings((prevSettings) => ({
      ...getPreferences(),
      collectionId: prevSettings.collectionId,
    }));

    setGenerationResult(null);
    setGenerationError("");
    setIsGenerating(false);
  };

  /*
   * Builds the generation request payload.
   */
  const buildGenerationPayload = () => {
    return {
      fileNames: selectedFiles.map((file) => file.name),
      questionTypes: generationSettings.questionTypes,
      numberOfQuestions: Number(generationSettings.numberOfQuestions),
      collectionId: generationSettings.collectionId,
      language: generationSettings.language,
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
        await new Promise((resolve) =>
          setTimeout(resolve, 700)
        );

        setGenerationResult({
          ...mockGenerationResult,

          generatedFrom: {
            fileName:
              selectedFiles[0]?.name ??
              mockGenerationResult.generatedFrom.fileName,

            fileType:
              selectedFiles[0]?.type ||
              selectedFiles[0]?.name?.split(".").pop() ||
              "file",

            fileNames:
              selectedFiles.map((file) => file.name),
          },

          settings: payload,
        });

        return;
      }

      const formData = new FormData();

      selectedFiles.forEach((file) => {
        formData.append("files", file);
      });

      formData.append(
        "request",
        new Blob(
          [JSON.stringify(payload)],
          {
            type: "application/json",
          }
        )
      );

      const response = await fetch(
        GENERATE_ENDPOINT,
        {
          method: "POST",
          body: formData,
        }
      );

      if (!response.ok) {
        throw new Error("Could not generate questions.");
      }

      const data = await response.json();

      setGenerationResult(
        normalizeGenerationResponse(
          data,
          selectedFiles,
          payload
        )
      );
    } catch (error) {
      setGenerationError(error.message);
    } finally {
      setIsGenerating(false);
    }
  };

  /*
   * Placeholder save logic.
   */
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

        <Stepper
          currentStep={currentStep}
          onStepClick={setCurrentStep}
        />

        <div className={getStepContentClass()}>
          {currentStep === 1 && (
            <UploadBox
              selectedFiles={selectedFiles}
              onFilesSelect={setSelectedFiles}
              settings={generationSettings}
              setSettings={setGenerationSettings}
              collections={collections}
              isLoadingCollections={isLoadingCollections}
              collectionsError={collectionsError}
              onCreateCollection={handleCreateCollection}
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
              selectedFiles={selectedFiles}
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
              (currentStep === 1 && selectedFiles.length === 0) ||
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