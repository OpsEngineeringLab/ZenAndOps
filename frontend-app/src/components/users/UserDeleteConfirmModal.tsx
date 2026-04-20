import { useState } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import type { UserResponse } from "../../hooks/useUserApi";

interface UserDeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (id: string) => Promise<void>;
  user: UserResponse | null;
}

export default function UserDeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  user,
}: UserDeleteConfirmModalProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleDelete = async () => {
    if (!user) return;
    setLoading(true);
    setError("");
    try {
      await onConfirm(user.id);
      onClose();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } } };
      if (axiosErr?.response?.status === 409) {
        setError(axiosErr.response.data?.error?.message ?? "This user cannot be deleted.");
      } else {
        setError(axiosErr?.response?.data?.error?.message ?? "An error occurred while deleting the user.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setError("");
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} className="max-w-sm p-6">
      <h4 className="mb-2 text-lg font-semibold text-gray-800 dark:text-white/90">
        Delete User
      </h4>
      <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
        Are you sure you want to delete the user{" "}
        <span className="font-medium text-gray-700 dark:text-white/80">
          {user?.name}
        </span>
        ? This action cannot be undone.
      </p>

      {error && (
        <div className="mb-4 rounded-lg border border-error-500 bg-error-50 p-3 text-sm text-error-500 dark:border-error-500/30 dark:bg-error-500/15">
          {error}
        </div>
      )}

      <div className="flex items-center justify-end gap-3">
        <Button variant="outline" size="sm" onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button
          size="sm"
          onClick={handleDelete}
          disabled={loading}
          className="bg-error-500 hover:bg-error-600 disabled:bg-error-300"
        >
          {loading ? "Deleting..." : "Delete"}
        </Button>
      </div>
    </Modal>
  );
}
