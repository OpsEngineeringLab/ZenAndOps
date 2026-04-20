import { useState, useEffect } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import Label from "../form/Label";
import Input from "../form/input/InputField";
import TextArea from "../form/input/TextArea";
import type { RoleResponse } from "../../hooks/useRoleApi";

const AVAILABLE_PERMISSIONS = [
  "users:read",
  "users:write",
  "roles:read",
  "roles:write",
  "tags:read",
  "tags:write",
  "profile:read",
  "profile:write",
  "dashboard:read",
];

interface RoleFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { name: string; description: string; permissions: string[] }) => Promise<void>;
  role?: RoleResponse | null;
}

export default function RoleFormModal({ isOpen, onClose, onSubmit, role }: RoleFormModalProps) {
  const isEdit = !!role;
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isOpen) {
      setName(role?.name ?? "");
      setDescription(role?.description ?? "");
      setPermissions(role?.permissions ?? []);
      setError("");
    }
  }, [isOpen, role]);

  const handlePermissionToggle = (permission: string) => {
    setPermissions((prev) =>
      prev.includes(permission)
        ? prev.filter((p) => p !== permission)
        : [...prev, permission]
    );
  };

  const handleSubmit = async () => {
    if (!name.trim()) {
      setError("Name is required.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await onSubmit({
        name: name.trim(),
        description: description.trim(),
        permissions,
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
    <Modal isOpen={isOpen} onClose={onClose} className="max-w-md p-6 lg:p-8">
      <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">
        {isEdit ? "Edit Role" : "Create Role"}
      </h4>

      <div className="space-y-4">
        <div>
          <Label htmlFor="role-name">Name</Label>
          <Input
            id="role-name"
            placeholder="e.g. ADMIN"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={loading}
          />
        </div>

        <div>
          <Label htmlFor="role-description">Description</Label>
          <TextArea
            placeholder="Optional description"
            value={description}
            onChange={(val) => setDescription(val)}
            disabled={loading}
            rows={3}
          />
        </div>

        <div>
          <Label>Permissions</Label>
          <div className="mt-2 space-y-2 rounded-lg border border-gray-200 p-3 dark:border-gray-700">
            {AVAILABLE_PERMISSIONS.map((permission) => (
              <label
                key={permission}
                className="flex cursor-pointer items-center gap-2 text-sm text-gray-700 dark:text-gray-300"
              >
                <input
                  type="checkbox"
                  checked={permissions.includes(permission)}
                  onChange={() => handlePermissionToggle(permission)}
                  disabled={loading}
                  className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500 dark:border-gray-600"
                />
                {permission}
              </label>
            ))}
          </div>
        </div>

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
