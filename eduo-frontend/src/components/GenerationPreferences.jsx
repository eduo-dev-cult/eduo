import {useEffect, useState} from "react";
import SettingsPanel from "./generations/SettingsPanel";
import {loadUserPreferences, updateUserPreferences,} from "../api/userApi.js";

//const STORAGE_KEY = "eduo_generation_preferences";

const defaultGenerationSettings = {
  language: "English",
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

async function getPreferences(userId) {
  const parsed = await loadUserPreferences(userId);

  if (!parsed) return defaultGenerationSettings;

  const parsedFormatted = {
    numberOfQuestions: parsed.numOfQuestions ?? defaultGenerationSettings.numberOfQuestions,
    language:
        parsed.language === "SWEDISH"
            ? "Swedish"
            : parsed.language === "ENGLISH"
                ? "English"
                : parsed.language,

    focusArea:
        parsed.focusArea === "KEY_CONCEPTS"
            ? "Key concepts"
            : parsed.focusArea === "TOPICS"
                ? "Specific topics"
                : parsed.focusArea === "ENTIRE_MATERIAL"
                    ? "Entire material"
                    : parsed.focusArea,

    specificTopics: parsed.topics ?? defaultGenerationSettings.specificTopics,

    difficulty: [
      parsed.easy ? "Easy" : null,
      parsed.medium ? "Medium" : null,
      parsed.hard ? "Hard" : null,
    ].filter(Boolean),

    questionTypes: [
      parsed.multipleChoice ? "multipleChoice" : null,
      parsed.openEnded ? "openEnded" : null,
      parsed.trueFalse ? "trueFalse" : null,
    ].filter(Boolean),

    outputContent: {
      questions: parsed.questions,
      correctAnswers: parsed.correctAnswers,
      answerExplanations: parsed.explanations,
    },
  };

  return {
    ...defaultGenerationSettings,
    ...parsedFormatted,
  };
}

async function savePreferences(userId, preferences) {
  var formattedPreferences = {
    numOfQuestions: preferences?.numberOfQuestions,

    language:
        preferences.language === "English"
            ? "ENGLISH"
            : preferences.language === "Swedish"
                ? "SWEDISH"
                : preferences.language,

    focusArea:
        preferences.focusArea === "Entire material"
            ? "ENTIRE_MATERIAL"
            : preferences.focusArea === "Key concepts"
                ? "KEY_CONCEPTS"
                : preferences.focusArea === "Specific topics"
                    ? "TOPICS"
                    : preferences.focusArea,

    topics: preferences.specificTopics ?? preferences.topics ?? null,

    easy: preferences.difficulty?.includes("Easy") ?? false,
    medium: preferences.difficulty?.includes("Medium") ?? false,
    hard: preferences.difficulty?.includes("Hard") ?? false,

    multipleChoice: preferences.questionTypes?.includes("multipleChoice") ?? false,
    openEnded: preferences.questionTypes?.includes("openEnded") ?? false,
    trueFalse: preferences.questionTypes?.includes("trueFalse") ?? false,

    correctAnswers: preferences.outputContent?.correctAnswers ?? false,
    explanations: preferences.outputContent?.answerExplanations ?? false,
    description: false,
    questions: preferences.outputContent?.questions ?? true,
  };

  await updateUserPreferences(userId, formattedPreferences);
}

export default function GenerationPreferences({userId}) {
  const [preferences, setPreferences] = useState( defaultGenerationSettings);
  const [message, setMessage] = useState("");

  // ------hårdkodad userID)--------
  useEffect (()=>{
    getPreferences(2).then(pref=>{
      setPreferences(pref)
    });
  }, [])

  const handleSave = async () => {
    await savePreferences(2, preferences);
    setMessage("Preferences saved.");
  // -----slut-------

  // -----------byt med hårdkodning när userID fungerar--------
  //useEffect(() => {
  //  if (!userId) return;
  //
  //  getPreferences(userId).then(pref => {
  //    setPreferences(pref);
  //  });
  //}, [userId]);

  //const handleSave = async () => {
  //  await savePreferences(userId, preferences);
  //  setMessage("Preferences saved.");
  //------------slut----------


    setTimeout(() => {setMessage("");}, 2000);
  };

  return (
    <>
      <div className="title-section">
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