import { useState, useEffect, useCallback } from "react";
import PageMeta from "../../components/common/PageMeta";
import PageBreadcrumb from "../../components/common/PageBreadCrumb";
import ComponentCard from "../../components/common/ComponentCard";
import Button from "../../components/ui/button/Button";
import {
  Table, TableHeader, TableBody, TableRow, TableCell,
} from "../../components/ui/table";
import { Modal } from "../../components/ui/modal";
import Label from "../../components/form/Label";
import Input from "../../components/form/input/InputField";
import { useCIApi } from "../../hooks/cmdb/useCIApi";
import type { CIResponse, CreateCIRequest, UpdateCIRequest, CIFilters } from "../../hooks/cmdb/useCIApi";
import { useCIVersionApi } from "../../hooks/cmdb/useCIVersionApi";
import type { CIVersionResponse } from "../../hooks/cmdb/useCIVersionApi";
import { useCIRelationshipApi } from "../../hooks/cmdb/useCIRelationshipApi";
import type { CIRelationshipResponse, CreateCIRelationshipRequest } from "../../hooks/cmdb/useCIRelationshipApi";
import { useOrganizationApi } from "../../hooks/cmdb/useOrganizationApi";
import type { OrganizationResponse } from "../../hooks/cmdb/useOrganizationApi";
import { useAssetApi } from "../../hooks/cmdb/useAssetApi";
import type { AssetResponse } from "../../hooks/cmdb/useAssetApi";
import { useModal } from "../../hooks/useModal";

const CI_TYPES = ["VM", "DATABASE", "API", "STORAGE", "NETWORK"];
const CI_STATUSES = ["ACTIVE", "INACTIVE", "DECOMMISSIONED"];
const RELATIONSHIP_TYPES = ["DEPENDS_ON", "HOSTS", "CONNECTS_TO"];

