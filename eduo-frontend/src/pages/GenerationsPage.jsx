import { useEffect, useState } from "react";

/*
 * Collection API functions.
 */
import {
  getCollections,
  createCollection,
} from "../api/collectionsApi";

/*
 * Material management API.
 * Includes functions for uploading files and fetching material metadata.
 */
import {
  uploadMaterials,
  getMaterialsByCollection,
} from "../api/materialsApi";

/*
 * Generation API functions.
 * Includes functions for creating generations and fetching generation results.
 */
import {
  createGeneration,
  getGenerationById,
} from "../api/generationsApi";

/*
 * UI components used in the generation flow.
 */
import Stepper from "../components/generations/Stepper";
import UploadBox from "../components/generations/UploadBox";
import SettingsPanel from "../components/generations/SettingsPanel";
import PreviewSave from "../components/generations/PreviewSave";
import GenerationPreferences from "../components/GenerationPreferences";

import "./GenerationsPage.css";

const STORAGE_KEY =
  "eduo_generation_preferences";

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

function getCollectionId(collection) {
  return collection?.id ?? collection?.collectionId;
}

function getUserId(user) {
  return user?.id ?? user?.userId;
}

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

function normalizeGenerationResponse(
  data,
  selectedFiles,
  settings,
  uploadedMaterialsMetadata
) {
  const sourceMaterials =
    data.sourceMaterials ??
    data.sourceMaterialIds ??
    uploadedMaterialsMetadata ??
    [];

  return {
    id: data.id ?? data.generationId ?? null,

    output:
      data.quiz?.rawContent ??
      data.output ??
      data.generatedOutput ??
      data.text ??
      data.rawContent ??
      "",

    generatedFrom: {
      fileName:
        sourceMaterials[0]?.fileName ??
        sourceMaterials[0]?.filename ??
        selectedFiles[0]?.name ??
        "Unknown source",

      fileType:
        sourceMaterials[0]?.fileType ??
        sourceMaterials[0]?.contentType ??
        selectedFiles[0]?.type ??
        "Unknown file type",

      fileNames:
        sourceMaterials.map(
          (material) =>
            material.fileName ??
            material.filename ??
            material.name ??
            "Unknown file"
        ),

      materials: sourceMaterials,
    },

    settings: {
      ...settings,

      numberOfQuestions:
        data.numOfQuestions ??
        settings.numberOfQuestions,

      language:
        data.language === "SWEDISH"
          ? "Swedish"
          : data.language === "ENGLISH"
          ? "English"
          : settings.language,

      focusArea:
        data.focusArea === "KEY_CONCEPTS"
          ? "Key concepts"
          : data.focusArea === "TOPICS"
          ? "Specific topics"
          : data.focusArea === "ENTIRE_MATERIAL"
          ? "Entire material"
          : settings.focusArea,

      specificTopics:
        data.topics ?? settings.specificTopics,

      difficulty: [
        data.easy ? "Easy" : null,
        data.medium ? "Medium" : null,
        data.hard ? "Hard" : null,
      ].filter(Boolean),

      questionTypes: [
        data.multipleChoice ? "multipleChoice" : null,
        data.openEnded ? "openEnded" : null,
        data.trueFalse ? "trueFalse" : null,
      ].filter(Boolean),

      outputContent: {
        questions:
          data.questions ??
          settings.outputContent?.questions,

        correctAnswers:
          data.correctAnswers ??
          settings.outputContent?.correctAnswers,

        answerExplanations:
          data.explanations ??
          settings.outputContent?.answerExplanations,
      },
    },

    createdAt:
      data.createdAt ??
      new Date().toISOString(),
  };
}

