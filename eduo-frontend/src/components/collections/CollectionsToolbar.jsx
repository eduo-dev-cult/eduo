import "./CollectionsToolbar.css";

export default function CollectionsToolbar({
  sortOption,
  setSortOption,
}) {
  return (
    <div className="collections-toolbar">
      <input
        type="text"
        placeholder="Search collections..."
        className="collections-search"
      />

      <select
        className="collections-sort"
        value={sortOption}
        onChange={(event) => setSortOption(event.target.value)}
      >
        <option value="recent">Recently updated</option>
        <option value="alphabetical">Alphabetical</option>
        <option value="most-generations">Most generations</option>
      </select>
    </div>
  );
}