export default function CIManagement() {
  const { create, update, remove, list } = useCIApi();
  const { listByCI: listVersions } = useCIVersionApi();
  const { create: createRel, remove: removeRel, listByCI: listRelsByCI } = useCIRelationshipApi();
  const { list: listOrgs } = useOrganizationApi();
  const { list: listAssets } = useAssetApi();

  const [cis, setCIs] = useState<CIResponse[]>([]);
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [assets, setAssets] = useState<AssetResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<CIFilters>({});

  const formModal = useModal();
  const deleteModal = useModal();
  const versionModal = useModal();
  const relModal = useModal();

  const [selectedCI, setSelectedCI] = useState<CIResponse | null>(null);
  const [isEdit, setIsEdit] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<CIResponse | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [versions, setVersions] = useState<CIVersionResponse[]>([]);
  const [relationships, setRelationships] = useState<CIRelationshipResponse[]>([]);

  // Form state
  const [formName, setFormName] = useState("");
  const [formType, setFormType] = useState("VM");
  const [formOrgId, setFormOrgId] = useState("");
  const [formAssetId, setFormAssetId] = useState<string | null>(null);
  const [formStatus, setFormStatus] = useState("ACTIVE");
  const [formException, setFormException] = useState(false);
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState("");

  // Relationship form state
  const [relCIId, setRelCIId] = useState<string | null>(null);
  const [relTargetId, setRelTargetId] = useState("");
  const [relType, setRelType] = useState("DEPENDS_ON");
  const [relLoading, setRelLoading] = useState(false);

  const fetchCIs = useCallback(async () => {
    setLoading(true);
    try {
      const cleanFilters: CIFilters = {};
      if (filters.organizationId) cleanFilters.organizationId = filters.organizationId;
      if (filters.type) cleanFilters.type = filters.type;
      if (filters.status) cleanFilters.status = filters.status;
      if (filters.assetId) cleanFilters.assetId = filters.assetId;
      const data = await list(Object.keys(cleanFilters).length > 0 ? cleanFilters : undefined);
      setCIs(data);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [list, filters]);

  useEffect(() => { fetchCIs(); }, [fetchCIs]);
  useEffect(() => { listOrgs().then(setOrganizations).catch(() => {}); }, [listOrgs]);
  useEffect(() => { listAssets().then(setAssets).catch(() => {}); }, [listAssets]);

  const orgMap = new Map(organizations.map((o) => [o.id, o.name]));
  const assetMap = new Map(assets.map((a) => [a.id, a.name]));
  const ciNameMap = new Map(cis.map((c) => [c.id, c.name]));

  const handleCreate = () => {
    setIsEdit(false); setSelectedCI(null);
    setFormName(""); setFormType("VM"); setFormOrgId(organizations[0]?.id ?? "");
    setFormAssetId(null); setFormStatus("ACTIVE"); setFormException(false); setFormError("");
    formModal.openModal();
  };

  const handleEdit = (ci: CIResponse) => {
    setIsEdit(true); setSelectedCI(ci);
    setFormName(ci.name); setFormType(ci.type); setFormOrgId(ci.organizationId);
    setFormAssetId(ci.assetId); setFormStatus(ci.status);
    setFormException(ci.controlledExceptionFlag); setFormError("");
    formModal.openModal();
  };

  const handleFormSubmit = async () => {
    if (!formName.trim()) { setFormError("Name is required."); return; }
    setFormLoading(true); setFormError("");
    try {
      if (isEdit && selectedCI) {
        const data: UpdateCIRequest = {
          name: formName.trim(), status: formStatus, controlledExceptionFlag: formException,
        };
        await update(selectedCI.id, data);
      } else {
        const data: CreateCIRequest = {
          name: formName.trim(), type: formType, organizationId: formOrgId,
          assetId: formAssetId || null, status: formStatus, controlledExceptionFlag: formException,
        };
        await create(data);
      }
      formModal.closeModal();
      await fetchCIs();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setFormError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
    } finally {
      setFormLoading(false);
    }
  };

  const handleDeleteClick = (ci: CIResponse) => {
    setDeleteTarget(ci); setDeleteError(""); deleteModal.openModal();
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setDeleteLoading(true); setDeleteError("");
    try {
      await remove(deleteTarget.id);
      deleteModal.closeModal();
      await fetchCIs();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } } };
      if (axiosErr?.response?.status === 409) {
        setDeleteError(axiosErr.response.data?.error?.message ?? "This CI is in use and cannot be deleted.");
      } else {
        setDeleteError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
      }
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleShowVersions = async (ci: CIResponse) => {
    setSelectedCI(ci);
    try { setVersions(await listVersions(ci.id)); } catch { setVersions([]); }
    versionModal.openModal();
  };

  const handleShowRels = async (ci: CIResponse) => {
    setRelCIId(ci.id); setRelTargetId(""); setRelType("DEPENDS_ON");
    try { setRelationships(await listRelsByCI(ci.id)); } catch { setRelationships([]); }
    relModal.openModal();
  };

  const handleAddRel = async () => {
    if (!relCIId || !relTargetId) return;
    setRelLoading(true);
    try {
      const data: CreateCIRelationshipRequest = { sourceCIId: relCIId, targetCIId: relTargetId, relationshipType: relType };
      await createRel(data);
      setRelationships(await listRelsByCI(relCIId));
      setRelTargetId("");
    } catch { /* handled */ } finally { setRelLoading(false); }
  };

  const handleRemoveRel = async (relId: string) => {
    if (!relCIId) return;
    try { await removeRel(relId); setRelationships(await listRelsByCI(relCIId)); } catch { /* handled */ }
  };

  const selectClasses = "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";
  const filterSelectClasses = "h-9 rounded-lg border border-gray-300 bg-transparent px-3 py-1 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

  const statusColor = (s: string) => {
    switch (s) {
      case "ACTIVE": return "bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-500";
      case "DECOMMISSIONED": return "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500";
      default: return "bg-gray-100 text-gray-700 dark:bg-white/5 dark:text-white/80";
    }
  };

  const formatDate = (d: string | null) => d ? new Date(d).toLocaleDateString() : "—";

  return (
    <>
      <PageMeta title="CI Management | ZenAndOps" description="Manage configuration items" />
      <PageBreadcrumb pageTitle="CI Management" />

      <div className="space-y-6">
        <ComponentCard title="Configuration Items">
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <select value={filters.organizationId ?? ""} onChange={(e) => setFilters({ ...filters, organizationId: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Organizations</option>
              {organizations.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </select>
            <select value={filters.type ?? ""} onChange={(e) => setFilters({ ...filters, type: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Types</option>
              {CI_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <select value={filters.status ?? ""} onChange={(e) => setFilters({ ...filters, status: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Statuses</option>
              {CI_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
            <div className="ml-auto">
              <Button size="sm" onClick={handleCreate}>+ Create CI</Button>
            </div>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-10">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
            </div>
          ) : cis.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500 dark:text-gray-400">No configuration items found.</p>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Name</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Type</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Organization</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Status</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Asset</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-right text-sm font-medium text-gray-500 dark:text-gray-400">Actions</TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {cis.map((ci) => (
                    <TableRow key={ci.id} className="border-t border-gray-100 dark:border-gray-800">
                      <TableCell className="px-4 py-3 text-sm font-medium text-gray-800 dark:text-white/90">{ci.name}</TableCell>
                      <TableCell className="px-4 py-3"><span className="inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{ci.type}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{orgMap.get(ci.organizationId) ?? ci.organizationId}</TableCell>
                      <TableCell className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(ci.status)}`}>{ci.status}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{ci.assetId ? (assetMap.get(ci.assetId) ?? ci.assetId) : "—"}</TableCell>
                      <TableCell className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button onClick={() => handleShowVersions(ci)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Version History">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M10 2V18M10 2L6 6M10 2L14 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleShowRels(ci)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Relationships">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M3 10H17M17 10L13 6M17 10L13 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleEdit(ci)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Edit">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M14.166 2.5L17.499 5.833M1.666 18.333L2.916 13.75L14.166 2.5L17.499 5.833L6.249 17.083L1.666 18.333Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleDeleteClick(ci)} className="rounded-lg p-1.5 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:text-gray-400 dark:hover:bg-error-500/15 dark:hover:text-error-400" title="Delete">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M2.5 5H17.5M8.333 9.167V14.167M11.667 9.167V14.167M3.333 5L4.167 16.667C4.167 17.108 4.342 17.531 4.655 17.845C4.967 18.158 5.391 18.333 5.833 18.333H14.167C14.608 18.333 15.032 18.158 15.345 17.845C15.658 17.531 15.833 17.108 15.833 16.667L16.667 5M6.667 5V2.5C6.667 2.279 6.755 2.067 6.911 1.911C7.067 1.755 7.279 1.667 7.5 1.667H12.5C12.721 1.667 12.933 1.755 13.089 1.911C13.245 2.067 13.333 2.279 13.333 2.5V5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </ComponentCard>
      </div>

      {/* Create/Edit Modal */}
      <Modal isOpen={formModal.isOpen} onClose={formModal.closeModal} className="max-w-lg p-6 lg:p-8">
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">{isEdit ? "Edit CI" : "Create CI"}</h4>
        <div className="space-y-4">
          <div>
            <Label htmlFor="ci-name">Name</Label>
            <Input id="ci-name" placeholder="CI name" value={formName} onChange={(e) => setFormName(e.target.value)} disabled={formLoading} />
          </div>
          {!isEdit && (
            <>
              <div>
                <Label htmlFor="ci-type">Type</Label>
                <select id="ci-type" value={formType} onChange={(e) => setFormType(e.target.value)} disabled={formLoading} className={selectClasses}>
                  {CI_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <Label htmlFor="ci-org">Organization</Label>
                <select id="ci-org" value={formOrgId} onChange={(e) => setFormOrgId(e.target.value)} disabled={formLoading} className={selectClasses}>
                  {organizations.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
                </select>
              </div>
              <div>
                <Label htmlFor="ci-asset">Associated Asset</Label>
                <select id="ci-asset" value={formAssetId ?? ""} onChange={(e) => setFormAssetId(e.target.value || null)} disabled={formLoading} className={selectClasses}>
                  <option value="">None</option>
                  {assets.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                </select>
              </div>
            </>
          )}
          <div>
            <Label htmlFor="ci-status">Status</Label>
            <select id="ci-status" value={formStatus} onChange={(e) => setFormStatus(e.target.value)} disabled={formLoading} className={selectClasses}>
              {CI_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
          </div>
          <label className="flex cursor-pointer items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
            <input type="checkbox" checked={formException} onChange={(e) => setFormException(e.target.checked)} disabled={formLoading}
              className="h-4 w-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500 dark:border-gray-600" />
            Controlled Exception (no service linkage required)
          </label>
          {formError && <p className="text-sm text-error-500">{formError}</p>}
        </div>
        <div className="mt-6 flex items-center justify-end gap-3">
          <Button variant="outline" size="sm" onClick={formModal.closeModal} disabled={formLoading}>Cancel</Button>
          <Button size="sm" onClick={handleFormSubmit} disabled={formLoading}>{formLoading ? "Saving..." : isEdit ? "Update" : "Create"}</Button>
        </div>
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal isOpen={deleteModal.isOpen} onClose={() => { setDeleteError(""); deleteModal.closeModal(); }} className="max-w-sm p-6">
        <h4 className="mb-2 text-lg font-semibold text-gray-800 dark:text-white/90">Delete CI</h4>
        <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
          Are you sure you want to delete <span className="font-medium text-gray-700 dark:text-white/80">{deleteTarget?.name}</span>?
        </p>
        {deleteError && (
          <div className="mb-4 rounded-lg border border-error-500 bg-error-50 p-3 text-sm text-error-500 dark:border-error-500/30 dark:bg-error-500/15">{deleteError}</div>
        )}
        <div className="flex items-center justify-end gap-3">
          <Button variant="outline" size="sm" onClick={() => { setDeleteError(""); deleteModal.closeModal(); }} disabled={deleteLoading}>Cancel</Button>
          <Button size="sm" onClick={handleDeleteConfirm} disabled={deleteLoading} className="bg-error-500 hover:bg-error-600 disabled:bg-error-300">{deleteLoading ? "Deleting..." : "Delete"}</Button>
        </div>
      </Modal>

      {/* Version History Modal */}
      <Modal isOpen={versionModal.isOpen} onClose={versionModal.closeModal} className="max-w-lg p-6 lg:p-8">
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">Version History — {selectedCI?.name}</h4>
        {versions.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400">No versions found.</p>
        ) : (
          <div className="space-y-3 max-h-96 overflow-y-auto">
            {versions.map((v) => (
              <div key={v.id} className="rounded-lg border border-gray-100 px-4 py-3 dark:border-gray-800">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm font-medium text-gray-800 dark:text-white/90">v{v.versionNumber}</span>
                  <span className="inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{v.dataOrigin}</span>
                </div>
                <div className="flex gap-4 text-xs text-gray-400">
                  <span>Start: {formatDate(v.startDate)}</span>
                  <span>End: {formatDate(v.endDate)}</span>
                </div>
                {v.changeReference && <p className="text-xs text-gray-400 mt-1">Ref: {v.changeReference}</p>}
              </div>
            ))}
          </div>
        )}
      </Modal>

      {/* Relationships Modal */}
      <Modal isOpen={relModal.isOpen} onClose={relModal.closeModal} className="max-w-lg p-6 lg:p-8">
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">
          Relationships — {ciNameMap.get(relCIId ?? "") ?? "CI"}
        </h4>
        {relationships.length === 0 ? (
          <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">No relationships found.</p>
        ) : (
          <div className="mb-4 space-y-2">
            {relationships.map((rel) => (
              <div key={rel.id} className="flex items-center justify-between rounded-lg border border-gray-100 px-3 py-2 dark:border-gray-800">
                <div className="text-sm">
                  <span className="font-medium text-gray-800 dark:text-white/90">{ciNameMap.get(rel.sourceCIId) ?? rel.sourceCIId}</span>
                  <span className="mx-2 text-gray-400">→</span>
                  <span className="font-medium text-gray-800 dark:text-white/90">{ciNameMap.get(rel.targetCIId) ?? rel.targetCIId}</span>
                  <span className="ml-2 inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{rel.relationshipType}</span>
                </div>
                <button onClick={() => handleRemoveRel(rel.id)} className="rounded-lg p-1 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:hover:bg-error-500/15" title="Remove">
                  <svg width="14" height="14" viewBox="0 0 20 20" fill="none"><path d="M6 6L14 14M14 6L6 14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
                </button>
              </div>
            ))}
          </div>
        )}
        <div className="flex items-center gap-2">
          <select value={relTargetId} onChange={(e) => setRelTargetId(e.target.value)} className={selectClasses} style={{ flex: 1 }}>
            <option value="">Select target CI</option>
            {cis.filter((c) => c.id !== relCIId).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select value={relType} onChange={(e) => setRelType(e.target.value)} className={selectClasses} style={{ width: 160 }}>
            {RELATIONSHIP_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <Button size="sm" onClick={handleAddRel} disabled={relLoading || !relTargetId}>Add</Button>
        </div>
      </Modal>
    </>
  );
}
