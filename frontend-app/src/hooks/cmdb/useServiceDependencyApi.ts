import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface ServiceDependencyResponse {
  id: string;
  sourceServiceId: string;
  targetServiceId: string;
  dependencyType: string;
  createdAt: string;
}

export interface CreateServiceDependencyRequest {
  sourceServiceId: string;
  targetServiceId: string;
  dependencyType: string;
}

export function useServiceDependencyApi() {
  const create = useCallback(async (data: CreateServiceDependencyRequest): Promise<ServiceDependencyResponse> => {
    const response = await apiClient.post<ServiceDependencyResponse>("/api/v1/cmdb/service-dependencies", data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/service-dependencies/${id}`);
  }, []);

  const listByService = useCallback(async (serviceId: string): Promise<ServiceDependencyResponse[]> => {
    const response = await apiClient.get<ServiceDependencyResponse[]>("/api/v1/cmdb/service-dependencies", {
      params: { serviceId },
    });
    return response.data;
  }, []);

  return { create, remove, listByService };
}
