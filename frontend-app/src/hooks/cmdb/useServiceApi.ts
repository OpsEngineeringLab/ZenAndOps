import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface ServiceResponse {
  id: string;
  name: string;
  description: string;
  type: string;
  parentId: string | null;
  organizationId: string;
  businessOwner: string;
  technicalOwner: string;
  criticality: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateServiceRequest {
  name: string;
  description: string;
  type: string;
  parentId?: string | null;
  organizationId: string;
  businessOwner: string;
  technicalOwner: string;
  criticality: string;
  status: string;
}

export interface UpdateServiceRequest {
  name: string;
  description: string;
  businessOwner: string;
  technicalOwner: string;
  criticality: string;
  status: string;
}

export interface ServiceFilters {
  organizationId?: string;
  type?: string;
  criticality?: string;
  status?: string;
}

export function useServiceApi() {
  const create = useCallback(async (data: CreateServiceRequest): Promise<ServiceResponse> => {
    const response = await apiClient.post<ServiceResponse>("/api/v1/cmdb/services", data);
    return response.data;
  }, []);

  const get = useCallback(async (id: string): Promise<ServiceResponse> => {
    const response = await apiClient.get<ServiceResponse>(`/api/v1/cmdb/services/${id}`);
    return response.data;
  }, []);

  const update = useCallback(async (id: string, data: UpdateServiceRequest): Promise<ServiceResponse> => {
    const response = await apiClient.put<ServiceResponse>(`/api/v1/cmdb/services/${id}`, data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/services/${id}`);
  }, []);

  const list = useCallback(async (filters?: ServiceFilters): Promise<ServiceResponse[]> => {
    const response = await apiClient.get<ServiceResponse[]>("/api/v1/cmdb/services", {
      params: filters,
    });
    return response.data;
  }, []);

  const getTree = useCallback(async (): Promise<ServiceResponse[]> => {
    const response = await apiClient.get<ServiceResponse[]>("/api/v1/cmdb/services/tree");
    return response.data;
  }, []);

  return { create, get, update, remove, list, getTree };
}
