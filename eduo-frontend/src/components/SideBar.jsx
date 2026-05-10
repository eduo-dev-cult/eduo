import "./SideBar.css";

export default function SideBar({ activePage, setActivePage }) {
  return (
    <aside className="sidebar">
      <nav className="nav">
        <button
          type="button"
          className={`sidebar-button ${
            activePage === "generate" ? "active" : ""
          }`}
          onClick={() => setActivePage("generate")}
        >
          Generate
        </button>

        <button
          type="button"
          className={`sidebar-button ${
            activePage === "collections" ? "active" : ""
          }`}
          onClick={() => setActivePage("collections")}
        >
          My Collections
        </button>

        <button
          type="button"
          className={`sidebar-button ${
            activePage === "material" ? "active" : ""
          }`}
          onClick={() => setActivePage("material")}
        >
          Material
        </button>

        <button
          type="button"
          className={`sidebar-button ${
            activePage === "preferences" ? "active" : ""
          }`}
          onClick={() => setActivePage("preferences")}
        >
          Preferences
        </button>
      </nav>

      <button
        type="button"
        className="sidebar-button sign-out"
        onClick={() => alert("You have been signed out")}
      >
        Sign Out
      </button>
    </aside>
  );
}