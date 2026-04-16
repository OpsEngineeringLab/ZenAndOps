import axios, {
  AxiosError,
  InternalAxiosRequestConfig,
} from "axios";

const STORAGE_KEY_ACCESS = "zenandops_access_token";
const STORAGE_KEY_REFRESH = "zenandops_refresh_token";

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_GATEWAY_URL || "",
  headers: {
    "Content-Type": "application/json",
  },
});

// Request interceptor: attach Bearer token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(STORAGE_KEY_ACCESS);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Track if a token refresh is already in progress to avoid concurrent refreshes
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string | null) => void;
  reject: (error: unknown) => void;
}> = [];

function processQueue(error: unknown, token: string | null = null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
}

// Response interceptor: handle gateway errors and 401 token refresh
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

    // Only attempt refresh for 401 errors on non-auth endpoints
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !originalRequest.url?.includes("/api/v1/auth/login") &&
      !originalRequest.url?.includes("/api/v1/auth/refresh")
    ) {
      if (isRefreshing) {
        // Queue this request until the refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string | null) => {
              if (token && originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${token}`;
              }
              resolve(apiClient(originalRequest));
            },
            reject,
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem(STORAGE_KEY_REFRESH);
        if (!refreshToken) {
          throw new Error("No refresh token available");
        }

        const response = await axios.post("/api/v1/auth/refresh", {
          refreshToken,
        });

        const { accessToken, refreshToken: newRefreshToken } = response.data;
        localStorage.setItem(STORAGE_KEY_ACCESS, accessToken);
        localStorage.setItem(STORAGE_KEY_REFRESH, newRefreshToken);

        processQueue(null, accessToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        }
        return apiClient(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        // Clear tokens and let the app redirect to login
        localStorage.removeItem(STORAGE_KEY_ACCESS);
        localStorage.removeItem(STORAGE_KEY_REFRESH);
        window.location.href = "/login";
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;
