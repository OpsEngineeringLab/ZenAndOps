import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface CIResponse {
  id: string;
  name: string;
  type: string;
  organizationId: string;
  assetId: string | null;
  status: string;
  controlledExceptionFlag: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCIRequest {
  name: string;
  type: string;
  organizationId: string;
  assetId?: string | null;
  status: string;
  controlledExceptionFlag: boolean;
}

export interface UpdateCIRequest {
  name: string;
  status: string;
  controlledExceptionFlag: boolean;
}

export interface CIFilters {
  organizationId?: string;
  type?: string;
  status?: string;
  assetId?: string;
}

interface PaginatedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export function useCIApi() {
  const create = useCallback(async (data: CreateCIRequest): Promise<CIResponse> => {
    const response = await apiClient.post<CIResponse>("/api/v1/cmdb/cis", data);
    return response.data;
  }, []);

  const get = useCallback(async (id: string): Promise<CIResponse> => {
    const response = await apiClient.get<CIResponse>(`/api/v1/cmdb/cis/${id}`);
    return response.data;
  }, []);

  const update = useCallback(async (id: string, data: UpdateCIRequest): Promise<CIResponse> => {
    const response = await apiClient.put<CIResponse>(`/api/v1/cmdb/cis/${id}`, data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/cis/${id}`);
  }, []);

  const list = useCallback(async (filters?: CIFilters): Promise<CIResponse[]> => {
    const response = await apiClient.get<PaginatedResponse<CIResponse>>("/api/v1/cmdb/cis", {
      params: { ...filters, page: 0, size: 200 },
    });
    return response.data.items;
  }, []);

  return { create, get, update, remove, list };
}
