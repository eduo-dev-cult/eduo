import "./App.css";
import SideBar from "./components/SideBar";
import TopBar from "./components/TopBar";
import MainContent from "./components/MainContent";

function App() {
  return (
    <div className="app">
      <TopBar />

      <SideBar />

      <div className="page">
        <MainContent />
      </div>
    </div>
  );
}

export default App;