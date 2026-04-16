import { useState, useEffect } from "react";
import { Modal } from "../ui/modal";
import Button from "../ui/button/Button";
import Label from "../form/Label";
import Input from "../form/input/InputField";
import TextArea from "../form/input/TextArea";
import type { TagResponse } from "../../hooks/useTagApi";

interface TagFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: { key: string; value: string; description: string }) => Promise<void>;
  tag?: TagResponse | null;
}

export default function TagFormModal({ isOpen, onClose, onSubmit, tag }: TagFormModalProps) {
  const isEdit = !!tag;
  const [key, setKey] = useState("");
  const [value, setValue] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (isOpen) {
      setKey(tag?.key ?? "");
      setValue(tag?.value ?? "");
      setDescription(tag?.description ?? "");
      setError("");
    }
  }, [isOpen, tag]);

  const handleSubmit = async () => {
    if (!isEdit && (!key.trim() || !value.trim())) {
      setError("Key and value are required.");
      return;
    }
    setLoading(true);
    setError("");
    try {
      await onSubmit({ key: key.trim(), value: value.trim(), description: description.trim() });
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
        {isEdit ? "Edit Tag" : "Create Tag"}
      </h4>

      <div className="space-y-4">
        <div>
          <Label htmlFor="tag-key">Key</Label>
          <Input
            id="tag-key"
            placeholder="e.g. department"
            value={key}
            onChange={(e) => setKey(e.target.value)}
            disabled={loading || isEdit}
          />
        </div>

        <div>
          <Label htmlFor="tag-value">Value</Label>
          <Input
            id="tag-value"
            placeholder="e.g. engineering"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            disabled={loading || isEdit}
          />
        </div>

        <div>
          <Label htmlFor="tag-description">Description</Label>
          <TextArea
            placeholder="Optional description"
            value={description}
            onChange={(val) => setDescription(val)}
            disabled={loading}
            rows={3}
          />
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
