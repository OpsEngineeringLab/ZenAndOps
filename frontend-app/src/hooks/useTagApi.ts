import { useCallback } from "react";
import apiClient from "../api/ApiClient";

export interface TagResponse {
  id: string;
  key: string;
  value: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedTagsResponse {
  items: TagResponse[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CreateTagRequest {
  key: string;
  value: string;
  description?: string;
}

export interface UpdateTagRequest {
  description: string;
}

export function useTagApi() {
  const createTag = useCallback(async (data: CreateTagRequest): Promise<TagResponse> => {
    const response = await apiClient.post<TagResponse>("/api/v1/tags", data);
    return response.data;
  }, []);

  const listTags = useCallback(async (page = 0, size = 20): Promise<PaginatedTagsResponse> => {
    const response = await apiClient.get<PaginatedTagsResponse>("/api/v1/tags", {
      params: { page, size },
    });
    return response.data;
  }, []);

  const getTag = useCallback(async (id: string): Promise<TagResponse> => {
    const response = await apiClient.get<TagResponse>(`/api/v1/tags/${id}`);
    return response.data;
  }, []);

  const updateTag = useCallback(async (id: string, data: UpdateTagRequest): Promise<TagResponse> => {
    const response = await apiClient.put<TagResponse>(`/api/v1/tags/${id}`, data);
    return response.data;
  }, []);

  const deleteTag = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/tags/${id}`);
  }, []);

  return { createTag, listTags, getTag, updateTag, deleteTag };
}
