import logo from "../assets/eduo-logo.png";

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div>
        <img src={logo} alt="Eduo logo" className="sidebar-logo" />

        <nav className="nav">
          <button className="nav-button secondary">Overview</button>
          <button className="nav-button active">My Collections</button>
          <button className="nav-button active">Material</button>
          <button className="nav-button active">Settings</button>
        </nav>
      </div>

      <button className="nav-button sign-out">Sign Out</button>
    </aside>
  );
}