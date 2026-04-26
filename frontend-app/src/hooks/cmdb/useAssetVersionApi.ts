import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface AssetVersionResponse {
  id: string;
  assetId: string;
  versionNumber: number;
  description: string;
  attributes: Record<string, unknown>;
  startDate: string;
  endDate: string | null;
  dataOrigin: string;
  dataSourceId: string;
  changeReference: string | null;
  createdAt: string;
}

export interface CreateAssetVersionRequest {
  description: string;
  attributes: Record<string, unknown>;
  dataOrigin: string;
  dataSourceId: string;
  changeReference?: string | null;
}

export function useAssetVersionApi() {
  const create = useCallback(async (assetId: string, data: CreateAssetVersionRequest): Promise<AssetVersionResponse> => {
    const response = await apiClient.post<AssetVersionResponse>(`/api/v1/cmdb/assets/${assetId}/versions`, data);
    return response.data;
  }, []);

  const listByAsset = useCallback(async (assetId: string): Promise<AssetVersionResponse[]> => {
    const response = await apiClient.get<AssetVersionResponse[]>(`/api/v1/cmdb/assets/${assetId}/versions`);
    return response.data;
  }, []);

  return { create, listByAsset };
}
