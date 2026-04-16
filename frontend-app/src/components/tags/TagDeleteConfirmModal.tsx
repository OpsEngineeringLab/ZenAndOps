import { useState } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import type { TagResponse } from "../../hooks/useTagApi";

interface TagDeleteConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: (id: string) => Promise<void>;
  tag: TagResponse | null;
}

export default function TagDeleteConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  tag,
}: TagDeleteConfirmModalProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleDelete = async () => {
    if (!tag) return;
    setLoading(true);
    setError("");
    try {
      await onConfirm(tag.id);
      onClose();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } } };
      if (axiosErr?.response?.status === 409) {
        setError(axiosErr.response.data?.error?.message ?? "This tag is currently assigned to one or more users and cannot be deleted.");
      } else {
        setError(axiosErr?.response?.data?.error?.message ?? "An error occurred while deleting the tag.");
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
        Delete Tag
      </h4>
      <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
        Are you sure you want to delete the tag{" "}
        <span className="font-medium text-gray-700 dark:text-white/80">
          {tag?.key}:{tag?.value}
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
