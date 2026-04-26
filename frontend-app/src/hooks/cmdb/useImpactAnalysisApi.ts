import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface RootEntity {
  id: string;
  name: string;
  type: string;
}

export interface AffectedEntity {
  id: string;
  name: string;
  entityType: string;
  relationshipPath: string[];
  depth: number;
}

export interface ImpactAnalysisResponse {
  rootEntity: RootEntity;
  affectedEntities: AffectedEntity[];
  totalAffectedServices: number;
  totalAffectedCIs: number;
  circularDependencyWarnings: string[];
  maxDepthReached: boolean;
}

export function useImpactAnalysisApi() {
  const analyzeCI = useCallback(async (ciId: string): Promise<ImpactAnalysisResponse> => {
    const response = await apiClient.get<ImpactAnalysisResponse>(`/api/v1/cmdb/impact-analysis/ci/${ciId}`);
    return response.data;
  }, []);

  const analyzeService = useCallback(async (serviceId: string): Promise<ImpactAnalysisResponse> => {
    const response = await apiClient.get<ImpactAnalysisResponse>(`/api/v1/cmdb/impact-analysis/service/${serviceId}`);
    return response.data;
  }, []);

  return { analyzeCI, analyzeService };
}
