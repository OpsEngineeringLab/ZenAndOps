import { useCallback } from "react";
import apiClient from "../../api/ApiClient";

export interface ImportErrorResponse {
  recordIndex: number;
  field: string;
  message: string;
}

export interface FileImportResponse {
  id: string;
  fileName: string;
  fileFormat: string;
  dataSourceId: string;
  status: string;
  totalRecords: number;
  successCount: number;
  failureCount: number;
  errors: ImportErrorResponse[];
  importedBy: string;
  createdAt: string;
}

export interface FileImportRequest {
  fileName: string;
  fileFormat: string;
  records: Record<string, unknown>[];
}

export function useFileImportApi() {
  const upload = useCallback(async (data: FileImportRequest): Promise<FileImportResponse> => {
    const response = await apiClient.post<FileImportResponse>("/api/v1/cmdb/imports", data);
    return response.data;
  }, []);

  const listHistory = useCallback(async (): Promise<FileImportResponse[]> => {
    const response = await apiClient.get<FileImportResponse[]>("/api/v1/cmdb/imports");
    return response.data;
  }, []);

  return { upload, listHistory };
}
