import { useState, useEffect } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import Label from "../form/Label";
import Input from "../form/input/InputField";
import type { UserResponse } from "../../hooks/useUserApi";
import { useRoleApi } from "../../hooks/useRoleApi";
import type { RoleResponse } from "../../hooks/useRoleApi";
import { useTagApi } from "../../hooks/useTagApi";
import type { TagResponse } from "../../hooks/useTagApi";

export interface UserFormData {
  login: string;
  name: string;
  email: string;
  password: string;
  roles: string[];
  tagIds: string[];
  active: boolean;
}

interface UserFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: UserFormData) => Promise<void>;
  user?: UserResponse | null;
}

export default function UserFormModal({ isOpen, onClose, onSubmit, user }: UserFormModalProps) {
  const isEdit = !!user;
  const { listRoles } = useRoleApi();
  const { listTags } = useTagApi();

  const [login, setLogin] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [roles, setRoles] = useState<string[]>([]);
  const [tagIds, setTagIds] = useState<string[]>([]);
  const [active, setActive] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [availableRoles, setAvailableRoles] = useState<RoleResponse[]>([]);
  const [availableTags, setAvailableTags] = useState<TagResponse[]>([]);

  useEffect(() => {
    if (isOpen) {
      setLogin(user?.login ?? "");
      setName(user?.name ?? "");
      setEmail(user?.email ?? "");
      setPassword("");
      setRoles(user?.roles ?? []);
      setTagIds(user?.tagIds ?? []);
      setActive(user?.active ?? true);
      setError("");

      // Fetch available roles and tags
      listRoles(0, 100)
        .then((data) => setAvailableRoles(data.items))
        .catch(() => setAvailableRoles([]));
      listTags(0, 100)
        .then((data) => setAvailableTags(data.items))
        .catch(() => setAvailableTags([]));
    }
  }, [isOpen, user, listRoles, listTags]);

  const handleRoleToggle = (roleName: string) => {
    setRoles((prev) =>
      prev.includes(roleName)
        ? prev.filter((r) => r !== roleName)
        : [...prev, roleName]
    );
  };

  const handleTagToggle = (tagId: string) => {
    setTagIds((prev) =>
      prev.includes(tagId)
        ? prev.filter((t) => t !== tagId)
        : [...prev, tagId]
    );
  };

  const handleSubmit = async () => {
    if (!isEdit && !login.trim()) {
      setError("Login is required.");
      return;
    }
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    if (!email.trim()) {
      setError("Email is required.");
      return;
    }
    if (!isEdit && !password.trim()) {
      setError("Password is required.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await onSubmit({
        login: login.trim(),
        name: name.trim(),
        email: email.trim(),
        password: password,
        roles,
        tagIds,
        active,
      });
      onClose();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} className="max-w-lg p-6 lg:p-8">
      <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">
        {isEdit ? "Edit User" : "Create User"}
      </h4>

      <div className="space-y-4 max-h-[70vh] overflow-y-auto pr-1">
        {!isEdit && (
          <div>
            <Label htmlFor="user-login">Login</Label>
            <Input
              id="user-login"
              placeholder="e.g. john.doe"
              value={login}
              onChange={(e) => setLogin(e.target.value)}
              disabled={loading}
            />
          </div>
        )}

        <div>
          <Label htmlFor="user-name">Name</Label>
          <Input
            id="user-name"
            placeholder="e.g. John Doe"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={loading}
          />
        </div>

        <div>
          <Label htmlFor="user-email">Email</Label>
          <Input
            id="user-email"
            placeholder="e.g. john@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={loading}
          />
        </div>

        <div>
          <Label htmlFor="user-password">
            Password{isEdit ? " (leave blank to keep current)" : ""}
          </Label>
          <Input
            id="user-password"
            type="password"
            placeholder={isEdit ? "••••••••" : "Enter password"}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            disabled={loading}
          />
        </div>

        <div>
          <Label>Roles</Label>
          <div className="mt-2 space-y-2 rounded-lg border border-gray-200 p-3 dark:border-gray-700">
            {availableRoles.length === 0 ? (
              <p className="text-sm text-gray-400">No roles available</p>
            ) : (
              availableRoles.map((role) => (
                <label
                  key={role.id}
                  className="flex cursor-pointer items-center gap-2 text-sm text-gray-700 dark:text-gray-300"
                >
                  <input
                    type="checkbox"
                    checked={roles.includes(role.name)}
                    onChange={() => handleRoleToggle(role.name)}
                    disabled={loading}
                    className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500 dark:border-gray-600"
                  />
                  {role.name}
                </label>
              ))
            )}
          </div>
        </div>

        <div>
          <Label>Tags</Label>
          <div className="mt-2 space-y-2 rounded-lg border border-gray-200 p-3 dark:border-gray-700 max-h-40 overflow-y-auto">
            {availableTags.length === 0 ? (
              <p className="text-sm text-gray-400">No tags available</p>
            ) : (
              availableTags.map((tag) => (
                <label
                  key={tag.id}
                  className="flex cursor-pointer items-center gap-2 text-sm text-gray-700 dark:text-gray-300"
                >
                  <input
                    type="checkbox"
                    checked={tagIds.includes(tag.id)}
                    onChange={() => handleTagToggle(tag.id)}
                    disabled={loading}
                    className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500 dark:border-gray-600"
                  />
                  {tag.key}:{tag.value}
                </label>
              ))
            )}
          </div>
        </div>

        {isEdit && (
          <div>
            <Label>Active Status</Label>
            <label
              className="mt-2 flex cursor-pointer select-none items-center gap-3 text-sm font-medium text-gray-700 dark:text-gray-400"
              onClick={() => !loading && setActive(!active)}
            >
              <div className="relative">
                <div
                  className={`block h-6 w-11 rounded-full transition duration-150 ease-linear ${
                    active
                      ? "bg-brand-500"
                      : "bg-gray-200 dark:bg-white/10"
                  }`}
                ></div>
                <div
                  className={`absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-white shadow-theme-sm duration-150 ease-linear transform ${
                    active ? "translate-x-full" : "translate-x-0"
                  }`}
                ></div>
              </div>
              {active ? "Active" : "Inactive"}
            </label>
          </div>
        )}

        {error && (
          <p className="text-sm text-error-500">{error}</p>
        )}
      </div>

      <div className="mt-6 flex items-center justify-end gap-3">
        <Button variant="outline" size="sm" onClick={onClose} disabled={loading}>
          Cancel
        </Button>
        <Button size="sm" onClick={handleSubmit} disabled={loading}>
          {loading ? "Saving..." : isEdit ? "Update" : "Create"}
        </Button>
      </div>
    </Modal>
  );
}
