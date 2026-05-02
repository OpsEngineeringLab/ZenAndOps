import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface AssetResponse {
  id: string;
  name: string;
  type: string;
  organizationId: string;
  cost: number;
  costType: string;
  acquisitionDate: string;
  status: string;
  supplier: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAssetRequest {
  name: string;
  type: string;
  organizationId: string;
  cost: number;
  costType: string;
  acquisitionDate: string;
  status: string;
  supplier: string;
}

export interface UpdateAssetRequest {
  name: string;
  cost: number;
  costType: string;
  status: string;
  supplier: string;
}

export interface AssetFilters {
  organizationId?: string;
  type?: string;
  costType?: string;
  status?: string;
  supplier?: string;
}

export interface CostSummaryEntry {
  organizationId: string;
  costType: string;
  totalCost: number;
}

interface PaginatedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export function useAssetApi() {
  const create = useCallback(async (data: CreateAssetRequest): Promise<AssetResponse> => {
    const response = await apiClient.post<AssetResponse>("/api/v1/cmdb/assets", data);
    return response.data;
  }, []);

  const get = useCallback(async (id: string): Promise<AssetResponse> => {
    const response = await apiClient.get<AssetResponse>(`/api/v1/cmdb/assets/${id}`);
    return response.data;
  }, []);

  const update = useCallback(async (id: string, data: UpdateAssetRequest): Promise<AssetResponse> => {
    const response = await apiClient.put<AssetResponse>(`/api/v1/cmdb/assets/${id}`, data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/assets/${id}`);
  }, []);

  const list = useCallback(async (filters?: AssetFilters): Promise<AssetResponse[]> => {
    const response = await apiClient.get<PaginatedResponse<AssetResponse>>("/api/v1/cmdb/assets", {
      params: { ...filters, page: 0, size: 200 },
    });
    return response.data.items;
  }, []);

  const getCostSummary = useCallback(async (): Promise<CostSummaryEntry[]> => {
    const response = await apiClient.get<CostSummaryEntry[]>("/api/v1/cmdb/assets/cost-summary");
    return response.data;
  }, []);

  return { create, get, update, remove, list, getCostSummary };
}
