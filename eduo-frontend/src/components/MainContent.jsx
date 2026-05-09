import { useEffect, useState } from "react";

import {
  getCollections,
  createCollection,
} from "../api/collectionsApi";

import { uploadMaterials } from "../api/materialsApi";

import Stepper from "./Stepper";
import UploadBox from "./UploadBox";
import SettingsPanel from "./SettingsPanel";
import PreviewSave from "./PreviewSave";
import GenerationPreferences from "./GenerationPreferences";

import "./MainContent.css";

/*
 * Temporary flag while generation backend
 * is still under development.
 */
const USE_MOCK_GENERATION = true;

/*
 * Backend API configuration.
 */
const API_BASE_URL = "http://localhost:8080";
const GENERATE_ENDPOINT = `${API_BASE_URL}/api/generations`;

/*
 * LocalStorage key for saved generation preferences.
 */
const STORAGE_KEY = "eduo_generation_preferences";

/*
 * Default generation settings used when:
 * - the app starts for the first time
 * - no saved preferences exist
 */
const defaultGenerationSettings = {
  questionTypes: ["multipleChoice"],
  numberOfQuestions: 10,

  /*
   * The selected collection where:
   * - uploaded materials belong
   * - generated questions are saved
   */
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
 * Makes frontend safer if backend DTO names change slightly.
 */
function getCollectionId(collection) {
  return collection?.id ?? collection?.collectionId;
}

/*
 * Handles different possible collection name field names.
 */
function getCollectionName(collection) {
  return (
    collection?.name ??
    collection?.collectionName ??
    "Unnamed collection"
  );
}

/*
 * Handles different possible user id field names.
 */
function getUserId(user) {
  return user?.id ?? user?.userId;
}

/*
 * Loads saved preferences from localStorage.
 */
function getPreferences() {
  const saved = localStorage.getItem(STORAGE_KEY);

  if (!saved) {
    return defaultGenerationSettings;
  }

  try {
    const parsed = JSON.parse(saved);

    return {
      ...defaultGenerationSettings,
      ...parsed,

      outputContent: {
        ...defaultGenerationSettings.outputContent,
        ...parsed.outputContent,

        /*
         * Questions should always exist in generated output.
         */
        questions: true,
      },
    };
  } catch {
    return defaultGenerationSettings;
  }
}

/*
 * Temporary mock generation result
 * while backend generation is unfinished.
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
 * Normalizes backend responses into the structure
 * PreviewSave expects.
 *
 * This helps protect the frontend if backend DTOs
 * change slightly.
 */
function normalizeGenerationResponse(
  data,
  selectedFiles,
  settings
) {
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

      /*
       * Needed because several files can now be uploaded.
       */
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
  /*
   * Current step in generation flow.
   */
  const [currentStep, setCurrentStep] = useState(1);

  /*
   * Files selected by the user.
   */
  const [selectedFiles, setSelectedFiles] = useState([]);

  /*
   * Collections belonging to the logged-in user.
   */
  const [collections, setCollections] = useState([]);

  /*
   * Loading/error state for collections.
   */
  const [isLoadingCollections, setIsLoadingCollections] =
    useState(false);

  const [collectionsError, setCollectionsError] =
    useState("");

  /*
   * Loading/error state for material uploads.
   */
  const [isUploadingMaterials, setIsUploadingMaterials] =
    useState(false);

  const [materialUploadError, setMaterialUploadError] =
    useState("");

  /*
   * All generation settings currently selected in GUI.
   */
  const [generationSettings, setGenerationSettings] =
    useState(() => getPreferences());

  /*
   * Generated output state.
   */
  const [generationResult, setGenerationResult] =
    useState(null);

  /*
   * Loading/error state for generation.
   */
  const [isGenerating, setIsGenerating] =
    useState(false);

  const [generationError, setGenerationError] =
    useState("");

  /*
   * Loads all collections belonging to the logged-in user.
   */
  const loadCollections = async (userId) => {
    try {
      setIsLoadingCollections(true);
      setCollectionsError("");

      const loadedCollections =
        await getCollections(userId);

      console.log(
        "Loaded collections:",
        loadedCollections
      );

      setCollections(loadedCollections);

      /*
       * Automatically select first collection
       * if no collection is currently selected.
       */
      if (loadedCollections.length > 0) {
        const firstCollectionId =
          getCollectionId(loadedCollections[0]);

        setGenerationSettings((prevSettings) => ({
          ...prevSettings,

          collectionId:
            prevSettings.collectionId ||
            firstCollectionId ||
            "",
        }));
      }
    } catch (error) {
      console.error(
        "Failed to load collections:",
        error
      );

      setCollectionsError(error.message);
    } finally {
      setIsLoadingCollections(false);
    }
  };

  /*
   * Loads collections when currentUser becomes available.
   */
  useEffect(() => {
    const userId = getUserId(currentUser);

    console.log(
      "Current user in MainContent:",
      currentUser
    );

    if (!userId) {
      return;
    }

    loadCollections(userId);
  }, [currentUser]);

  /*
   * Creates a new collection for the current user.
   */
  const handleCreateCollection = async () => {
    const userId = getUserId(currentUser);

    if (!userId) {
      setCollectionsError(
        "No logged-in user found."
      );

      return;
    }

    const name = window.prompt(
      "Collection name:"
    );

    if (!name || name.trim() === "") {
      return;
    }

    try {
      setIsLoadingCollections(true);
      setCollectionsError("");

      const newCollection =
        await createCollection({
          userId,
          name: name.trim(),
        });

      const newCollectionId =
        getCollectionId(newCollection);

      /*
       * Add new collection directly to state.
       */
      setCollections((prevCollections) => [
        ...prevCollections,
        newCollection,
      ]);

      /*
       * Automatically select newly created collection.
       */
      setGenerationSettings((prevSettings) => ({
        ...prevSettings,
        collectionId: newCollectionId,
      }));
    } catch (error) {
      console.error(
        "Failed to create collection:",
        error
      );

      setCollectionsError(error.message);
    } finally {
      setIsLoadingCollections(false);
    }
  };

  /*
   * Resets generation settings when returning to step 1
   * without files selected.
   */
  useEffect(() => {
    if (
      activePage === "generate" &&
      currentStep === 1 &&
      selectedFiles.length === 0
    ) {
      setGenerationSettings((prevSettings) => ({
        ...getPreferences(),

        /*
         * Keep selected collection when resetting.
         */
        collectionId:
          prevSettings.collectionId,
      }));
    }
  }, [
    activePage,
    currentStep,
    selectedFiles.length,
  ]);

  /*
   * Subtitle below page title.
   */
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

  /*
   * Returns correct layout class for each step.
   */
  const getStepContentClass = () => {
    if (currentStep === 1) {
      return "step-content step-content-start";
    }

    if (currentStep === 3) {
      return "step-content step-content-preview";
    }

    return "step-content step-content-center";
  };

  /*
   * Goes one step forward.
   */
  const goToNextStep = () => {
    setCurrentStep((prevStep) =>
      Math.min(prevStep + 1, 3)
    );
  };

  /*
   * Goes one step backwards.
   */
  const goToPreviousStep = () => {
    setCurrentStep((prevStep) =>
      Math.max(prevStep - 1, 1)
    );
  };

  /*
   * Uploads selected files to selected collection.
   *
   * Used by:
   * - Continue button
   * - Stepper navigation
   */
  const uploadSelectedFilesToCollection =
    async () => {
      if (selectedFiles.length === 0) {
        setMaterialUploadError(
          "Please select at least one file."
        );

        return false;
      }

      if (!generationSettings.collectionId) {
        setMaterialUploadError(
          "Please choose a collection."
        );

        return false;
      }

      try {
        setIsUploadingMaterials(true);
        setMaterialUploadError("");

        await uploadMaterials(
          generationSettings.collectionId,
          selectedFiles
        );

        return true;
      } catch (error) {
        console.error(
          "Failed to upload materials:",
          error
        );

        setMaterialUploadError(
          error.message
        );

        return false;
      } finally {
        setIsUploadingMaterials(false);
      }
    };

  /*
   * Stepper navigation rules:
   *
   * - Navigation should ALWAYS be allowed.
   * - Files should upload automatically IF possible.
   * - Missing files should NOT block navigation.
   */
  const uploadIfPossibleFromStepper =
    async () => {
      const hasFiles =
        selectedFiles.length > 0;

      const hasCollection =
        Boolean(
          generationSettings.collectionId
        );

      /*
       * Skip upload if missing files or collection.
       */
      if (!hasFiles || !hasCollection) {
        return true;
      }

      return await uploadSelectedFilesToCollection();
    };

  /*
   * Handles navigation through stepper.
   */
  const handleStepClick = async (
    targetStep
  ) => {
    /*
     * Ignore click on current step.
     */
    if (targetStep === currentStep) {
      return;
    }

    /*
     * When leaving step 1:
     * upload files IF possible.
     */
    if (
      currentStep === 1 &&
      targetStep > 1
    ) {
      const uploadSucceeded =
        await uploadIfPossibleFromStepper();

      /*
       * Stop navigation if upload failed.
       */
      if (!uploadSucceeded) {
        return;
      }
    }

    setCurrentStep(targetStep);
  };

  /*
   * Starts a completely new generation flow.
   */
  const startNewGeneration = () => {
    setCurrentStep(1);

    setSelectedFiles([]);

    setGenerationSettings(
      (prevSettings) => ({
        ...getPreferences(),

        collectionId:
          prevSettings.collectionId,
      })
    );

    setGenerationResult(null);

    setGenerationError("");

    setIsGenerating(false);

    setMaterialUploadError("");
  };

  /*
   * Builds payload sent to generation backend.
   */
  const buildGenerationPayload = () => {
    return {
      fileNames: selectedFiles.map(
        (file) => file.name
      ),

      questionTypes:
        generationSettings.questionTypes,

      numberOfQuestions: Number(
        generationSettings.numberOfQuestions
      ),

      collectionId:
        generationSettings.collectionId,

      language:
        generationSettings.language,

      focusArea:
        generationSettings.focusArea,

      specificTopics:
        generationSettings.focusArea ===
        "specificTopics"
          ? generationSettings.specificTopics
              .split(",")
              .map((topic) =>
                topic.trim()
              )
              .filter(Boolean)
          : [],

      difficulty:
        generationSettings.difficulty,

      outputContent: {
        ...generationSettings.outputContent,

        questions: true,
      },
    };
  };

  /*
   * Sends generation request to backend.
   */
  const handleGenerate = async () => {
    setIsGenerating(true);

    setGenerationError("");

    setCurrentStep(3);

    const payload =
      buildGenerationPayload();

    try {
      /*
       * Temporary mock mode.
       */
      if (USE_MOCK_GENERATION) {
        await new Promise((resolve) =>
          setTimeout(resolve, 700)
        );

        setGenerationResult({
          ...mockGenerationResult,

          generatedFrom: {
            fileName:
              selectedFiles[0]?.name ??
              mockGenerationResult
                .generatedFrom.fileName,

            fileType:
              selectedFiles[0]?.type ||
              selectedFiles[0]?.name
                ?.split(".")
                .pop() ||
              "file",

            fileNames:
              selectedFiles.map(
                (file) => file.name
              ),
          },

          settings: payload,
        });

        return;
      }

      /*
       * Real backend generation request.
       */
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

      const response =
        await fetch(
          GENERATE_ENDPOINT,
          {
            method: "POST",
            body: formData,
          }
        );

      if (!response.ok) {
        throw new Error(
          "Could not generate questions."
        );
      }

      const data =
        await response.json();

      setGenerationResult(
        normalizeGenerationResponse(
          data,
          selectedFiles,
          payload
        )
      );
    } catch (error) {
      setGenerationError(
        error.message
      );
    } finally {
      setIsGenerating(false);
    }
  };

  /*
   * Placeholder save logic.
   */
  const handleSave = () => {
    const savePayload = {
      generationId:
        generationResult?.id,

      collectionId:
        generationSettings.collectionId,

      output:
        generationResult?.output,

      generatedFrom:
        generationResult?.generatedFrom,

      settings:
        generationResult?.settings,
    };

    console.log(
      "Save payload:",
      savePayload
    );
  };

  /*
   * Returns correct button text depending on current state.
   */
  const getButtonText = () => {
    if (currentStep === 1) {
      return isUploadingMaterials
        ? "Uploading..."
        : "Continue";
    }

    if (currentStep === 2) {
      return isGenerating
        ? "Generating..."
        : "Generate";
    }

    return "Save to Collection";
  };

  /*
   * Main action button logic.
   *
   * Unlike the stepper:
   * - Continue button REQUIRES upload success.
   */
  const handleMainButtonClick =
    async () => {
      /*
       * Step 1:
       * Upload files before continuing.
       */
      if (currentStep === 1) {
        const uploadSucceeded =
          await uploadSelectedFilesToCollection();

        if (!uploadSucceeded) {
          return;
        }

        goToNextStep();

        return;
      }

      /*
       * Step 2:
       * Generate questions.
       */
      if (currentStep === 2) {
        handleGenerate();

        return;
      }

      /*
       * Step 3:
       * Save generation.
       */
      if (currentStep === 3) {
        handleSave();
      }
    };

  /*
   * Preferences page.
   */
  if (activePage === "preferences") {
    return (
      <main className="main-content">
        <section className="content-card">
          <GenerationPreferences />
        </section>
      </main>
    );
  }

  /*
   * Placeholder pages.
   */
  if (activePage !== "generate") {
    return (
      <main className="main-content">
        <section className="content-card">
          <div className="step-placeholder">
            <h1>
              {activePage ===
                "collections" &&
                "My Collections"}

              {activePage ===
                "material" &&
                "Material"}
            </h1>

            <p>
              This page will be implemented later.
            </p>
          </div>
        </section>
      </main>
    );
  }

  /*
   * Main generation flow.
   */
  return (
    <main className="main-content">
      <section className="content-card">
        <div className="title-section">
          <h1>
            Create questions from material
          </h1>

          <p>{getSubtitle()}</p>
        </div>

        <Stepper
          currentStep={currentStep}
          onStepClick={handleStepClick}
        />

        <div
          className={getStepContentClass()}
        >
          {currentStep === 1 && (
            <UploadBox
              selectedFiles={
                selectedFiles
              }

              onFilesSelect={
                setSelectedFiles
              }

              settings={
                generationSettings
              }

              setSettings={
                setGenerationSettings
              }

              collections={
                collections
              }

              isLoadingCollections={
                isLoadingCollections
              }

              collectionsError={
                collectionsError ||
                materialUploadError
              }

              onCreateCollection={
                handleCreateCollection
              }
            />
          )}

          {currentStep === 2 && (
            <SettingsPanel
              settings={
                generationSettings
              }

              setSettings={
                setGenerationSettings
              }
            />
          )}

          {currentStep === 3 && (
            <PreviewSave
              generationResult={
                generationResult
              }

              isGenerating={
                isGenerating
              }

              generationError={
                generationError
              }

              selectedFiles={
                selectedFiles
              }

              settings={
                generationSettings
              }

              setSettings={
                setGenerationSettings
              }

              onRegenerate={
                handleGenerate
              }
            />
          )}
        </div>

        <div className="actions">
          {currentStep > 1 && (
            <button
              className="button secondary-button"

              onClick={
                goToPreviousStep
              }

              disabled={
                isGenerating ||
                isUploadingMaterials
              }
            >
              Back
            </button>
          )}

          <button
            className="button primary-button"

            onClick={
              handleMainButtonClick
            }

            disabled={
              /*
               * Continue button requires:
               * - at least one file
               * - selected collection
               */
              (currentStep === 1 &&
                (selectedFiles.length ===
                  0 ||
                  !generationSettings.collectionId)) ||

              isUploadingMaterials ||

              isGenerating ||

              (currentStep === 3 &&
                !generationResult)
            }
          >
            {getButtonText()}
          </button>

          {currentStep === 3 && (
            <button
              className="button primary-button"

              onClick={
                startNewGeneration
              }

              disabled={
                isGenerating ||
                isUploadingMaterials
              }
            >
              Discard
            </button>
          )}
        </div>
      </section>
    </main>
  );
};