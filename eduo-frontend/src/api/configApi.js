const API_BASE_URL = "http://localhost:8080";

// Fetched once and reused for the lifetime of the page.
let configPromise = null;

export function fetchConfig() {
  if (!configPromise) {
    configPromise = fetch(`${API_BASE_URL}/config`).then((res) => res.json());
  }
  return configPromise;
}