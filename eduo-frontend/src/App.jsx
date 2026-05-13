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

  // Stores the currently logged in demo user returned from the backend.
  const [currentUser, setCurrentUser] = useState(null);

  // Prevents the app from trying to load user-specific data before login is done.
  const [isDemoLoginDone, setIsDemoLoginDone] = useState(false);

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
          return;
        }

        const user = await response.json();
        setCurrentUser(user);
      } catch (error) {
        console.error("Failed to auto-login demo user:", error);
      } finally {
        setIsDemoLoginDone(true);
      }
    };

    loginDemoUser();
  }, []);

  if (!isDemoLoginDone) {
    return (
      <div className="app">
        <main className="page">
          <p>Loading demo user...</p>
        </main>
      </div>
    );
  }

  return (
    <div className="app">
      {/* Top navigation/header area */}
      <TopBar />

      {/* Side navigation. Changes activePage when a menu option is clicked. */}
      <SideBar activePage={activePage} setActivePage={setActivePage} />

      {/* Main page content. The shown content depends on activePage. */}
      <div className="page">
        <MainContent activePage={activePage} currentUser={currentUser} />
      </div>
    </div>
  );
}

export default App;