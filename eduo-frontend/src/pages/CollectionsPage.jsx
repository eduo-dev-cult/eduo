import { useEffect, useState } from "react";

import "./CollectionsPage.css";

import CollectionsToolbar from "../components/collections/CollectionsToolbar";
import CollectionsGrid from "../components/collections/CollectionsGrid";

import { getCollections } from "../api/collectionsApi";

export default function CollectionsPage({ currentUser }) {
  const [collections, setCollections] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  /*
   * Fetch all collections for the currently logged in user.
   * This replaces the temporary mock collections.
   */
  useEffect(() => {
    const fetchCollections = async () => {
      if (!currentUser?.id) {
        setIsLoading(false);
        return;
      }

      try {
        setIsLoading(true);
        setErrorMessage("");

        const fetchedCollections = await getCollections(currentUser.id);

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

  return (
    <main className="collections-page">
      {/* Page header */}
      <div className="collections-header">
        <div>
          <h1>My Collections</h1>

          <p>
            Manage uploaded material, generated questions and saved
            study setups.
          </p>
        </div>

        <button className="create-collection-button">
          + Create Collection
        </button>
      </div>

      <CollectionsToolbar />

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
        <CollectionsGrid collections={collections} />
      )}
    </main>
  );
}