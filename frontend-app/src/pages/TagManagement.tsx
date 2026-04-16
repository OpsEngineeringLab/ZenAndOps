import { useState, useEffect, useCallback } from "react";
import PageMeta from "../components/common/PageMeta";
import PageBreadcrumb from "../components/common/PageBreadCrumb";
import ComponentCard from "../components/common/ComponentCard";
import Button from "../components/ui/button/Button";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
} from "../components/ui/table";
import { useTagApi } from "../hooks/useTagApi";
import type { TagResponse } from "../hooks/useTagApi";
import TagFormModal from "../components/tags/TagFormModal";
import TagDeleteConfirmModal from "../components/tags/TagDeleteConfirmModal";
import { useModal } from "../hooks/useModal";

export default function TagManagement() {
  const { listTags, createTag, updateTag, deleteTag } = useTagApi();
  const [tags, setTags] = useState<TagResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const formModal = useModal();
  const deleteModal = useModal();
  const [selectedTag, setSelectedTag] = useState<TagResponse | null>(null);

  const fetchTags = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listTags(0, 100);
      setTags(data.items);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [listTags]);

  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  const handleCreate = () => {
    setSelectedTag(null);
    formModal.openModal();
  };

  const handleEdit = (tag: TagResponse) => {
    setSelectedTag(tag);
    formModal.openModal();
  };

  const handleDeleteClick = (tag: TagResponse) => {
    setSelectedTag(tag);
    deleteModal.openModal();
  };

  const handleFormSubmit = async (data: { key: string; value: string; description: string }) => {
    if (selectedTag) {
      await updateTag(selectedTag.id, { description: data.description });
    } else {
      await createTag(data);
    }
    await fetchTags();
  };

  const handleDeleteConfirm = async (id: string) => {
    await deleteTag(id);
    await fetchTags();
  };

  return (
    <>
      <PageMeta
        title="Tag Management | ZenAndOps"
        description="Manage tags for attribute-based access control"
      />
      <PageBreadcrumb pageTitle="Tag Management" />

      <div className="space-y-6">
        <ComponentCard title="Tags">
          <div className="mb-4 flex items-center justify-end">
            <Button size="sm" onClick={handleCreate}>
              + Create Tag
            </Button>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-10">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
            </div>
          ) : tags.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500 dark:text-gray-400">
              No tags found. Create one to get started.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">
                      Key
                    </TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">
                      Value
                    </TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">
                      Description
                    </TableCell>
                    <TableCell isHeader className="px-4 py-3 text-right text-sm font-medium text-gray-500 dark:text-gray-400">
                      Actions
                    </TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {tags.map((tag) => (
                    <TableRow key={tag.id} className="border-t border-gray-100 dark:border-gray-800">
                      <TableCell className="px-4 py-3 text-sm text-gray-800 dark:text-white/90">
                        {tag.key}
                      </TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-800 dark:text-white/90">
                        {tag.value}
                      </TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                        {tag.description || "—"}
                      </TableCell>
                      <TableCell className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => handleEdit(tag)}
                            className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400"
                            title="Edit"
                          >
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <path d="M14.166 2.5L17.499 5.833M1.666 18.333L2.916 13.75L14.166 2.5L17.499 5.833L6.249 17.083L1.666 18.333Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                          </button>
                          <button
                            onClick={() => handleDeleteClick(tag)}
                            className="rounded-lg p-1.5 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:text-gray-400 dark:hover:bg-error-500/15 dark:hover:text-error-400"
                            title="Delete"
                          >
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                              <path d="M2.5 5H17.5M8.333 9.167V14.167M11.667 9.167V14.167M3.333 5L4.167 16.667C4.167 17.108 4.342 17.531 4.655 17.845C4.967 18.158 5.391 18.333 5.833 18.333H14.167C14.608 18.333 15.032 18.158 15.345 17.845C15.658 17.531 15.833 17.108 15.833 16.667L16.667 5M6.667 5V2.5C6.667 2.279 6.755 2.067 6.911 1.911C7.067 1.755 7.279 1.667 7.5 1.667H12.5C12.721 1.667 12.933 1.755 13.089 1.911C13.245 2.067 13.333 2.279 13.333 2.5V5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                          </button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </ComponentCard>
      </div>

      <TagFormModal
        isOpen={formModal.isOpen}
        onClose={formModal.closeModal}
        onSubmit={handleFormSubmit}
        tag={selectedTag}
      />

      <TagDeleteConfirmModal
        isOpen={deleteModal.isOpen}
        onClose={deleteModal.closeModal}
        onConfirm={handleDeleteConfirm}
        tag={selectedTag}
      />
    </>
  );
}
