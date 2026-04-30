import { useAuth } from "../../context/AuthContext";
import keycloak from "../../lib/keycloak";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

/**
 * Safety net for unauthenticated access. With keycloak-js configured with
 * onLoad: 'login-required', the user should always be authenticated by the
 * time this component renders. If somehow they are not, redirect to Keycloak
 * login rather than showing a blank page.
 */
export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    keycloak.login();
    return null;
  }

  return <>{children}</>;
}
