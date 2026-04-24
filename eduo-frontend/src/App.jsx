import Sidebar from "./components/SideBar";
import TopBar from "./components/TopBar";
import MainContent from "./components/MainContent";
import "./App.css";

function App() {
  return (
    <div className="app">
      <Sidebar />

      <div className="page">
        <TopBar />
        <MainContent />
      </div>
    </div>
  );
}

export default App;