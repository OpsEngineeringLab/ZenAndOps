import { useState, useEffect, useCallback, useRef } from "react";
import PageMeta from "../../components/common/PageMeta";
import PageBreadcrumb from "../../components/common/PageBreadCrumb";
import ComponentCard from "../../components/common/ComponentCard";
import Button from "../../components/ui/button/Button";
import {
  Table, TableHeader, TableBody, TableRow, TableCell,
} from "../../components/ui/table";
import { useFileImportApi } from "../../hooks/cmdb/useFileImportApi";
import type { FileImportResponse } from "../../hooks/cmdb/useFileImportApi";

const ACCEPTED_FORMATS = ".csv,.json,.xml";

export default function FileImport() {
  const { upload, listHistory } = useFileImportApi();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [history, setHistory] = useState<FileImportResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [lastResult, setLastResult] = useState<FileImportResponse | null>(null);
  const [error, setError] = useState("");

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listHistory();
      setHistory(data);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [listHistory]);

  useEffect(() => { fetchHistory(); }, [fetchHistory]);

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
    const formatMap: Record<string, string> = { csv: "CSV", json: "JSON", xml: "XML" };
    const fileFormat = formatMap[ext];

    if (!fileFormat) {
      setError("Unsupported file format. Please upload CSV, JSON, or XML files.");
      return;
    }

    setUploading(true); setError(""); setLastResult(null);
    try {
      const text = await file.text();
      let records: Record<string, unknown>[] = [];

      if (fileFormat === "JSON") {
        const parsed = JSON.parse(text);
        records = Array.isArray(parsed) ? parsed : [parsed];
      } else {
        // For CSV/XML, send as raw content for server-side parsing
        records = [{ rawContent: text, format: fileFormat }];
      }

      const result = await upload({
        fileName: file.name,
        fileFormat,
        records,
      });
      setLastResult(result);
      await fetchHistory();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setError(axiosErr?.response?.data?.error?.message ?? "An error occurred during import.");
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const formatDate = (d: string) => new Date(d).toLocaleString();

  const statusColor = (s: string) => {
    switch (s) {
      case "COMPLETED": return "bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-500";
      case "FAILED": return "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500";
      default: return "bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-orange-400";
    }
  };

  return (
    <>
      <PageMeta title="File Import | ZenAndOps" description="Import assets and CIs from files" />
      <PageBreadcrumb pageTitle="File Import" />

      <div className="space-y-6">
        {/* Upload Section */}
        <ComponentCard title="Upload File">
          <div className="flex flex-wrap items-center gap-4">
            <div className="flex-1">
              <p className="mb-2 text-sm text-gray-500 dark:text-gray-400">
                Upload a CSV, JSON, or XML file to import asset and CI data.
              </p>
              <input
                ref={fileInputRef}
                type="file"
                accept={ACCEPTED_FORMATS}
                onChange={handleFileSelect}
                disabled={uploading}
                className="block w-full text-sm text-gray-500 file:mr-4 file:rounded-lg file:border-0 file:bg-brand-50 file:px-4 file:py-2 file:text-sm file:font-medium file:text-brand-600 hover:file:bg-brand-100 dark:text-gray-400 dark:file:bg-brand-500/15 dark:file:text-brand-400"
              />
            </div>
            {uploading && (
              <div className="flex items-center gap-2">
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-brand-500 border-t-transparent" />
                <span className="text-sm text-gray-500 dark:text-gray-400">Importing...</span>
              </div>
            )}
          </div>

          {error && (
            <div className="mt-4 rounded-lg border border-error-500 bg-error-50 p-3 text-sm text-error-500 dark:border-error-500/30 dark:bg-error-500/15">{error}</div>
          )}

          {/* Import Summary */}
          {lastResult && (
            <div className="mt-4 rounded-lg border border-gray-200 p-4 dark:border-gray-700">
              <h5 className="mb-3 text-sm font-medium text-gray-800 dark:text-white/90">Import Summary</h5>
              <div className="flex flex-wrap gap-4 mb-3">
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Status</p>
                  <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(lastResult.status)}`}>{lastResult.status}</span>
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Total Records</p>
                  <p className="text-sm font-medium text-gray-800 dark:text-white/90">{lastResult.totalRecords}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Successful</p>
                  <p className="text-sm font-medium text-success-600 dark:text-success-500">{lastResult.successCount}</p>
                </div>
                <div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">Failed</p>
                  <p className="text-sm font-medium text-error-600 dark:text-error-500">{lastResult.failureCount}</p>
                </div>
              </div>
              {lastResult.errors.length > 0 && (
                <div className="space-y-1">
                  <p className="text-xs font-medium text-gray-700 dark:text-gray-300">Errors:</p>
                  {lastResult.errors.map((err, i) => (
                    <p key={i} className="text-xs text-error-500">
                      Record {err.recordIndex}: {err.field} — {err.message}
                    </p>
                  ))}
                </div>
              )}
            </div>
          )}
        </ComponentCard>

        {/* Import History */}
        <ComponentCard title="Import History">
          {loading ? (
            <div className="flex items-center justify-center py-10">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
            </div>
          ) : history.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500 dark:text-gray-400">No import history found.</p>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">File Name</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Format</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Status</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Total</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Success</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Failed</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Imported By</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Date</TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {history.map((record) => (
                    <TableRow key={record.id} className="border-t border-gray-100 dark:border-gray-800">
                      <TableCell className="px-4 py-3 text-sm font-medium text-gray-800 dark:text-white/90">{record.fileName}</TableCell>
                      <TableCell className="px-4 py-3"><span className="inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{record.fileFormat}</span></TableCell>
                      <TableCell className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(record.status)}`}>{record.status}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{record.totalRecords}</TableCell>
                      <TableCell className="px-4 py-3 text-sm text-success-600 dark:text-success-500">{record.successCount}</TableCell>
                      <TableCell className="px-4 py-3 text-sm text-error-600 dark:text-error-500">{record.failureCount}</TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{record.importedBy}</TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{formatDate(record.createdAt)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </ComponentCard>
      </div>
    </>
  );
}
