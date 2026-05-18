const API_BASE_URL = "http://localhost:8080";

// Fetched once and reused for the lifetime of the page.
let configPromise = null;

export function fetchConfig() {
  if (!configPromise) {
    configPromise = fetch(`${API_BASE_URL}/config`)
      .then((res) => {
        if (!res.ok) {
          throw new Error(`Failed to load config (HTTP ${res.status}).`);
        }
        return res.json();
      })
      .catch((err) => {
        // Drop the cached rejection so the next call retries instead of
        // permanently disabling client-side validation.
        configPromise = null;
        throw err;
      });
  }
  return configPromise;
}
