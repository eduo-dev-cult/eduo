import "./CollectionCard.css";

export default function CollectionCard({ collection }) {
  // Backend data can have different field names than our first mock data.
  const name = collection.name || "Untitled collection";

  const description =
    collection.description || "No description added yet.";

  const files = Array.isArray(collection.sourceMaterials)
    ? collection.sourceMaterials.length
    : collection.files ?? 0;

  const generations = Array.isArray(collection.generations)
    ? collection.generations.length
    : collection.generations ?? 0;

  const rawUpdatedDate =
  collection.updatedAt || collection.createdAt;

  const updated = rawUpdatedDate
    ? new Date(rawUpdatedDate).toLocaleDateString("sv-SE", {
        year: "numeric",
        month: "short",
        day: "numeric",
      })
    : "Unknown";

  return (
    <div className="collection-card">
      {/* Top section */}
      <div className="collection-card-top">
        <div className="collection-icon">📚</div>

        <button className="collection-menu-button">
          ⋮
        </button>
      </div>

      {/* Collection info */}
      <div className="collection-content">
        <h2>{name}</h2>

        <p>{description}</p>
      </div>

      {/* Stats */}
      <div className="collection-stats">
        <span>{files} files</span>

        <span>{generations} generations</span>

        <span>Updated {updated}</span>
      </div>

      {/* Actions */}
      <div className="collection-actions">
        <button className="secondary-button">
          Open
        </button>

        <button className="primary-button">
          Generate
        </button>

        <button className="secondary-button">
          Upload
        </button>
      </div>
    </div>
  );
}