import { useEffect, useState } from "react";

import "./CollectionsPage.css";

import CollectionsToolbar from "../components/collections/CollectionsToolbar";
import CollectionsGrid from "../components/collections/CollectionsGrid";

import {
  getCollections,
  createCollection,
} from "../api/collectionsApi";

function getUserId(user) {
  return user?.id ?? user?.userId;
}

export default function CollectionsPage({
  currentUser,
  onOpenCollection,
  onGenerateFromCollection,
  onUploadToCollection,
}) {
  const [collections, setCollections] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  // Controls the create collection modal.
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  // Stores form values for the new collection.
  const [newCollectionName, setNewCollectionName] = useState("");
  const [newCollectionDescription, setNewCollectionDescription] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  // Stores the current sorting option for the collections.
  const [sortOption, setSortOption] = useState("recent");

  // Stores the current search query for filtering collections.
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    const fetchCollections = async () => {
      const userId = getUserId(currentUser);

      if (!userId) {
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setErrorMessage("");

        const fetchedCollections = await getCollections(userId);

        setCollections(fetchedCollections);
      } catch (error) {
        console.error("Failed to load collections:", error);
        setErrorMessage("Could not load collections.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchCollections();
  }, [currentUser]);

  const handleCreateCollection = async (event) => {
    event.preventDefault();

    const userId = getUserId(currentUser);

    if (!userId || !newCollectionName.trim()) {
      return;
    }

    try {
      setIsCreating(true);
      setErrorMessage("");

      const createdCollection = await createCollection({
        userId,
        name: newCollectionName.trim(),
        description: newCollectionDescription.trim(),
      });

      setCollections((previousCollections) => [
        createdCollection,
        ...previousCollections,
      ]);

      setNewCollectionName("");
      setNewCollectionDescription("");
      setIsCreateModalOpen(false);
    } catch (error) {
      console.error("Failed to create collection:", error);
      setErrorMessage("Could not create collection.");
    } finally {
      setIsCreating(false);
    }
  };

  const filteredCollections = collections.filter((collection) => {
    const name = collection.name || "";
    const description = collection.description || "";

    const query = searchQuery.toLowerCase();

    return (
      name.toLowerCase().includes(query) ||
      description.toLowerCase().includes(query)
    );
  });

  const sortedCollections = [...filteredCollections].sort((a, b) => {
    if (sortOption === "alphabetical") {
      return (a.name || "").localeCompare(b.name || "");
    }

    if (sortOption === "most-generations") {
      const aGenerations = Array.isArray(a.generations)
        ? a.generations.length
        : 0;

      const bGenerations = Array.isArray(b.generations)
        ? b.generations.length
        : 0;

      return bGenerations - aGenerations;
    }

    const aDate = new Date(a.updatedAt || a.createdAt || 0);
    const bDate = new Date(b.updatedAt || b.createdAt || 0);

    return bDate - aDate;
  });

  return (
    <main className="collections-page">
      <div className="collections-header">
        <div>
          <h1>My Collections</h1>

          <p>
            Manage uploaded material, generated questions and saved
            study setups.
          </p>
        </div>

        <button
          className="create-collection-button"
          onClick={() => setIsCreateModalOpen(true)}
        >
          + Create Collection
        </button>
      </div>

      <CollectionsToolbar
        sortOption={sortOption}
        setSortOption={setSortOption}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
      />

      {isLoading && (
        <p className="collections-status-text">
          Loading collections...
        </p>
      )}

      {!isLoading && errorMessage && (
        <p className="collections-error-text">
          {errorMessage}
        </p>
      )}

      {!isLoading && !errorMessage && collections.length === 0 && (
        <div className="collections-empty-state">
          <h2>No collections yet</h2>

          <p>
            Create your first collection to organize material and save
            generated questions.
          </p>
        </div>
      )}

      {!isLoading && !errorMessage && collections.length > 0 && (
        <CollectionsGrid
          collections={sortedCollections}
          onOpenCollection={onOpenCollection}
          onGenerateFromCollection={
            onGenerateFromCollection
          }
          onUploadToCollection={
            onUploadToCollection
          }
        />
      )}

      {isCreateModalOpen && (
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
                  setNewCollectionName(event.target.value)
                }
                placeholder="Example: Interaktionsdesign"
              />
            </label>

            <label>
              Description
              <textarea
                value={newCollectionDescription}
                onChange={(event) =>
                  setNewCollectionDescription(event.target.value)
                }
                placeholder="What will this collection be used for?"
              />
            </label>

            <div className="collection-modal-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => setIsCreateModalOpen(false)}
              >
                Cancel
              </button>

              <button
                type="submit"
                className="primary-button"
                disabled={isCreating || !newCollectionName.trim()}
              >
                {isCreating ? "Creating..." : "Create"}
              </button>
            </div>
          </form>
        </div>
      )}
    </main>
  );
}