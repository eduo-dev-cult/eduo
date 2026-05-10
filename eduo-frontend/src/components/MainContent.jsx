import { useEffect, useState } from "react";

/*
 * Collection API functions.
 */
import {
  getCollections,
  createCollection,
} from "../api/collectionsApi";

/*
 * Material upload API.
 * Returns metadata for uploaded files.
 */
import { uploadMaterials } from "../api/materialsApi";

/*
 * UI components used in the generation flow.
 */
import Stepper from "./Stepper";
import UploadBox from "./UploadBox";
import SettingsPanel from "./SettingsPanel";
import PreviewSave from "./PreviewSave";
import GenerationPreferences from "./GenerationPreferences";

import "./MainContent.css";

/*
 * Temporary flag while generation backend
 * is still under development.
 *
 * true  = use mock output
 * false = use real backend
 */
const USE_MOCK_GENERATION = true;

/*
 * Backend API configuration.
 */
const API_BASE_URL = "http://localhost:8080";

const GENERATE_ENDPOINT =
  `${API_BASE_URL}/api/generations`;

/*
 * LocalStorage key for saved generation preferences.
 */
const STORAGE_KEY =
  "eduo_generation_preferences";

/*
 * Default generation settings used:
 * - on first app start
 * - if no saved preferences exist
 */
