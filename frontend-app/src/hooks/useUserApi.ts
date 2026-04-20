import { useCallback } from "react";
import apiClient from "../api/ApiClient";

export interface UserResponse {
  id: string;
  login: string;
  name: string;
  email: string;
  roles: string[];
  tagIds: string[];
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedUsersResponse {
  items: UserResponse[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CreateUserRequest {
  login: string;
  name: string;
  email: string;
  password: string;
  roles: string[];
  tagIds: string[];
}

export interface UpdateUserRequest {
  name: string;
  email: string;
  password?: string;
  active: boolean;
  roles: string[];
  tagIds: string[];
}

export function useUserApi() {
  const createUser = useCallback(async (data: CreateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>("/api/v1/users", data);
    return response.data;
  }, []);

  const listUsers = useCallback(async (page = 0, size = 20): Promise<PaginatedUsersResponse> => {
    const response = await apiClient.get<PaginatedUsersResponse>("/api/v1/users", {
      params: { page, size },
    });
    return response.data;
  }, []);

  const getUser = useCallback(async (id: string): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>(`/api/v1/users/${id}`);
    return response.data;
  }, []);

  const updateUser = useCallback(async (id: string, data: UpdateUserRequest): Promise<UserResponse> => {
    const response = await apiClient.put<UserResponse>(`/api/v1/users/${id}`, data);
    return response.data;
  }, []);

  const deleteUser = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/users/${id}`);
  }, []);

  const assignRoles = useCallback(async (userId: string, roleNames: string[]): Promise<UserResponse> => {
    const response = await apiClient.post<UserResponse>(`/api/v1/users/${userId}/roles`, { roleNames });
    return response.data;
  }, []);

  const removeRoles = useCallback(async (userId: string, roleNames: string[]): Promise<UserResponse> => {
    const response = await apiClient.delete<UserResponse>(`/api/v1/users/${userId}/roles`, {
      data: { roleNames },
    });
    return response.data;
  }, []);

  return { createUser, listUsers, getUser, updateUser, deleteUser, assignRoles, removeRoles };
}
