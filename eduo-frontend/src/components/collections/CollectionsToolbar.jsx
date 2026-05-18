import "./CollectionsToolbar.css";

export default function CollectionsToolbar({
  sortOption,
  setSortOption,
  searchQuery,
  setSearchQuery,
}) {
  return (
    <div className="collections-toolbar">
      <input
        type="text"
        placeholder="Search collections..."
        className="collections-search"
        value={searchQuery}
        onChange={(event) =>
          setSearchQuery(event.target.value)
        }
      />

      <select
        className="collections-sort"
        value={sortOption}
        onChange={(event) =>
          setSortOption(event.target.value)
        }
      >
        <option value="recent">
          Recently updated
        </option>

        <option value="alphabetical">
          Alphabetical
        </option>

        <option value="most-generations">
          Most generations
        </option>
      </select>
    </div>
  );
}