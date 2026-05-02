import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface OrganizationResponse {
  id: string;
  name: string;
  type: string;
  parentId: string | null;
  responsiblePerson: string;
  costCenter: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrganizationRequest {
  name: string;
  type: string;
  parentId?: string | null;
  responsiblePerson: string;
  costCenter: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  responsiblePerson: string;
  costCenter: string;
}

interface PaginatedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export function useOrganizationApi() {
  const create = useCallback(async (data: CreateOrganizationRequest): Promise<OrganizationResponse> => {
    const response = await apiClient.post<OrganizationResponse>("/api/v1/cmdb/organizations", data);
    return response.data;
  }, []);

  const get = useCallback(async (id: string): Promise<OrganizationResponse> => {
    const response = await apiClient.get<OrganizationResponse>(`/api/v1/cmdb/organizations/${id}`);
    return response.data;
  }, []);

  const update = useCallback(async (id: string, data: UpdateOrganizationRequest): Promise<OrganizationResponse> => {
    const response = await apiClient.put<OrganizationResponse>(`/api/v1/cmdb/organizations/${id}`, data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/organizations/${id}`);
  }, []);

  const list = useCallback(async (): Promise<OrganizationResponse[]> => {
    const response = await apiClient.get<PaginatedResponse<OrganizationResponse>>("/api/v1/cmdb/organizations", {
      params: { page: 0, size: 200 },
    });
    return response.data.items;
  }, []);

  const getTree = useCallback(async (): Promise<OrganizationResponse[]> => {
    const response = await apiClient.get<OrganizationResponse[]>("/api/v1/cmdb/organizations/tree");
    return response.data;
  }, []);

  return { create, get, update, remove, list, getTree };
}
