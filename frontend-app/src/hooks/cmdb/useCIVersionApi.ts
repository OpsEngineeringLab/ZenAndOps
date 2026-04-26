import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface CIVersionResponse {
  id: string;
  ciId: string;
  versionNumber: number;
  attributes: Record<string, unknown>;
  startDate: string;
  endDate: string | null;
  dataOrigin: string;
  dataSourceId: string;
  changeReference: string | null;
  createdAt: string;
}

export interface CreateCIVersionRequest {
  attributes: Record<string, unknown>;
  dataOrigin: string;
  dataSourceId: string;
  changeReference?: string | null;
}

export function useCIVersionApi() {
  const create = useCallback(async (ciId: string, data: CreateCIVersionRequest): Promise<CIVersionResponse> => {
    const response = await apiClient.post<CIVersionResponse>(`/api/v1/cmdb/cis/${ciId}/versions`, data);
    return response.data;
  }, []);

  const listByCI = useCallback(async (ciId: string): Promise<CIVersionResponse[]> => {
    const response = await apiClient.get<CIVersionResponse[]>(`/api/v1/cmdb/cis/${ciId}/versions`);
    return response.data;
  }, []);

  return { create, listByCI };
}
