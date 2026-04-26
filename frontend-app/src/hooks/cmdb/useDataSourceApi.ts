import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface DataSourceResponse {
  id: string;
  name: string;
  type: string;
  reliabilityRating: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDataSourceRequest {
  name: string;
  type: string;
  reliabilityRating: number;
}

export interface UpdateDataSourceRequest {
  name: string;
  reliabilityRating: number;
}

export function useDataSourceApi() {
  const create = useCallback(async (data: CreateDataSourceRequest): Promise<DataSourceResponse> => {
    const response = await apiClient.post<DataSourceResponse>("/api/v1/cmdb/data-sources", data);
    return response.data;
  }, []);

  const get = useCallback(async (id: string): Promise<DataSourceResponse> => {
    const response = await apiClient.get<DataSourceResponse>(`/api/v1/cmdb/data-sources/${id}`);
    return response.data;
  }, []);

  const update = useCallback(async (id: string, data: UpdateDataSourceRequest): Promise<DataSourceResponse> => {
    const response = await apiClient.put<DataSourceResponse>(`/api/v1/cmdb/data-sources/${id}`, data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/data-sources/${id}`);
  }, []);

  const list = useCallback(async (): Promise<DataSourceResponse[]> => {
    const response = await apiClient.get<DataSourceResponse[]>("/api/v1/cmdb/data-sources");
    return response.data;
  }, []);

  return { create, get, update, remove, list };
}
