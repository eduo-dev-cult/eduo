import "./CollectionsGrid.css";

import CollectionCard from "./CollectionCard";

export default function CollectionsGrid({
  collections,
  onOpenCollection,
  onGenerateFromCollection,
  onUploadToCollection,
}) {
  return (
    <div className="collections-grid">
      {collections.map((collection) => (
        <CollectionCard
          key={collection.id}
          collection={collection}
          onOpenCollection={onOpenCollection}
          onGenerateFromCollection={
            onGenerateFromCollection
          }
          onUploadToCollection={
            onUploadToCollection
          }
        />
      ))}
    </div>
  );
}