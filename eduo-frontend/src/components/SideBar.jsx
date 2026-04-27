import "./SideBar.css";

export default function SideBar({ activePage, setActivePage }) {
  return (
    <aside className="sidebar">
      <div>
        <nav className="nav">
          <button
            className={`primary-button sidebar-button nav-button ${
              activePage === "generate" ? "active" : ""
            }`}
            onClick={() => setActivePage("generate")}
          >
            Generate
          </button>

          <button
            className={`primary-button sidebar-button nav-button ${
              activePage === "collections" ? "active" : ""
            }`}
            onClick={() => setActivePage("collections")}
          >
            My Collections
          </button>

          <button
            className={`primary-button sidebar-button nav-button ${
              activePage === "material" ? "active" : ""
            }`}
            onClick={() => setActivePage("material")}
          >
            Material
          </button>

          <button
            className={`primary-button sidebar-button nav-button ${
              activePage === "settings" ? "active" : ""
            }`}
            onClick={() => setActivePage("settings")}
          >
            Settings
          </button>
        </nav>
      </div>

      <button
        className="primary-button sidebar-button sign-out"
        onClick={() => {
          alert("You have been signed out");
        }}
      >
        Sign Out
      </button>
    </aside>
  );
}