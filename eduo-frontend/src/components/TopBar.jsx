import logo from "../assets/eduo-logo.png";
import "./TopBar.css";

function TopBar() {
  return (
    <header className="topbar">
      <div className="topbar-left">
        <img src={logo} alt="Eduo logo" className="logo" />
      </div>

      <div className="topbar-right">
        <button className="icon-button" aria-label="Help">?</button>
        <button className="profile-button" aria-label="Profile"></button>
      </div>
    </header>
  );
}

export default TopBar;