export default function MainContent({
  activePage,
  currentUser,
  initialCollection,
  initialMaterial,
  generateFromCollection,
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

  const [existingMaterials, setExistingMaterials] =
    useState([]);

  const [
    selectedExistingMaterialIds,
    setSelectedExistingMaterialIds,
  ] = useState([]);

  const [
    isLoadingExistingMaterials,
    setIsLoadingExistingMaterials,
  ] = useState(false);

  const [
    existingMaterialsError,
    setExistingMaterialsError,
  ] = useState("");

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

  const [
    isCreateCollectionModalOpen,
    setIsCreateCollectionModalOpen,
  ] = useState(false);

  const [
    newCollectionName,
    setNewCollectionName,
  ] = useState("");

  const [
    newCollectionDescription,
    setNewCollectionDescription,
  ] = useState("");

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

  const handleCreateCollection =
    async (event) => {
      if (event) {
        event.preventDefault();
      }

      const userId =
        getUserId(currentUser);

      if (!userId) {
        setCollectionsError(
          "No logged-in user found."
        );

        return;
      }

      if (
        !newCollectionName ||
        newCollectionName.trim() === ""
      ) {
        return;
      }

      try {
        setIsLoadingCollections(true);
        setCollectionsError("");

        const newCollection =
          await createCollection({
            userId,
            name: newCollectionName.trim(),
            description:
              newCollectionDescription.trim(),
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

        setNewCollectionName("");
        setNewCollectionDescription("");
        setIsCreateCollectionModalOpen(false);
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

  /*
  * Prefills the generation flow when opened from CollectionDetailsPage.
  *
  * Generate from collection:
  * Selects the collection and stays on step 1.
  *
  * Use specific material for generation:
  * Selects the collection and material, then jumps to step 2.
  */
  useEffect(() => {
    if (!initialCollection) {
      return;
    }

    const collectionId =
      getCollectionId(initialCollection);

    if (!collectionId) {
      return;
    }

    setGenerationSettings((prevSettings) => ({
      ...prevSettings,
      collectionId,
    }));

    if (initialMaterial?.id) {
      setSelectedExistingMaterialIds([
        initialMaterial.id,
      ]);

      setCurrentStep(2);
      return;
    }

    if (generateFromCollection) {
      setSelectedExistingMaterialIds([]);
      setCurrentStep(1);
    }
  }, [
    initialCollection,
    initialMaterial,
    generateFromCollection,
  ]);

  useEffect(() => {
    const loadExistingMaterials =
      async () => {
        if (
          !generationSettings.collectionId
        ) {
          setExistingMaterials([]);

          setSelectedExistingMaterialIds(
            []
          );

          return;
        }

        try {
          setIsLoadingExistingMaterials(true);
          setExistingMaterialsError("");

          const materials =
            await getMaterialsByCollection(
              generationSettings.collectionId
            );

          setExistingMaterials(materials);

          if (
            initialMaterial?.id &&
            getCollectionId(initialCollection) ===
              generationSettings.collectionId
          ) {
            setSelectedExistingMaterialIds([
              initialMaterial.id,
            ]);
          } else {
            setSelectedExistingMaterialIds(
              []
            );
          }
        } catch (error) {
          console.error(
            "Failed to load existing materials:",
            error
          );

          setExistingMaterialsError(
            error.message
          );
        } finally {
          setIsLoadingExistingMaterials(false);
        }
      };

    loadExistingMaterials();
  }, [
    generationSettings.collectionId,
    initialCollection,
    initialMaterial,
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

  const uploadSelectedFilesIfPossible =
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
        await uploadSelectedFilesIfPossible();

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

  const buildGenerationPayload = () => {
    const uploadedMaterialIds =
      uploadedMaterialsMetadata
        .map((material) => material.id)
        .filter(Boolean);

    return {
      sourceMaterials: [
        ...selectedExistingMaterialIds,
        ...uploadedMaterialIds,
      ],

      numOfQuestions: Number(
        generationSettings.numberOfQuestions
      ),

      language:
        generationSettings.language
          ?.toString()
          .toLowerCase() === "swedish"
          ? "SWEDISH"
          : "ENGLISH",

      focusArea:
        generationSettings.focusArea === "keyConcepts"
          ? "KEY_CONCEPTS"
          : generationSettings.focusArea === "specificTopics"
          ? "TOPICS"
          : generationSettings.focusArea === "entireMaterial"
          ? "ENTIRE_MATERIAL"
          : "ENTIRE_MATERIAL",

      topics:
        generationSettings.specificTopics ?? "",

      easy:
        generationSettings.difficulty.includes("Easy"),

      medium:
        generationSettings.difficulty.includes("Medium"),

      hard:
        generationSettings.difficulty.includes("Hard"),

      multipleChoice:
        generationSettings.questionTypes.includes(
          "multipleChoice"
        ),

      openEnded:
        generationSettings.questionTypes.includes(
          "openEnded"
        ),

      trueFalse:
        generationSettings.questionTypes.includes(
          "trueFalse"
        ),

      questions:
        generationSettings.outputContent?.questions ?? true,

      correctAnswers:
        generationSettings.outputContent?.correctAnswers ?? false,

      explanations:
        generationSettings.outputContent?.answerExplanations ?? false,

      description: false,
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

      const collectionId =
        generationSettings.collectionId;

      const createdGeneration =
        await createGeneration(
          collectionId,
          payload
        );

      const generationId =
        createdGeneration?.id ??
        createdGeneration?.generationId;

      if (!generationId) {
        throw new Error(
          "Generation was created, but no generation ID was returned."
        );
      }

      const savedGeneration =
        await getGenerationById(
          collectionId,
          generationId
        );

      setGenerationResult(
        normalizeGenerationResponse(
          savedGeneration,
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
        if (selectedFiles.length > 0) {
          const uploadSucceeded =
            await uploadSelectedFilesToCollection();

          if (!uploadSucceeded) {
            return;
          }
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
              selectedFiles={selectedFiles}
              onFilesSelect={setSelectedFiles}
              settings={generationSettings}
              setSettings={setGenerationSettings}
              collections={collections}
              isLoadingCollections={
                isLoadingCollections
              }
              collectionsError={
                collectionsError ||
                materialUploadError
              }
              onCreateCollection={() =>
                setIsCreateCollectionModalOpen(true)
              }
              uploadStatus={materialUploadStatus}
              existingMaterials={existingMaterials}
              selectedExistingMaterialIds={
                selectedExistingMaterialIds
              }
              setSelectedExistingMaterialIds={
                setSelectedExistingMaterialIds
              }
              isLoadingExistingMaterials={
                isLoadingExistingMaterials
              }
              existingMaterialsError={
                existingMaterialsError
              }
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
            onClick={handleMainButtonClick}
            disabled={
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
              onClick={startNewGeneration}
              disabled={
                isGenerating ||
                isUploadingMaterials
              }
            >
              Discard
            </button>
          )}
        </div>

        {isCreateCollectionModalOpen && (
          <div className="collection-modal-backdrop">
            <form
              className="collection-modal"
              onSubmit={handleCreateCollection}
            >
              <h2>Create Collection</h2>

              <label>
                Collection name

                <input
                  type="text"
                  value={newCollectionName}
                  onChange={(event) =>
                    setNewCollectionName(
                      event.target.value
                    )
                  }
                  placeholder="Example: Interaktionsdesign"
                />
              </label>

              <label>
                Description

                <textarea
                  value={
                    newCollectionDescription
                  }
                  onChange={(event) =>
                    setNewCollectionDescription(
                      event.target.value
                    )
                  }
                  placeholder="What will this collection be used for?"
                />
              </label>

              <div className="collection-modal-actions">
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() =>
                    setIsCreateCollectionModalOpen(
                      false
                    )
                  }
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={
                    !newCollectionName.trim()
                  }
                >
                  Create
                </button>
              </div>
            </form>
          </div>
        )}
      </section>
    </main>
  );
}