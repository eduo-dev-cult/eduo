import Stepper from "./Stepper";
import UploadBox from "./UploadBox";

export default function MainContent() {
  return (
    <main className="main-content">
      <section className="content-card">
        <div className="title-section">
          <h1>Create questions from material</h1>
          <p>Upload or paste material and let Eduo create questions based on it</p>
        </div>

        <Stepper />

        <UploadBox />

        <div className="actions">
          <button className="continue-button">Continue</button>
        </div>
      </section>
    </main>
  );
}