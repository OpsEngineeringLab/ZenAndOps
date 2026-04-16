import { useAuth } from "../context/AuthContext";

/**
 * Check if the user has at least one of the required roles.
 */
export function useHasRole(...requiredRoles: string[]): boolean {
  const { user } = useAuth();
  if (!user || !user.roles) return false;
  return requiredRoles.some((role) => user.roles.includes(role));
}

/**
 * Check if the user has all of the required tag key-value pairs.
 */
export function useHasAttributes(
  requiredAttributes: Record<string, string>
): boolean {
  const { user } = useAuth();
  if (!user || !user.tags) return false;
  return Object.entries(requiredAttributes).every(([key, value]) =>
    user.tags.some((tag) => tag.key === key && tag.value === value)
  );
}

/**
 * Combined RBAC + ABAC check.
 * Pass roles, attributes, or both. All provided conditions must be satisfied.
 * Attributes are matched against the user's tags array (key-value pairs).
 */
export function useIsAuthorized(options: {
  roles?: string[];
  attributes?: Record<string, string>;
}): boolean {
  const { user } = useAuth();
  if (!user) return false;

  if (options.roles && options.roles.length > 0) {
    const hasRole = options.roles.some((role) => user.roles?.includes(role));
    if (!hasRole) return false;
  }

  if (options.attributes) {
    const hasAttrs = Object.entries(options.attributes).every(([key, value]) =>
      user.tags?.some((tag) => tag.key === key && tag.value === value)
    );
    if (!hasAttrs) return false;
  }

  return true;
}
