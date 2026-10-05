const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const AUTH_STORAGE_KEY = "inkwell_auth";

function readStoredToken() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw);
    return parsed?.token || null;
  } catch {
    return null;
  }
}

export async function apiRequest(path, options = {}) {
  const {
    token,
    isFormData = false,
    headers: customHeaders = {},
    trackLoader,
    loaderLabel,
    silentErrors = false,
    showSuccessToast,
    ...rest
  } = options;
  const method = String(rest.method || "GET").toUpperCase();
  const shouldTrackLoader =
    typeof trackLoader === "boolean"
      ? trackLoader
      : method !== "GET" && method !== "HEAD";
  const shouldShowSuccessToast =
    typeof showSuccessToast === "boolean"
      ? showSuccessToast
      : method !== "GET" && method !== "HEAD";
  const authToken = typeof token === "string" ? token : token === false ? null : readStoredToken();
  const headers = { ...customHeaders };

  if (!isFormData) {
    headers["Content-Type"] = headers["Content-Type"] || "application/json";
  }
  if (authToken) {
    headers.Authorization = `Bearer ${authToken}`;
  }

  if (typeof window !== "undefined") {
    window.dispatchEvent(
      new CustomEvent("inkwell:api:start", {
        detail: {
          path,
          method,
          trackLoader: shouldTrackLoader,
          label: loaderLabel || "Please wait, processing...",
        },
      })
    );
  }

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      headers,
      ...rest,
    });

    let payload = null;
    const text = await response.text();
    if (text) {
      try {
        payload = JSON.parse(text);
      } catch {
        payload = { message: text };
      }
    }

    if (!response.ok) {
      const message =
        payload?.message || payload?.error || `Request failed: ${response.status}`;
      if (!silentErrors && typeof window !== "undefined") {
        window.dispatchEvent(
          new CustomEvent("inkwell:notify", {
            detail: { type: "error", title: "Error", message },
          })
        );
      }
      const requestError = new Error(message);
      requestError.status = response.status;
      requestError.payload = payload;
      requestError.inkwellNotified = true;
      throw requestError;
    }

    if (
      shouldShowSuccessToast &&
      payload?.message &&
      typeof window !== "undefined"
    ) {
      window.dispatchEvent(
        new CustomEvent("inkwell:notify", {
          detail: { type: "success", title: "Success", message: payload.message },
        })
      );
    }

    return payload;
  } catch (error) {
    if (
      !silentErrors &&
      typeof window !== "undefined" &&
      !error?.inkwellNotified
    ) {
      window.dispatchEvent(
        new CustomEvent("inkwell:notify", {
          detail: {
            type: "error",
            title: "Error",
            message: error?.message || "Unexpected network error",
          },
        })
      );
    }
    throw error;
  } finally {
    if (typeof window !== "undefined") {
      window.dispatchEvent(
        new CustomEvent("inkwell:api:end", {
          detail: { path, method, trackLoader: shouldTrackLoader },
        })
      );
    }
  }
}

export { API_BASE };
