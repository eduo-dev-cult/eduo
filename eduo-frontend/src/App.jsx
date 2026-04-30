import { useState } from "react";

import "./App.css";
import SideBar from "./components/SideBar";
import TopBar from "./components/TopBar";
import MainContent from "./components/MainContent";
import "./styles/Variables.css";
import "./styles/Global.css";
import "./styles/Buttons.css";
import "./App.css";

function App() {
  const [activePage, setActivePage] = useState("generate");

  return (
    <div className="app">
      <TopBar />

      <SideBar activePage={activePage} setActivePage={setActivePage} />

      <div className="page">
        <MainContent activePage={activePage} />
      </div>
    </div>
  );
}

export default App;