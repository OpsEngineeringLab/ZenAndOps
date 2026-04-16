import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
  useMemo,
} from "react";
import { jwtDecode } from "jwt-decode";
import axios from "axios";

interface JwtClaims {
  sub: string;
  userId: string;
  name: string;
  email: string;
  roles: string[];
  tags: Array<{ key: string; value: string }>;
  iat: number;
  exp: number;
}

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: JwtClaims | null;
}

interface AuthContextType {
  accessToken: string | null;
  refreshToken: string | null;
  user: JwtClaims | null;
  isAuthenticated: boolean;
  login: (loginId: string, password: string) => Promise<void>;
  logoff: () => Promise<void>;
  refreshTokens: () => Promise<string | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const STORAGE_KEY_ACCESS = "zenandops_access_token";
const STORAGE_KEY_REFRESH = "zenandops_refresh_token";

// Auto-refresh buffer: refresh 60 seconds before expiry
const REFRESH_BUFFER_MS = 60 * 1000;

function decodeToken(token: string): JwtClaims | null {
  try {
    return jwtDecode<JwtClaims>(token);
  } catch {
    return null;
  }
}

function loadInitialState(): AuthState {
  const accessToken = localStorage.getItem(STORAGE_KEY_ACCESS);
  const refreshToken = localStorage.getItem(STORAGE_KEY_REFRESH);

  if (accessToken) {
    const claims = decodeToken(accessToken);
    if (claims && claims.exp * 1000 > Date.now()) {
      return { accessToken, refreshToken, user: claims };
    }
  }

  // If access token is expired but refresh token exists, keep refresh token
  // so auto-refresh can attempt renewal
  if (refreshToken) {
    return { accessToken: null, refreshToken, user: null };
  }

  return { accessToken: null, refreshToken: null, user: null };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>(loadInitialState);
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearTokens = useCallback(() => {
    localStorage.removeItem(STORAGE_KEY_ACCESS);
    localStorage.removeItem(STORAGE_KEY_REFRESH);
    setAuthState({ accessToken: null, refreshToken: null, user: null });
    if (refreshTimerRef.current) {
      clearTimeout(refreshTimerRef.current);
      refreshTimerRef.current = null;
    }
  }, []);

  const storeTokens = useCallback((accessToken: string, refreshToken: string) => {
    localStorage.setItem(STORAGE_KEY_ACCESS, accessToken);
    localStorage.setItem(STORAGE_KEY_REFRESH, refreshToken);
    const user = decodeToken(accessToken);
    setAuthState({ accessToken, refreshToken, user });
  }, []);

  const refreshTokens = useCallback(async (): Promise<string | null> => {
    const currentRefresh = localStorage.getItem(STORAGE_KEY_REFRESH);
    if (!currentRefresh) {
      clearTokens();
      return null;
    }

    try {
      const response = await axios.post("/api/v1/auth/refresh", {
        refreshToken: currentRefresh,
      });

      const { accessToken, refreshToken: newRefreshToken } = response.data;
      storeTokens(accessToken, newRefreshToken);
      return accessToken;
    } catch {
      clearTokens();
      return null;
    }
  }, [clearTokens, storeTokens]);

  const login = useCallback(
    async (loginId: string, password: string): Promise<void> => {
      const response = await axios.post("/api/v1/auth/login", {
        login: loginId,
        password,
      });

      const { accessToken, refreshToken } = response.data;
      storeTokens(accessToken, refreshToken);
    },
    [storeTokens]
  );

  const logoff = useCallback(async (): Promise<void> => {
    const token = localStorage.getItem(STORAGE_KEY_ACCESS);
    try {
      if (token) {
        await axios.post(
          "/api/v1/auth/logoff",
          {},
          { headers: { Authorization: `Bearer ${token}` } }
        );
      }
    } catch {
      // Logoff should always clear local state regardless of server response
    } finally {
      clearTokens();
    }
  }, [clearTokens]);

  // Schedule auto-refresh based on access token expiry
  useEffect(() => {
    if (refreshTimerRef.current) {
      clearTimeout(refreshTimerRef.current);
      refreshTimerRef.current = null;
    }

    if (authState.accessToken && authState.user) {
      const expiresAt = authState.user.exp * 1000;
      const now = Date.now();
      const timeUntilRefresh = expiresAt - now - REFRESH_BUFFER_MS;

      if (timeUntilRefresh > 0) {
        refreshTimerRef.current = setTimeout(() => {
          refreshTokens();
        }, timeUntilRefresh);
      } else if (authState.refreshToken) {
        // Token is about to expire or already expired, refresh immediately
        refreshTokens();
      }
    }

    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current);
      }
    };
  }, [authState.accessToken, authState.user, authState.refreshToken, refreshTokens]);

  // On mount, if we have a refresh token but no valid access token, try to refresh
  useEffect(() => {
    if (!authState.accessToken && authState.refreshToken) {
      refreshTokens();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const contextValue = useMemo<AuthContextType>(
    () => ({
      accessToken: authState.accessToken,
      refreshToken: authState.refreshToken,
      user: authState.user,
      isAuthenticated: !!authState.accessToken && !!authState.user,
      login,
      logoff,
      refreshTokens,
    }),
    [authState, login, logoff, refreshTokens]
  );

  return (
    <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}

export type { JwtClaims, AuthContextType };
