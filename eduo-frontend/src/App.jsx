import { useEffect, useState } from "react";

import "./App.css";
import SideBar from "./components/SideBar";
import TopBar from "./components/TopBar";

import GenerationsPage from "./pages/GenerationsPage";
import CollectionsPage from "./pages/CollectionsPage";
import CollectionDetailsPage from "./pages/CollectionDetailsPage";
import PreviewSave from "./components/generations/PreviewSave";

import "./styles/Variables.css";
import "./styles/Global.css";
import "./styles/Buttons.css";

function App() {
  // Keeps track of which main page/view is currently shown in the app.
  const [activePage, setActivePage] = useState("generate");

  // Stores the currently logged in demo user returned from the backend.
  const [currentUser, setCurrentUser] = useState(null);

  // Prevents the app from trying to load user-specific data before login is done.
  const [isDemoLoginDone, setIsDemoLoginDone] = useState(false);

  // Stores the collection that is currently selected in the CollectionsPage.
  const [selectedCollection, setSelectedCollection] = useState(null);

  // Opens Generate directly from a collection.
  const [
    shouldGenerateFromCollection,
    setShouldGenerateFromCollection,
  ] = useState(false);

  // Stores the generation that is currently opened from CollectionDetailsPage.
  const [selectedGeneration, setSelectedGeneration] = useState(null);

  // Stores a material selected from CollectionDetailsPage for generation.
  const [
    selectedMaterialForGeneration,
    setSelectedMaterialForGeneration,
  ] = useState(null);

  // Opens the upload modal when navigating from a collection card.
  const [
    shouldOpenCollectionUploadModal,
    setShouldOpenCollectionUploadModal,
  ] = useState(false);

  useEffect(() => {
    const loginDemoUser = async () => {
      try {
        const response = await fetch("http://localhost:8080/users/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify({
            username: "demo",
            password: "demo",
          }),
        });

        if (!response.ok) {
          console.error("Demo login failed with status:", response.status);
          return;
        }

        const user = await response.json();
        setCurrentUser(user);
      } catch (error) {
        console.error("Failed to auto-login demo user:", error);
      } finally {
        setIsDemoLoginDone(true);
      }
    };

    loginDemoUser();
  }, []);

  const handleSetActivePage = (page) => {
    if (page === "generate") {
      setSelectedMaterialForGeneration(null);
      setShouldGenerateFromCollection(false);
    }

    setActivePage(page);
  };

  const buildSavedGenerationPreview = () => {
    return {
      output: selectedGeneration?.quiz?.rawContent ?? "",

      generatedFrom: {
        fileNames:
          selectedGeneration?.sourceMaterials?.map(
            (material) =>
              material.filename ??
              material.fileName ??
              "Unknown file"
          ) ?? [],
      },

      settings: {
        numberOfQuestions: selectedGeneration?.numOfQuestions,

        language:
          selectedGeneration?.language === "ENGLISH"
            ? "English"
            : selectedGeneration?.language === "SWEDISH"
            ? "Swedish"
            : selectedGeneration?.language,

        focusArea:
          selectedGeneration?.focusArea === "ENTIRE_MATERIAL"
            ? "entireMaterial"
            : selectedGeneration?.focusArea === "KEY_CONCEPTS"
            ? "keyConcepts"
            : selectedGeneration?.focusArea === "TOPICS"
            ? "specificTopics"
            : selectedGeneration?.focusArea,

        specificTopics: selectedGeneration?.topics,

        difficulty: [
          selectedGeneration?.easy ? "Easy" : null,
          selectedGeneration?.medium ? "Medium" : null,
          selectedGeneration?.hard ? "Hard" : null,
        ].filter(Boolean),

        questionTypes: [
          selectedGeneration?.multipleChoice ? "multipleChoice" : null,
          selectedGeneration?.openEnded ? "openEnded" : null,
          selectedGeneration?.trueFalse ? "trueFalse" : null,
        ].filter(Boolean),

        outputContent: {
          questions: selectedGeneration?.questions,
          correctAnswers: selectedGeneration?.correctAnswers,
          answerExplanations: selectedGeneration?.explanations,
        },

        collectionId: selectedCollection?.id,
      },
    };
  };

  const renderActivePage = () => {
    if (activePage === "collections") {
      return (
        <CollectionsPage
          currentUser={currentUser}
          onOpenCollection={(collection) => {
            setSelectedCollection(collection);
            setShouldGenerateFromCollection(false);
            setSelectedMaterialForGeneration(null);

            setActivePage("collection-details");
          }}
          onGenerateFromCollection={(collection) => {
            setSelectedCollection(collection);
            setSelectedMaterialForGeneration(null);
            setShouldGenerateFromCollection(true);

            setActivePage("generate");
          }}
          onUploadToCollection={(collection) => {
            setSelectedCollection(collection);
            setShouldGenerateFromCollection(false);
            setSelectedMaterialForGeneration(null);

            setActivePage("collection-details");

            setTimeout(() => {
              window.dispatchEvent(
                new CustomEvent("openCollectionUploadModal")
              );
            }, 0);
          }}
        />
      );
    }

    if (activePage === "collection-details") {
      return (
        <CollectionDetailsPage
          collection={selectedCollection}
          onBack={() =>
            setActivePage("collections")
          }
          onGenerateFromCollection={(collection) => {
            setSelectedCollection(collection);
            setSelectedMaterialForGeneration(null);
            setShouldGenerateFromCollection(true);

            setActivePage("generate");
          }}
          onOpenGenerationPreview={(generation) => {
            setSelectedGeneration(generation);
            setActivePage("generation-preview");
          }}
          onUseMaterialForGeneration={(material) => {
            /*
             * This flow generates from one specific material,
             * not from the whole collection.
             */
            setShouldGenerateFromCollection(false);
            setSelectedMaterialForGeneration(material);

            setActivePage("generate");
          }}
        />
      );
    }

    if (activePage === "generation-preview") {
      return (
        <main className="main-content">
          <section className="content-card">
            <button
              className="collection-back-button"
              onClick={() => setActivePage("collection-details")}
            >
              ← Back to Collection
            </button>

            <PreviewSave
              generationResult={buildSavedGenerationPreview()}
              isGenerating={false}
              generationError=""
              selectedFiles={[]}
              settings={{}}
              onRegenerate={() => {}}
            />
          </section>
        </main>
      );
    }

    return (
      <GenerationsPage
        activePage={activePage}
        currentUser={currentUser}
        initialCollection={selectedCollection}
        initialMaterial={selectedMaterialForGeneration}
        generateFromCollection={shouldGenerateFromCollection}
      />
    );
  };

  if (!isDemoLoginDone) {
    return (
      <div className="app">
        <main className="page">
          <p>Loading demo user...</p>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      <TopBar />

      <SideBar
        activePage={activePage}
        setActivePage={handleSetActivePage}
      />

      <div className="page">
        {renderActivePage()}
      </div>
    </div>
  );
}

export default App;