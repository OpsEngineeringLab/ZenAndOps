import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useRef,
  useMemo,
} from "react";
import keycloak from "../lib/keycloak";

interface JwtClaims {
  sub: string;
  userId: string;
  name: string;
  email: string;
  roles: string[];
  tags: Array<{ key: string; value: string }>;
  permissions: string[];
  iat: number;
  exp: number;
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

// Token refresh interval: check every 30 seconds
const TOKEN_REFRESH_INTERVAL_MS = 30 * 1000;

// Minimum token validity buffer in seconds for updateToken
const TOKEN_MIN_VALIDITY_SECONDS = 60;

/**
 * Parse user claims from keycloak.tokenParsed into the JwtClaims interface.
 * Custom claims (userId, name, email, roles, tags, permissions) are added
 * to the token via Keycloak protocol mappers configured in the realm.
 */
function parseUserClaims(): JwtClaims | null {
  const parsed = keycloak.tokenParsed;
  if (!parsed) return null;

  // Parse tags: may come as a JSON string or already parsed array
  let tags: Array<{ key: string; value: string }> = [];
  if (parsed.tags) {
    if (typeof parsed.tags === "string") {
      try {
        tags = JSON.parse(parsed.tags);
      } catch {
        tags = [];
      }
    } else if (Array.isArray(parsed.tags)) {
      tags = parsed.tags;
    }
  }

  // Parse permissions: may come as a JSON string or already parsed array
  let permissions: string[] = [];
  if (parsed.permissions) {
    if (typeof parsed.permissions === "string") {
      try {
        permissions = JSON.parse(parsed.permissions);
      } catch {
        permissions = [];
      }
    } else if (Array.isArray(parsed.permissions)) {
      permissions = parsed.permissions;
    }
  }

  // Parse roles: may come from custom claim or realm_access
  let roles: string[] = [];
  if (parsed.roles) {
    if (Array.isArray(parsed.roles)) {
      roles = parsed.roles;
    }
  } else if (parsed.realm_access?.roles) {
    roles = parsed.realm_access.roles;
  }

  return {
    sub: parsed.sub ?? "",
    userId: parsed.userId ?? parsed.sub ?? "",
    name: parsed.name ?? "",
    email: parsed.email ?? "",
    roles,
    tags,
    permissions,
    iat: parsed.iat ?? 0,
    exp: parsed.exp ?? 0,
  };
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [initialized, setInitialized] = useState(false);
  const [initError, setInitError] = useState<string | null>(null);
  const [user, setUser] = useState<JwtClaims | null>(null);
  const [authenticated, setAuthenticated] = useState(false);
  const refreshIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Update auth state from keycloak instance
  const updateAuthState = useCallback(() => {
    setAuthenticated(!!keycloak.authenticated);
    setUser(keycloak.authenticated ? parseUserClaims() : null);
  }, []);

  // Initialize keycloak on mount
  useEffect(() => {
    let cancelled = false;

    async function initKeycloak() {
      try {
        const auth = await keycloak.init({
          onLoad: "login-required",
          pkceMethod: "S256",
          checkLoginIframe: false,
        });

        if (cancelled) return;

        if (auth) {
          updateAuthState();
        }

        setInitialized(true);
      } catch (error) {
        if (cancelled) return;
        console.error("Keycloak initialization failed:", error);
        setInitError("Failed to initialize authentication. Please try again.");
        setInitialized(true);
      }
    }

    initKeycloak();

    return () => {
      cancelled = true;
    };
  }, [updateAuthState]);

  // Set up automatic token refresh interval
  useEffect(() => {
    if (!initialized || !keycloak.authenticated) return;

    // Set up keycloak event callbacks
    keycloak.onTokenExpired = () => {
      keycloak.updateToken(TOKEN_MIN_VALIDITY_SECONDS).catch(() => {
        // Token refresh failed, redirect to login
        keycloak.login();
      });
    };

    keycloak.onAuthRefreshSuccess = () => {
      updateAuthState();
    };

    keycloak.onAuthRefreshError = () => {
      // Redirect to Keycloak login on refresh failure
      keycloak.login();
    };

    // Periodic token refresh every 30 seconds with 60-second buffer
    refreshIntervalRef.current = setInterval(() => {
      keycloak
        .updateToken(TOKEN_MIN_VALIDITY_SECONDS)
        .then((refreshed) => {
          if (refreshed) {
            updateAuthState();
          }
        })
        .catch(() => {
          // Token refresh failed, redirect to login
          keycloak.login();
        });
    }, TOKEN_REFRESH_INTERVAL_MS);

    return () => {
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current);
        refreshIntervalRef.current = null;
      }
      keycloak.onTokenExpired = undefined;
      keycloak.onAuthRefreshSuccess = undefined;
      keycloak.onAuthRefreshError = undefined;
    };
  }, [initialized, authenticated, updateAuthState]);

  // login: redirect to Keycloak login page
  // loginId and password params are kept for backward compatibility but ignored
  const login = useCallback(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    async (_loginId: string, _password: string): Promise<void> => {
      await keycloak.login();
    },
    []
  );

  // logoff: terminate SSO session via Keycloak
  const logoff = useCallback(async (): Promise<void> => {
    if (refreshIntervalRef.current) {
      clearInterval(refreshIntervalRef.current);
      refreshIntervalRef.current = null;
    }
    await keycloak.logout();
  }, []);

  // refreshTokens: manually trigger token refresh and return new access token
  const refreshTokens = useCallback(async (): Promise<string | null> => {
    try {
      await keycloak.updateToken(TOKEN_MIN_VALIDITY_SECONDS);
      updateAuthState();
      return keycloak.token ?? null;
    } catch {
      // Token refresh failed, redirect to Keycloak login
      keycloak.login();
      return null;
    }
  }, [updateAuthState]);

  const contextValue = useMemo<AuthContextType>(
    () => ({
      accessToken: keycloak.token ?? null,
      refreshToken: keycloak.refreshToken ?? null,
      user,
      isAuthenticated: authenticated,
      login,
      logoff,
      refreshTokens,
    }),
    [user, authenticated, login, logoff, refreshTokens]
  );

  // Show loading state while keycloak is initializing
  if (!initialized) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Initializing authentication...
          </p>
        </div>
      </div>
    );
  }

  // Show error state if initialization failed
  if (initError) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="text-center">
          <p className="mb-4 text-sm text-red-500">{initError}</p>
          <button
            onClick={() => window.location.reload()}
            className="rounded-lg bg-brand-500 px-4 py-2 text-sm text-white hover:bg-brand-600"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

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
