// Base URL for the backend API.
// Change this if the backend URL changes.
const API_BASE_URL = "http://localhost:8080";

/**
 * Handles backend responses in a consistent way.
 *
 * If the request succeeds:
 * - Returns parsed JSON data.
 *
 * If the request fails:
 * - Attempts to extract backend error information.
 * - Throws a detailed Error object.
 */
async function handleResponse(response, defaultMessage) {
  // Successful response
  if (response.ok) {
    return response.json();
  }

  // Default error object
  let errorDetails = {
    status: response.status,
    statusText: response.statusText,
    message: defaultMessage,
  };

  // Try to extract backend error response
  try {
    const errorBody = await response.json();

    errorDetails = {
      ...errorDetails,
      ...errorBody,
    };
  } catch {
    // Ignore JSON parsing errors
  }

  console.error("API Error:", errorDetails);

  throw new Error(
    `${errorDetails.message} (Status: ${errorDetails.status})`
  );
}

/**
 * Fetches all collections belonging to a specific user.
 *
 * Backend endpoint:
 * GET /collections/user/{userId}
 */
export async function getCollections(userId) {
  const response = await fetch(
    `${API_BASE_URL}/collections/user/${userId}`
  );

  return handleResponse(
    response,
    "Failed to fetch collections"
  );
}

/**
 * Fetches one specific collection by ID.
 *
 * Backend endpoint:
 * GET /collections/{collectionId}
 */
export async function getCollection(collectionId) {
  const response = await fetch(
    `${API_BASE_URL}/collections/${collectionId}`
  );

  return handleResponse(
    response,
    "Failed to fetch collection"
  );
}

/**
 * Creates a new collection.
 *
 * Backend endpoint:
 * POST /collections
 */
export async function createCollection(collectionData) {
  const response = await fetch(
    `${API_BASE_URL}/collections`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(collectionData),
    }
  );

  return handleResponse(
    response,
    "Failed to create collection"
  );
}