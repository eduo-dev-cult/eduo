import { useState } from "react";
import SettingsPanel from "./SettingsPanel";

const STORAGE_KEY = "eduo_generation_preferences";

const defaultGenerationSettings = {
  questionTypes: ["multipleChoice"],
  numberOfQuestions: 10,
  collectionId: "default",
  focusArea: "entireMaterial",
  specificTopics: "",
  difficulty: ["Medium"],
  outputContent: {
    questions: true,
    correctAnswers: true,
    answerExplanations: false,
  },
};

function getPreferences() {
  const saved = localStorage.getItem(STORAGE_KEY);

  if (!saved) return defaultGenerationSettings;

  try {
    const parsed = JSON.parse(saved);

    return {
      ...defaultGenerationSettings,
      ...parsed,
      outputContent: {
        ...defaultGenerationSettings.outputContent,
        ...parsed.outputContent,
        questions: true,
      },
    };
  } catch {
    return defaultGenerationSettings;
  }
}

function savePreferences(preferences) {
  localStorage.setItem(
    STORAGE_KEY,
    JSON.stringify({
      ...preferences,
      outputContent: {
        ...preferences.outputContent,
        questions: true,
      },
    })
  );
}

export default function GenerationPreferences() {
  const [preferences, setPreferences] = useState(() => getPreferences());
  const [message, setMessage] = useState("");

  const handleSave = () => {
    savePreferences(preferences);
    setMessage("Preferences saved.");

    setTimeout(() => {
      setMessage("");
    }, 2000);
  };

  return (
    <>
      <div className="title-section">
        <h1>Preferences</h1>
        <p>
          Set your preferred generation options. These will be used as defaults when you generate 
          new questions, but can be adjusted on a per-generation basis.
        </p>
      </div>

      <div className="step-content step-content-center">
        <SettingsPanel settings={preferences} setSettings={setPreferences} />
      </div>

      <div className="actions">
        <button
          type="button"
          className="button primary-button"
          onClick={handleSave}
        >
          Save Preferences
        </button>
      </div>

      {message && (
        <p style={{ textAlign: "center", marginTop: "10px", fontWeight: "600" }}>
          {message}
        </p>
      )}
    </>
  );
}