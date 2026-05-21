// Base URL for the backend API.
// Change this if the backend URL or port changes.
const API_BASE_URL = "http://localhost:8080";

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
 * Updates user preferences.
 *
 * Backend endpoint:
 * PUT /users/{id}/preferences
 */
export async function updateUserPreferences(userId, preferencesData) {
    const response = await fetch(
        `${API_BASE_URL}/users/${userId}/preferences`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(preferencesData),
        }
    );

    return handleResponse(
        response,
        "Failed to update user preferences"
    );
}

/**
 * loads user preferences
 *
 * Backend endpoint:
 * GET /users/{id}/preferences
 */
export async function loadUserPreferences(userId){
    const response = await fetch(
        `${API_BASE_URL}/users/${userId}/preferences`
    );

    return handleResponse(
        response,
        "Failed to get user preferences"
    );
}
