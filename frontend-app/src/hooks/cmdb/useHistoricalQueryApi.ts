import { useCallback } from "react";
import apiClient from "../../api/ApiClient";
import type { AssetVersionResponse } from "./useAssetVersionApi";
import type { CIVersionResponse } from "./useCIVersionApi";

export function useHistoricalQueryApi() {
  const getAssetVersionAt = useCallback(async (assetId: string, at: string): Promise<AssetVersionResponse> => {
    const response = await apiClient.get<AssetVersionResponse>(`/api/v1/cmdb/history/assets/${assetId}`, {
      params: { at },
    });
    return response.data;
  }, []);

  const getCIVersionAt = useCallback(async (ciId: string, at: string): Promise<CIVersionResponse> => {
    const response = await apiClient.get<CIVersionResponse>(`/api/v1/cmdb/history/cis/${ciId}`, {
      params: { at },
    });
    return response.data;
  }, []);

  return { getAssetVersionAt, getCIVersionAt };
}
