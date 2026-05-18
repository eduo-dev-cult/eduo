import "./CollectionsGrid.css";

import CollectionCard from "./CollectionCard";

export default function CollectionsGrid({ collections }) {
  return (
    <div className="collections-grid">
      {collections.map((collection) => (
        <CollectionCard
          key={collection.id}
          collection={collection}
        />
      ))}
    </div>
  );
}