import { useCallback } from "react";
import apiClient from "../api/ApiClient";
import type { TagResponse } from "./useTagApi";

export function useUserTagApi() {
  const getUserTags = useCallback(async (userId: string): Promise<TagResponse[]> => {
    const response = await apiClient.get<TagResponse[]>(`/api/v1/users/${userId}/tags`);
    return response.data;
  }, []);

  const assignTags = useCallback(async (userId: string, tagIds: string[]): Promise<TagResponse[]> => {
    const response = await apiClient.post<TagResponse[]>(`/api/v1/users/${userId}/tags`, { tagIds });
    return response.data;
  }, []);

  const removeTags = useCallback(async (userId: string, tagIds: string[]): Promise<void> => {
    await apiClient.delete(`/api/v1/users/${userId}/tags`, { data: { tagIds } });
  }, []);

  return { getUserTags, assignTags, removeTags };
}
