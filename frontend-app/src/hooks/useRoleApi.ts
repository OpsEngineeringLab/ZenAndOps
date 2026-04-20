import { useCallback } from "react";
import apiClient from "../api/ApiClient";

export interface RoleResponse {
  id: string;
  name: string;
  description: string;
  permissions: string[];
  createdAt: string;
  updatedAt: string;
}

export interface PaginatedRolesResponse {
  items: RoleResponse[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CreateRoleRequest {
  name: string;
  description: string;
  permissions: string[];
}

export interface UpdateRoleRequest {
  name: string;
  description: string;
  permissions: string[];
}

export function useRoleApi() {
  const createRole = useCallback(async (data: CreateRoleRequest): Promise<RoleResponse> => {
    const response = await apiClient.post<RoleResponse>("/api/v1/roles", data);
    return response.data;
  }, []);

  const listRoles = useCallback(async (page = 0, size = 20): Promise<PaginatedRolesResponse> => {
    const response = await apiClient.get<PaginatedRolesResponse>("/api/v1/roles", {
      params: { page, size },
    });
    return response.data;
  }, []);

  const getRole = useCallback(async (id: string): Promise<RoleResponse> => {
    const response = await apiClient.get<RoleResponse>(`/api/v1/roles/${id}`);
    return response.data;
  }, []);

  const updateRole = useCallback(async (id: string, data: UpdateRoleRequest): Promise<RoleResponse> => {
    const response = await apiClient.put<RoleResponse>(`/api/v1/roles/${id}`, data);
    return response.data;
  }, []);

  const deleteRole = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/roles/${id}`);
  }, []);

  return { createRole, listRoles, getRole, updateRole, deleteRole };
}
