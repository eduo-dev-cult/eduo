import { useEffect, useState } from "react";

import "./App.css";
import SideBar from "./components/SideBar";
import TopBar from "./components/TopBar";
import MainContent from "./components/MainContent";
import "./styles/Variables.css";
import "./styles/Global.css";
import "./styles/Buttons.css";

function App() {
  // Keeps track of which main page/view is currently shown in the app.
  const [activePage, setActivePage] = useState("generate");

  /*
   * Temporary demo login.
   *
   * This runs once when the frontend starts and logs in the demo user
   * through the regular backend login endpoint.
   *
   * Later, when a real login page is used, this useEffect can be removed.
   */
  useEffect(() => {
    const loginDemoUser = async () => {
      try {
        const response = await fetch("http://localhost:8080/users/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          credentials: "include",
          body: JSON.stringify({
            username: "demo",
            password: "demo",
          }),
        });

        if (!response.ok) {
          console.error("Demo login failed with status:", response.status);
        }
      } catch (error) {
        console.error("Failed to auto-login demo user:", error);
      }
    };

    loginDemoUser();
  }, []);

  return (
    <div className="app">
      {/* Top navigation/header area */}
      <TopBar />

      {/* Side navigation. Changes activePage when a menu option is clicked. */}
      <SideBar activePage={activePage} setActivePage={setActivePage} />

      {/* Main page content. The shown content depends on activePage. */}
      <div className="page">
        <MainContent activePage={activePage} />
      </div>
    </div>
  );
}

export default App;