import { useState, useEffect, useCallback } from "react";
import Button from "../ui/button/Button";
import { useUserTagApi } from "../../hooks/useUserTagApi";
import { useTagApi } from "../../hooks/useTagApi";
import type { TagResponse } from "../../hooks/useTagApi";

interface UserTagsSectionProps {
  userId: string;
}

export default function UserTagsSection({ userId }: UserTagsSectionProps) {
  const { getUserTags, assignTags, removeTags } = useUserTagApi();
  const { listTags } = useTagApi();

  const [userTags, setUserTags] = useState<TagResponse[]>([]);
  const [allTags, setAllTags] = useState<TagResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState("");
  const [selectedTagId, setSelectedTagId] = useState("");

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [assigned, available] = await Promise.all([
        getUserTags(userId),
        listTags(0, 100),
      ]);
      setUserTags(assigned);
      setAllTags(available.items);
    } catch {
      setError("Failed to load tags.");
    } finally {
      setLoading(false);
    }
  }, [userId, getUserTags, listTags]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const assignedIds = new Set(userTags.map((t) => t.id));
  const availableTags = allTags.filter((t) => !assignedIds.has(t.id));

  const handleAssign = async () => {
    if (!selectedTagId) return;
    setActionLoading(true);
    setError("");
    try {
      await assignTags(userId, [selectedTagId]);
      setSelectedTagId("");
      await fetchData();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setError(axiosErr?.response?.data?.error?.message ?? "Failed to assign tag.");
    } finally {
      setActionLoading(false);
    }
  };

  const handleRemove = async (tagId: string) => {
    setActionLoading(true);
    setError("");
    try {
      await removeTags(userId, [tagId]);
      await fetchData();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setError(axiosErr?.response?.data?.error?.message ?? "Failed to remove tag.");
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="h-6 w-6 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <h5 className="text-sm font-medium text-gray-800 dark:text-white/90">
        Assigned Tags
      </h5>

      {error && (
        <p className="text-sm text-error-500">{error}</p>
      )}

      {userTags.length === 0 ? (
        <p className="text-sm text-gray-500 dark:text-gray-400">
          No tags assigned to this user.
        </p>
      ) : (
        <div className="flex flex-wrap gap-2">
          {userTags.map((tag) => (
            <span
              key={tag.id}
              className="inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-sm text-brand-600 dark:bg-brand-500/15 dark:text-brand-400"
            >
              {tag.key}:{tag.value}
              <button
                onClick={() => handleRemove(tag.id)}
                disabled={actionLoading}
                className="ml-0.5 rounded-full p-0.5 hover:bg-brand-100 dark:hover:bg-brand-500/25 disabled:opacity-50"
                title="Remove tag"
              >
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M9 3L3 9M3 3L9 9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </button>
            </span>
          ))}
        </div>
      )}

      <div className="flex items-center gap-2 pt-2">
        <select
          value={selectedTagId}
          onChange={(e) => setSelectedTagId(e.target.value)}
          disabled={actionLoading || availableTags.length === 0}
          className="h-10 rounded-lg border border-gray-300 bg-transparent px-3 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90 dark:focus:border-brand-800 disabled:opacity-50"
        >
          <option value="">
            {availableTags.length === 0 ? "No tags available" : "Select a tag..."}
          </option>
          {availableTags.map((tag) => (
            <option key={tag.id} value={tag.id}>
              {tag.key}:{tag.value}
            </option>
          ))}
        </select>
        <Button
          size="sm"
          onClick={handleAssign}
          disabled={actionLoading || !selectedTagId}
        >
          {actionLoading ? "Assigning..." : "Assign"}
        </Button>
      </div>
    </div>
  );
}
