// Base URL for the backend API.
// Change this if the backend URL or port changes.
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
  if (response.ok) {
    return response.json();
  }

  let errorDetails = {
    status: response.status,
    statusText: response.statusText,
    message: defaultMessage,
  };

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
 * Sends a CreateGenerationRequest to the backend.
 *
 * Backend endpoint:
 * POST /collections/{collectionId}/generations
 *
 * The request body contains:
 * - selected collection id
 * - uploaded material ids
 * - selected generation settings
 */
export async function createGeneration(
  collectionId,
  generationRequest
) {
  const response = await fetch(
    `${API_BASE_URL}/collections/${collectionId}/generations`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(generationRequest),
    }
  );

  return handleResponse(
    response,
    "Failed to create generation"
  );
}