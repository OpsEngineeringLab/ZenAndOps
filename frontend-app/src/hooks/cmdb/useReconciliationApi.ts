import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface ReconciliationDetailResponse {
  entityId: string;
  entityName: string;
  conflictType: string;
  resolution: string;
  preferredSourceId: string | null;
}

export interface ReconciliationResponse {
  id: string;
  entityType: string;
  recordsAnalyzed: number;
  duplicatesFound: number;
  conflictsResolved: number;
  unresolvedConflicts: number;
  details: ReconciliationDetailResponse[];
  triggeredBy: string;
  createdAt: string;
}

export interface TriggerReconciliationRequest {
  entityType: string;
}

export function useReconciliationApi() {
  const trigger = useCallback(async (data: TriggerReconciliationRequest): Promise<ReconciliationResponse> => {
    const response = await apiClient.post<ReconciliationResponse>("/api/v1/cmdb/reconciliations", data);
    return response.data;
  }, []);

  const listHistory = useCallback(async (): Promise<ReconciliationResponse[]> => {
    const response = await apiClient.get<ReconciliationResponse[]>("/api/v1/cmdb/reconciliations");
    return response.data;
  }, []);

  return { trigger, listHistory };
}
