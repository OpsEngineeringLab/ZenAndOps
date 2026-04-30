import axios, {
  AxiosError,
  InternalAxiosRequestConfig,
} from "axios";
import keycloak from "../lib/keycloak";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_GATEWAY_URL || "",
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor: attach Bearer token from keycloak
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    if (keycloak.token && config.headers) {
      config.headers.Authorization = `Bearer ${keycloak.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: handle 401 with token refresh, 429, 503
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // Handle Gateway rate limit exceeded (429)
    if (error.response?.status === 429) {
      alert("Rate limit exceeded. Please wait a moment and try again.");
      return Promise.reject(error);
    }

    // Handle Gateway service unavailable (503)
    if (error.response?.status === 503) {
      alert("Service is temporarily unavailable. Please try again later.");
      return Promise.reject(error);
    }

    // On 401, attempt token refresh via keycloak
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry
    ) {
      originalRequest._retry = true;
      try {
        await keycloak.updateToken(60);
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${keycloak.token}`;
        }
        return apiClient(originalRequest);
      } catch {
        keycloak.login();
        return Promise.reject(error);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
