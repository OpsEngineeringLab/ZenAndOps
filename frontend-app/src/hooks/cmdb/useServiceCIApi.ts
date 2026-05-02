import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface ServiceCIResponse {
  id: string;
  serviceId: string;
  ciId: string;
  createdAt: string;
}

export interface CreateServiceCIRequest {
  serviceId: string;
  ciId: string;
}

interface PaginatedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export function useServiceCIApi() {
  const create = useCallback(async (data: CreateServiceCIRequest): Promise<ServiceCIResponse> => {
    const response = await apiClient.post<ServiceCIResponse>("/api/v1/cmdb/service-cis", data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/service-cis/${id}`);
  }, []);

  const listByService = useCallback(async (serviceId: string): Promise<ServiceCIResponse[]> => {
    const response = await apiClient.get<PaginatedResponse<ServiceCIResponse>>("/api/v1/cmdb/service-cis", {
      params: { serviceId, page: 0, size: 200 },
    });
    return response.data.items;
  }, []);

  const listByCI = useCallback(async (ciId: string): Promise<ServiceCIResponse[]> => {
    const response = await apiClient.get<PaginatedResponse<ServiceCIResponse>>("/api/v1/cmdb/service-cis", {
      params: { ciId, page: 0, size: 200 },
    });
    return response.data.items;
  }, []);

  return { create, remove, listByService, listByCI };
}
