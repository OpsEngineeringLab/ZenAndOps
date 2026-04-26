import { useState, useEffect, useCallback } from "react";
import PageMeta from "../../components/common/PageMeta";
import PageBreadcrumb from "../../components/common/PageBreadCrumb";
import ComponentCard from "../../components/common/ComponentCard";
import Button from "../../components/ui/button/Button";
import { useImpactAnalysisApi } from "../../hooks/cmdb/useImpactAnalysisApi";
import type { ImpactAnalysisResponse } from "../../hooks/cmdb/useImpactAnalysisApi";
import { useCIApi } from "../../hooks/cmdb/useCIApi";
import type { CIResponse } from "../../hooks/cmdb/useCIApi";
import { useServiceApi } from "../../hooks/cmdb/useServiceApi";
import type { ServiceResponse } from "../../hooks/cmdb/useServiceApi";

type EntityType = "CI" | "SERVICE";

export default function ImpactAnalysis() {
  const { analyzeCI, analyzeService } = useImpactAnalysisApi();
  const { list: listCIs } = useCIApi();
  const { list: listServices } = useServiceApi();

  const [entityType, setEntityType] = useState<EntityType>("CI");
  const [entityId, setEntityId] = useState("");
  const [ciList, setCIList] = useState<CIResponse[]>([]);
  const [serviceList, setServiceList] = useState<ServiceResponse[]>([]);
  const [result, setResult] = useState<ImpactAnalysisResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { listCIs().then(setCIList).catch(() => {}); }, [listCIs]);
  useEffect(() => { listServices().then(setServiceList).catch(() => {}); }, [listServices]);

  const handleAnalyze = useCallback(async () => {
    if (!entityId) return;
    setLoading(true); setError(""); setResult(null);
    try {
      const data = entityType === "CI" ? await analyzeCI(entityId) : await analyzeService(entityId);
      setResult(data);
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setError(axiosErr?.response?.data?.error?.message ?? "An error occurred during analysis.");
    } finally {
      setLoading(false);
    }
  }, [entityType, entityId, analyzeCI, analyzeService]);

  const selectClasses = "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

  return (
    <>
      <PageMeta title="Impact Analysis | ZenAndOps" description="Analyze impact of CI or service changes" />
      <PageBreadcrumb pageTitle="Impact Analysis" />

      <div className="space-y-6">
        <ComponentCard title="Impact Analysis">
          <div className="flex flex-wrap items-end gap-3 mb-6">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">Entity Type</label>
              <select value={entityType} onChange={(e) => { setEntityType(e.target.value as EntityType); setEntityId(""); setResult(null); }} className={selectClasses} style={{ width: 160 }}>
                <option value="CI">Configuration Item</option>
                <option value="SERVICE">Service</option>
              </select>
            </div>
            <div style={{ flex: 1, minWidth: 200 }}>
              <label className="mb-1.5 block text-sm font-medium text-gray-700 dark:text-gray-400">Select {entityType === "CI" ? "CI" : "Service"}</label>
              <select value={entityId} onChange={(e) => setEntityId(e.target.value)} className={selectClasses}>
                <option value="">Select...</option>
                {entityType === "CI"
                  ? ciList.map((ci) => <option key={ci.id} value={ci.id}>{ci.name} ({ci.type})</option>)
                  : serviceList.map((svc) => <option key={svc.id} value={svc.id}>{svc.name} ({svc.type})</option>)
                }
              </select>
            </div>
            <Button size="sm" onClick={handleAnalyze} disabled={loading || !entityId}>
              {loading ? "Analyzing..." : "Analyze Impact"}
            </Button>
          </div>

          {error && (
            <div className="mb-4 rounded-lg border border-error-500 bg-error-50 p-3 text-sm text-error-500 dark:border-error-500/30 dark:bg-error-500/15">{error}</div>
          )}

          {result && (
            <div className="space-y-4">
              {/* Summary */}
              <div className="flex flex-wrap gap-4">
                <div className="rounded-lg border border-gray-100 px-4 py-3 dark:border-gray-800">
                  <p className="text-xs text-gray-500 dark:text-gray-400">Root Entity</p>
                  <p className="text-sm font-medium text-gray-800 dark:text-white/90">{result.rootEntity.name}</p>
                  <span className="inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{result.rootEntity.type}</span>
                </div>
                <div className="rounded-lg border border-gray-100 px-4 py-3 dark:border-gray-800">
                  <p className="text-xs text-gray-500 dark:text-gray-400">Affected Services</p>
                  <p className="text-lg font-semibold text-gray-800 dark:text-white/90">{result.totalAffectedServices}</p>
                </div>
                <div className="rounded-lg border border-gray-100 px-4 py-3 dark:border-gray-800">
                  <p className="text-xs text-gray-500 dark:text-gray-400">Affected CIs</p>
                  <p className="text-lg font-semibold text-gray-800 dark:text-white/90">{result.totalAffectedCIs}</p>
                </div>
                {result.maxDepthReached && (
                  <div className="rounded-lg border border-warning-500 bg-warning-50 px-4 py-3 dark:border-warning-500/30 dark:bg-warning-500/15">
                    <p className="text-xs font-medium text-warning-600 dark:text-orange-400">Max depth reached</p>
                  </div>
                )}
              </div>

              {/* Circular dependency warnings */}
              {result.circularDependencyWarnings.length > 0 && (
                <div className="rounded-lg border border-warning-500 bg-warning-50 p-3 dark:border-warning-500/30 dark:bg-warning-500/15">
                  <p className="text-sm font-medium text-warning-600 dark:text-orange-400 mb-1">Circular Dependencies Detected</p>
                  {result.circularDependencyWarnings.map((w, i) => (
                    <p key={i} className="text-xs text-warning-600 dark:text-orange-400">{w}</p>
                  ))}
                </div>
              )}

              {/* Affected entities list */}
              {result.affectedEntities.length > 0 && (
                <div className="space-y-2">
                  <h5 className="text-sm font-medium text-gray-800 dark:text-white/90">Affected Entities</h5>
                  {result.affectedEntities.map((entity) => (
                    <div key={entity.id} className="flex items-center gap-3 rounded-lg border border-gray-100 px-4 py-2 dark:border-gray-800">
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                        entity.entityType === "SERVICE"
                          ? "bg-blue-light-50 text-blue-light-500 dark:bg-blue-light-500/15 dark:text-blue-light-500"
                          : "bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400"
                      }`}>{entity.entityType}</span>
                      <span className="text-sm font-medium text-gray-800 dark:text-white/90">{entity.name}</span>
                      <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                        entity.depth === 1
                          ? "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500"
                          : "bg-gray-100 text-gray-700 dark:bg-white/5 dark:text-white/80"
                      }`}>depth {entity.depth}</span>
                      {entity.relationshipPath.length > 0 && (
                        <span className="text-xs text-gray-400">{entity.relationshipPath.join(" → ")}</span>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </ComponentCard>
      </div>
    </>
  );
}
