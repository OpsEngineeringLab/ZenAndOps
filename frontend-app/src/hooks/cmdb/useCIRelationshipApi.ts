import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface CIRelationshipResponse {
  id: string;
  sourceCIId: string;
  targetCIId: string;
  relationshipType: string;
  createdAt: string;
}

export interface CreateCIRelationshipRequest {
  sourceCIId: string;
  targetCIId: string;
  relationshipType: string;
}

export function useCIRelationshipApi() {
  const create = useCallback(async (data: CreateCIRelationshipRequest): Promise<CIRelationshipResponse> => {
    const response = await apiClient.post<CIRelationshipResponse>("/api/v1/cmdb/ci-relationships", data);
    return response.data;
  }, []);

  const remove = useCallback(async (id: string): Promise<void> => {
    await apiClient.delete(`/api/v1/cmdb/ci-relationships/${id}`);
  }, []);

  const listByCI = useCallback(async (ciId: string): Promise<CIRelationshipResponse[]> => {
    const response = await apiClient.get<CIRelationshipResponse[]>("/api/v1/cmdb/ci-relationships", {
      params: { ciId },
    });
    return response.data;
  }, []);

  return { create, remove, listByCI };
}
