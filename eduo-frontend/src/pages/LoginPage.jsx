import { useState } from "react";

import "./LoginPage.css";

const TEST_USERS = [
  { label: "Alice", username: "alisve-5", password: "placeholder" },
  { label: "Bob", username: "boblin-3", password: "placeholder" },
  { label: "Demo", username: "demo", password: "demo" },
];

export default function LoginPage({ onLogin }) {
  const [isLoggingIn, setIsLoggingIn] = useState(false);
  const [error, setError] = useState("");

  const handleSelectUser = async (user) => {
    setIsLoggingIn(true);
    setError("");

    try {
      const response = await fetch("http://localhost:8080/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: user.username,
          password: user.password,
        }),
      });

      if (!response.ok) {
        setError(
          `Login failed for ${user.label} (status ${response.status}). Is the backend running and seeded?`
        );
        return;
      }

      const loggedInUser = await response.json();
      onLogin(loggedInUser);
    } catch (err) {
      console.error("Login request failed:", err);
      setError(
        "Login failed — is the backend running and seeded?"
      );
    } finally {
      setIsLoggingIn(false);
    }
  };

  return (
    <main className="login-page">
      <section className="login-card">
        <h1 className="login-title">Select a test user</h1>

        <div className="login-user-buttons">
          {TEST_USERS.map((user) => (
            <button
              key={user.username}
              type="button"
              className="login-user-button"
              onClick={() => handleSelectUser(user)}
              disabled={isLoggingIn}
            >
              {user.label}
            </button>
          ))}
        </div>

        {error && <p className="login-error">{error}</p>}
      </section>
    </main>
  );
}