const defaultGenerationSettings = {
  questionTypes: ["multipleChoice"],

  numberOfQuestions: 10,

  /*
   * Collection where:
   * - uploaded materials are stored
   * - generated questions are saved
   *
   * Important:
   * This must eventually be a real collection UUID from backend.
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
 * Handles different possible backend field names
 * for collection IDs.
 */
function getCollectionId(collection) {
  return collection?.id ?? collection?.collectionId;
}

/*
 * Handles different possible backend field names
 * for user IDs.
 */
function getUserId(user) {
  return user?.id ?? user?.userId;
}

/*
 * Loads saved preferences from localStorage.
 */
function getPreferences() {
  const saved = localStorage.getItem(STORAGE_KEY);

  /*
   * No saved settings found.
   */
  if (!saved) {
    return defaultGenerationSettings;
  }

  try {
    const parsed = JSON.parse(saved);

    /*
     * Fix for older saved preferences.
     *
     * Earlier, collectionId could be saved as "default".
     * That breaks uploads because backend expects a UUID:
     * /collections/{collectionId}/materials
     *
     * So if localStorage contains "default",
     * we replace it with "" and let the app select
     * a real collection from backend instead.
     */
    const safeCollectionId =
      parsed.collectionId === "default"
        ? ""
        : parsed.collectionId;

    return {
      ...defaultGenerationSettings,
      ...parsed,

      collectionId: safeCollectionId,

      outputContent: {
        ...defaultGenerationSettings.outputContent,
        ...parsed.outputContent,

        /*
         * Questions should always exist.
         */
        questions: true,
      },
    };
  } catch {
    /*
     * Invalid localStorage data.
     */
    return defaultGenerationSettings;
  }
}

/*
 * Converts backend material metadata into
 * a consistent frontend format.
 */
function normalizeMaterialMetadata(
  material,
  fallbackFile
) {
  return {
    /*
     * Backend material ID.
     */
    id:
      material?.id ??
      material?.materialId ??
      material?.sourceMaterialId ??
      null,

    /*
     * Uploaded file name.
     */
    fileName:
      material?.fileName ??
      material?.filename ??
      material?.name ??
      fallbackFile?.name ??
      "Unknown file",

    /*
     * MIME type.
     */
    fileType:
      material?.fileType ??
      material?.contentType ??
      material?.mimeType ??
      fallbackFile?.type ??
      "Unknown file type",

    /*
     * File size in bytes.
     */
    size:
      material?.size ??
      material?.fileSize ??
      fallbackFile?.size ??
      null,

    /*
     * Which collection the material belongs to.
     */
    collectionId:
      material?.collectionId ??
      material?.collection?.id ??
      null,

    /*
     * Keep original backend object for debugging/future use.
     */
    raw: material,
  };
}

/*
 * Creates a lightweight signature for uploaded files.
 * Used to avoid duplicate uploads of the exact same files.
 */
function createFilesSignature(files, collectionId) {
  return JSON.stringify({
    collectionId,

    files: files.map((file) => ({
      name: file.name,
      size: file.size,
      lastModified: file.lastModified,
    })),
  });
}

/*
 * Temporary mock generation result
 * while generation backend is unfinished.
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
    fileNames: [],
  },

  settings: defaultGenerationSettings,

  createdAt: new Date().toISOString(),
};

/*
 * Converts backend generation response into
 * the format expected by PreviewSave.
 */
function normalizeGenerationResponse(
  data,
  selectedFiles,
  settings,
  uploadedMaterialsMetadata
) {
  const firstFile = selectedFiles[0];
  const firstMaterial = uploadedMaterialsMetadata[0];

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
        firstMaterial?.fileName ??
        firstFile?.name ??
        "Unknown source",

      fileType:
        data.generatedFrom?.fileType ??
        data.fileType ??
        firstMaterial?.fileType ??
        firstFile?.type ??
        "Unknown file type",

      fileNames:
        data.generatedFrom?.fileNames ??
        uploadedMaterialsMetadata.map(
          (material) => material.fileName
        ),

      materials: uploadedMaterialsMetadata,
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
   * Current step in the generation flow.
   */
  const [currentStep, setCurrentStep] =
    useState(1);

  /*
   * Browser File objects selected by the user.
   */
  const [selectedFiles, setSelectedFiles] =
    useState([]);

  /*
   * Metadata returned from backend after upload.
   * This is saved for later use on page 3.
   */
  const [
    uploadedMaterialsMetadata,
    setUploadedMaterialsMetadata,
  ] = useState([]);

  /*
   * Used to avoid uploading the exact same files twice.
   */
  const [
    uploadedFilesSignature,
    setUploadedFilesSignature,
  ] = useState("");

  /*
   * Collections belonging to the current user.
   */
  const [collections, setCollections] =
    useState([]);

  /*
   * Loading state while fetching collections.
   */
  const [
    isLoadingCollections,
    setIsLoadingCollections,
  ] = useState(false);

  /*
   * Error state for collections.
   */
  const [
    collectionsError,
    setCollectionsError,
  ] = useState("");

  /*
   * Loading state while uploading materials.
   */
  const [
    isUploadingMaterials,
    setIsUploadingMaterials,
  ] = useState(false);

  /*
   * Error state for material upload.
   */
  const [
    materialUploadError,
    setMaterialUploadError,
  ] = useState("");

  /*
   * Upload feedback shown in UploadBox.
   */
  const [
    materialUploadStatus,
    setMaterialUploadStatus,
  ] = useState({
    type: "",
    message: "",
  });

  /*
   * Current generation settings.
   */
  const [
    generationSettings,
    setGenerationSettings,
  ] = useState(() => getPreferences());

  /*
   * Generated result returned from backend/mock.
   */
  const [
    generationResult,
    setGenerationResult,
  ] = useState(null);

  /*
   * Loading state while generating questions.
   */
  const [isGenerating, setIsGenerating] =
    useState(false);

  /*
   * Error state for generation.
   */
  const [generationError, setGenerationError] =
    useState("");

  /*
   * Loads collections belonging to current user.
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
       * Auto-select first collection if no collection
       * is already selected.
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
   * Creates a new collection for the current user.
   * This function is passed down to UploadBox.
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
       * Add the new collection to the existing list.
       */
      setCollections((prevCollections) => [
        ...prevCollections,
        newCollection,
      ]);

      /*
       * Select the newly created collection automatically.
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
   * Load collections when currentUser becomes available.
   */
  useEffect(() => {
    const userId = getUserId(currentUser);

    if (!userId) {
      return;
    }

    loadCollections(userId);
  }, [currentUser]);

  /*
   * If selected files change, old uploaded metadata
   * is no longer valid.
   */
  useEffect(() => {
    setUploadedMaterialsMetadata([]);
    setUploadedFilesSignature("");

    setMaterialUploadStatus({
      type: "",
      message: "",
    });
  }, [selectedFiles]);

  /*
   * Subtitle below the page title.
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
   * Uploads files to backend and stores
   * returned metadata in frontend state.
   */
  const uploadSelectedFilesToCollection =
    async () => {
      if (selectedFiles.length === 0) {
        setMaterialUploadError(
          "Please select at least one file."
        );

        setMaterialUploadStatus({
          type: "error",
          message: "Please select at least one file.",
        });

        return false;
      }

      if (!generationSettings.collectionId) {
        setMaterialUploadError(
          "Please choose a collection."
        );

        setMaterialUploadStatus({
          type: "error",
          message: "Please choose a collection.",
        });

        return false;
      }

      const currentSignature =
        createFilesSignature(
          selectedFiles,
          generationSettings.collectionId
        );

      /*
       * If same upload already succeeded:
       * reuse metadata instead of uploading again.
       */
      if (
        uploadedFilesSignature ===
          currentSignature &&
        uploadedMaterialsMetadata.length > 0
      ) {
        setMaterialUploadStatus({
          type: "success",
          message: "Files are already uploaded.",
        });

        return true;
      }

      try {
        setIsUploadingMaterials(true);
        setMaterialUploadError("");

        setMaterialUploadStatus({
          type: "loading",
          message: "Uploading files...",
        });

        const uploadedMaterials =
          await uploadMaterials(
            generationSettings.collectionId,
            selectedFiles
          );

        const normalizedMetadata =
          uploadedMaterials.map(
            (material, index) =>
              normalizeMaterialMetadata(
                material,
                selectedFiles[index]
              )
          );

        console.log(
          "Uploaded materials metadata:",
          normalizedMetadata
        );

        setUploadedMaterialsMetadata(
          normalizedMetadata
        );

        setUploadedFilesSignature(
          currentSignature
        );

        setMaterialUploadStatus({
          type: "success",
          message: "Files uploaded successfully.",
        });

        return true;
      } catch (error) {
        console.error(
          "Failed to upload materials:",
          error
        );

        setMaterialUploadError(error.message);

        setMaterialUploadStatus({
          type: "error",
          message: error.message,
        });

        return false;
      } finally {
        setIsUploadingMaterials(false);
      }
    };

  /*
   * Used by stepper navigation.
   * Stepper should allow navigation even without files,
   * but upload files if possible.
   */
  const uploadIfPossibleFromStepper =
    async () => {
      const hasFiles = selectedFiles.length > 0;

      const hasCollection = Boolean(
        generationSettings.collectionId
      );

      if (!hasFiles || !hasCollection) {
        return true;
      }

      return await uploadSelectedFilesToCollection();
    };

  /*
   * Handles clicks on stepper numbers.
   */
  const handleStepClick = async (
    targetStep
  ) => {
    if (targetStep === currentStep) {
      return;
    }

    if (
      currentStep === 1 &&
      targetStep > 1
    ) {
      const uploadSucceeded =
        await uploadIfPossibleFromStepper();

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

    setUploadedMaterialsMetadata([]);

    setUploadedFilesSignature("");

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

    setMaterialUploadStatus({
      type: "",
      message: "",
    });
  };

  /*
   * Builds payload sent to generation backend.
   */
  const buildGenerationPayload = () => {
    return {
      fileNames: selectedFiles.map(
        (file) => file.name
      ),

      materialIds:
        uploadedMaterialsMetadata
          .map((material) => material.id)
          .filter(Boolean),

      materials:
        uploadedMaterialsMetadata,

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
      if (USE_MOCK_GENERATION) {
        await new Promise((resolve) =>
          setTimeout(resolve, 700)
        );

        setGenerationResult({
          ...mockGenerationResult,

          generatedFrom: {
            fileName:
              uploadedMaterialsMetadata[0]?.fileName ??
              selectedFiles[0]?.name ??
              mockGenerationResult.generatedFrom.fileName,

            fileType:
              uploadedMaterialsMetadata[0]?.fileType ??
              (
                selectedFiles[0]?.type ||
                selectedFiles[0]?.name
                  ?.split(".")
                  .pop() ||
                "file"
              ),

            fileNames:
              uploadedMaterialsMetadata.length > 0
                ? uploadedMaterialsMetadata.map(
                    (material) =>
                      material.fileName
                  )
                : selectedFiles.map(
                    (file) => file.name
                  ),

            materials:
              uploadedMaterialsMetadata,
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
          payload,
          uploadedMaterialsMetadata
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
      generationId:
        generationResult?.id,

      collectionId:
        generationSettings.collectionId,

      output:
        generationResult?.output,

      generatedFrom:
        generationResult?.generatedFrom,

      uploadedMaterials:
        uploadedMaterialsMetadata,

      settings:
        generationResult?.settings,
    };

    console.log(
      "Save payload:",
      savePayload
    );
  };

  /*
   * Returns correct button text depending on state.
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
   */
  const handleMainButtonClick =
    async () => {
      if (currentStep === 1) {
        const uploadSucceeded =
          await uploadSelectedFilesToCollection();

        if (!uploadSucceeded) {
          return;
        }

        setCurrentStep(2);

        return;
      }

      if (currentStep === 2) {
        handleGenerate();

        return;
      }

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
   * Main generation flow UI.
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
          className={
            currentStep === 1
              ? "step-content step-content-start"
              : currentStep === 3
              ? "step-content step-content-preview"
              : "step-content step-content-center"
          }
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

              uploadStatus={
                materialUploadStatus
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

              onClick={() =>
                setCurrentStep((prev) =>
                  Math.max(prev - 1, 1)
                )
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
}