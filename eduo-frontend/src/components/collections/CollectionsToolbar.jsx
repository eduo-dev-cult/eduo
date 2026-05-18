import "./CollectionsToolbar.css";

export default function CollectionsToolbar() {
  return (
    <div className="collections-toolbar">
      {/* Search input */}
      <input
        type="text"
        placeholder="Search collections..."
        className="collections-search"
      />

      {/* Sort dropdown */}
      <select className="collections-sort">
        <option>Recently updated</option>
        <option>Alphabetical</option>
        <option>Most generations</option>
      </select>
    </div>
  );
}