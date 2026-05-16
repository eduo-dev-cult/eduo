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
 * Uploads one file/material to a specific collection.
 *
 * Backend endpoint:
 * POST /collections/{collectionId}/materials
 *
 * FormData is used because file uploads are sent as multipart/form-data,
 * not as normal JSON.
 */
export async function uploadMaterial(collectionId, file) {
  const formData = new FormData();

  // The key "file" must match the parameter name expected by the backend.
  formData.append("file", file);

  const response = await fetch(
    `${API_BASE_URL}/collections/${collectionId}/materials`,
    {
      method: "POST",
      body: formData,
    }
  );

  return handleResponse(
    response,
    "Failed to upload material"
  );
}

/**
 * Uploads several files/materials to a specific collection.
 *
 * This calls uploadMaterial once per file and waits for all uploads to finish.
 * The returned value is an array with metadata for the uploaded materials.
 */
export async function uploadMaterials(collectionId, files) {
  const uploadPromises = Array.from(files).map((file) =>
    uploadMaterial(collectionId, file)
  );

  return Promise.all(uploadPromises);
}

/*
 * Fetches all materials that already exist
 * in the selected collection.
 */
export async function getMaterialsByCollection(collectionId) {
  const response = await fetch(
    `http://localhost:8080/collections/${collectionId}`
  );

  if (!response.ok) {
    throw new Error(
      `Failed to load materials (Status: ${response.status})`
    );
  }

  const collection = await response.json();

  /*
   * The backend might return materials in different fields due to historical reasons
   * Fallbacks make the frontend more tolerant.
   */
  return (
    collection.materials ??
    collection.sourceMaterials ??
    collection.sourceMaterial ??
    []
  );
}