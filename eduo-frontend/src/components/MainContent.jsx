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
 * Generation API.
 * Sends CreateGenerationRequest to backend.
 */
import { createGeneration } from "../api/generationsApi";

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

  collectionId: "",

  focusArea: "entireMaterial",

  specificTopics: "",

  difficulty: ["Medium"],

  language: "English",

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

  if (!saved) {
    return defaultGenerationSettings;
  }

  try {
    const parsed = JSON.parse(saved);

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
        questions: true,
      },
    };
  } catch {
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
    id:
      material?.id ??
      material?.materialId ??
      material?.sourceMaterialId ??
      null,

    fileName:
      material?.fileName ??
      material?.filename ??
      material?.name ??
      fallbackFile?.name ??
      "Unknown file",

    fileType:
      material?.fileType ??
      material?.contentType ??
      material?.mimeType ??
      fallbackFile?.type ??
      "Unknown file type",

    size:
      material?.size ??
      material?.fileSize ??
      fallbackFile?.size ??
      null,

    collectionId:
      material?.collectionId ??
      material?.collection?.id ??
      null,

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
      data.rawContent ??
      data.quiz?.rawContent ??
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

    settings,

    createdAt:
      data.createdAt ??
      new Date().toISOString(),
  };
}

export default function MainContent({
  activePage,
  currentUser,
}) {
  const [currentStep, setCurrentStep] =
    useState(1);

  const [selectedFiles, setSelectedFiles] =
    useState([]);

  const [
    uploadedMaterialsMetadata,
    setUploadedMaterialsMetadata,
  ] = useState([]);

  const [
    uploadedFilesSignature,
    setUploadedFilesSignature,
  ] = useState("");

  const [collections, setCollections] =
    useState([]);

  const [
    isLoadingCollections,
    setIsLoadingCollections,
  ] = useState(false);

  const [
    collectionsError,
    setCollectionsError,
  ] = useState("");

  const [
    isUploadingMaterials,
    setIsUploadingMaterials,
  ] = useState(false);

  const [
    materialUploadError,
    setMaterialUploadError,
  ] = useState("");

  const [
    materialUploadStatus,
    setMaterialUploadStatus,
  ] = useState({
    type: "",
    message: "",
  });

  const [
    generationSettings,
    setGenerationSettings,
  ] = useState(() => getPreferences());

  const [
    generationResult,
    setGenerationResult,
  ] = useState(null);

  const [isGenerating, setIsGenerating] =
    useState(false);

  const [generationError, setGenerationError] =
    useState("");

  const loadCollections = async (userId) => {
    try {
      setIsLoadingCollections(true);
      setCollectionsError("");

      const loadedCollections =
        await getCollections(userId);

      setCollections(loadedCollections);

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

      setCollections((prevCollections) => [
        ...prevCollections,
        newCollection,
      ]);

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

  useEffect(() => {
    const userId = getUserId(currentUser);

    if (!userId) {
      return;
    }

    loadCollections(userId);
  }, [currentUser]);

  useEffect(() => {
    setUploadedMaterialsMetadata([]);
    setUploadedFilesSignature("");

    setMaterialUploadStatus({
      type: "",
      message: "",
    });
  }, [selectedFiles]);

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
   * Builds CreateGenerationRequest sent to backend.
   *
   * Backend currently expects:
   * {
   *   sourceMaterialIds: [...]
   * }
   *
   * Settings are kept separately in frontend state
   * until backend DTO supports them.
   */
  const buildGenerationPayload = () => {
    return {
      sourceMaterialIds:
        uploadedMaterialsMetadata
          .map((material) => material.id)
          .filter(Boolean),
    };
  };

  const handleGenerate = async () => {
    setIsGenerating(true);
    setGenerationError("");
    setCurrentStep(3);

    try {
      const payload =
        buildGenerationPayload();

      console.log(
        "Generation request payload:",
        payload
      );

      const response =
        await createGeneration(
          generationSettings.collectionId,
          payload
        );

      console.log(
        "Generation response:",
        response
      );

      setGenerationResult(
        normalizeGenerationResponse(
          response,
          selectedFiles,
          generationSettings,
          uploadedMaterialsMetadata
        )
      );
    } catch (error) {
      console.error(
        "Failed to generate:",
        error
      );

      setGenerationError(error.message);
    } finally {
      setIsGenerating(false);
    }
  };

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