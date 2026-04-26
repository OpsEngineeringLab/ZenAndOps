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
import { useAssetApi } from "../../hooks/cmdb/useAssetApi";
import type { AssetResponse, CreateAssetRequest, UpdateAssetRequest, AssetFilters } from "../../hooks/cmdb/useAssetApi";
import { useAssetVersionApi } from "../../hooks/cmdb/useAssetVersionApi";
import type { AssetVersionResponse } from "../../hooks/cmdb/useAssetVersionApi";
import { useOrganizationApi } from "../../hooks/cmdb/useOrganizationApi";
import type { OrganizationResponse } from "../../hooks/cmdb/useOrganizationApi";
import { useModal } from "../../hooks/useModal";

const ASSET_TYPES = ["HARDWARE", "SOFTWARE", "CLOUD"];
const ASSET_STATUSES = ["ACTIVE", "INACTIVE", "RETIRED"];
const COST_TYPES = ["CAPEX", "OPEX"];

export default function AssetManagement() {
  const { create, update, remove, list } = useAssetApi();
  const { listByAsset } = useAssetVersionApi();
  const { list: listOrgs } = useOrganizationApi();

  const [assets, setAssets] = useState<AssetResponse[]>([]);
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<AssetFilters>({});

  const formModal = useModal();
  const deleteModal = useModal();
  const versionModal = useModal();

  const [selectedAsset, setSelectedAsset] = useState<AssetResponse | null>(null);
  const [isEdit, setIsEdit] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<AssetResponse | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [versions, setVersions] = useState<AssetVersionResponse[]>([]);

  // Form state
  const [formName, setFormName] = useState("");
  const [formType, setFormType] = useState("HARDWARE");
  const [formOrgId, setFormOrgId] = useState("");
  const [formCost, setFormCost] = useState("0");
  const [formCostType, setFormCostType] = useState("CAPEX");
  const [formAcquisitionDate, setFormAcquisitionDate] = useState("");
  const [formStatus, setFormStatus] = useState("ACTIVE");
  const [formSupplier, setFormSupplier] = useState("");
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState("");

  const fetchAssets = useCallback(async () => {
    setLoading(true);
    try {
      const cleanFilters: AssetFilters = {};
      if (filters.organizationId) cleanFilters.organizationId = filters.organizationId;
      if (filters.type) cleanFilters.type = filters.type;
      if (filters.costType) cleanFilters.costType = filters.costType;
      if (filters.status) cleanFilters.status = filters.status;
      if (filters.supplier) cleanFilters.supplier = filters.supplier;
      const data = await list(Object.keys(cleanFilters).length > 0 ? cleanFilters : undefined);
      setAssets(data);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [list, filters]);

  useEffect(() => { fetchAssets(); }, [fetchAssets]);
  useEffect(() => { listOrgs().then(setOrganizations).catch(() => {}); }, [listOrgs]);

  const orgMap = new Map(organizations.map((o) => [o.id, o.name]));

  const handleCreate = () => {
    setIsEdit(false); setSelectedAsset(null);
    setFormName(""); setFormType("HARDWARE"); setFormOrgId(organizations[0]?.id ?? "");
    setFormCost("0"); setFormCostType("CAPEX"); setFormAcquisitionDate(new Date().toISOString().split("T")[0]);
    setFormStatus("ACTIVE"); setFormSupplier(""); setFormError("");
    formModal.openModal();
  };

  const handleEdit = (asset: AssetResponse) => {
    setIsEdit(true); setSelectedAsset(asset);
    setFormName(asset.name); setFormType(asset.type); setFormOrgId(asset.organizationId);
    setFormCost(String(asset.cost)); setFormCostType(asset.costType);
    setFormAcquisitionDate(asset.acquisitionDate?.split("T")[0] ?? "");
    setFormStatus(asset.status); setFormSupplier(asset.supplier); setFormError("");
    formModal.openModal();
  };

  const handleFormSubmit = async () => {
    if (!formName.trim()) { setFormError("Name is required."); return; }
    setFormLoading(true); setFormError("");
    try {
      if (isEdit && selectedAsset) {
        const data: UpdateAssetRequest = {
          name: formName.trim(), cost: parseFloat(formCost) || 0,
          costType: formCostType, status: formStatus, supplier: formSupplier.trim(),
        };
        await update(selectedAsset.id, data);
      } else {
        const data: CreateAssetRequest = {
          name: formName.trim(), type: formType, organizationId: formOrgId,
          cost: parseFloat(formCost) || 0, costType: formCostType,
          acquisitionDate: formAcquisitionDate, status: formStatus, supplier: formSupplier.trim(),
        };
        await create(data);
      }
      formModal.closeModal();
      await fetchAssets();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setFormError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
    } finally {
      setFormLoading(false);
    }
  };

  const handleDeleteClick = (asset: AssetResponse) => {
    setDeleteTarget(asset); setDeleteError(""); deleteModal.openModal();
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setDeleteLoading(true); setDeleteError("");
    try {
      await remove(deleteTarget.id);
      deleteModal.closeModal();
      await fetchAssets();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } } };
      if (axiosErr?.response?.status === 409) {
        setDeleteError(axiosErr.response.data?.error?.message ?? "This asset is in use and cannot be deleted.");
      } else {
        setDeleteError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
      }
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleShowVersions = async (asset: AssetResponse) => {
    setSelectedAsset(asset);
    try {
      const v = await listByAsset(asset.id);
      setVersions(v);
    } catch {
      setVersions([]);
    }
    versionModal.openModal();
  };

  const selectClasses = "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";
  const filterSelectClasses = "h-9 rounded-lg border border-gray-300 bg-transparent px-3 py-1 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

  const statusColor = (s: string) => {
    switch (s) {
      case "ACTIVE": return "bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-500";
      case "RETIRED": return "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500";
      default: return "bg-gray-100 text-gray-700 dark:bg-white/5 dark:text-white/80";
    }
  };

  const formatDate = (d: string | null) => d ? new Date(d).toLocaleDateString() : "—";

  return (
    <>
      <PageMeta title="Asset Management | ZenAndOps" description="Manage IT assets" />
      <PageBreadcrumb pageTitle="Asset Management" />

      <div className="space-y-6">
        <ComponentCard title="Assets">
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <select value={filters.organizationId ?? ""} onChange={(e) => setFilters({ ...filters, organizationId: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Organizations</option>
              {organizations.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </select>
            <select value={filters.type ?? ""} onChange={(e) => setFilters({ ...filters, type: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Types</option>
              {ASSET_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <select value={filters.costType ?? ""} onChange={(e) => setFilters({ ...filters, costType: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Cost Types</option>
              {COST_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <select value={filters.status ?? ""} onChange={(e) => setFilters({ ...filters, status: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Statuses</option>
              {ASSET_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
            <div className="ml-auto">
              <Button size="sm" onClick={handleCreate}>+ Create Asset</Button>
            </div>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-10">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
            </div>
          ) : assets.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500 dark:text-gray-400">No assets found.</p>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Name</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Type</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Organization</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Cost</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Cost Type</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Status</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Supplier</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-right text-sm font-medium text-gray-500 dark:text-gray-400">Actions</TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {assets.map((asset) => (
                    <TableRow key={asset.id} className="border-t border-gray-100 dark:border-gray-800">
                      <TableCell className="px-4 py-3 text-sm font-medium text-gray-800 dark:text-white/90">{asset.name}</TableCell>
                      <TableCell className="px-4 py-3"><span className="inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{asset.type}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{orgMap.get(asset.organizationId) ?? asset.organizationId}</TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">${asset.cost.toLocaleString()}</TableCell>
                      <TableCell className="px-4 py-3"><span className="inline-flex items-center rounded-full bg-blue-light-50 px-2 py-0.5 text-xs font-medium text-blue-light-500 dark:bg-blue-light-500/15 dark:text-blue-light-500">{asset.costType}</span></TableCell>
                      <TableCell className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(asset.status)}`}>{asset.status}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{asset.supplier}</TableCell>
                      <TableCell className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button onClick={() => handleShowVersions(asset)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Version History">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M10 2V18M10 2L6 6M10 2L14 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleEdit(asset)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Edit">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M14.166 2.5L17.499 5.833M1.666 18.333L2.916 13.75L14.166 2.5L17.499 5.833L6.249 17.083L1.666 18.333Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleDeleteClick(asset)} className="rounded-lg p-1.5 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:text-gray-400 dark:hover:bg-error-500/15 dark:hover:text-error-400" title="Delete">
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
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">{isEdit ? "Edit Asset" : "Create Asset"}</h4>
        <div className="space-y-4">
          <div>
            <Label htmlFor="asset-name">Name</Label>
            <Input id="asset-name" placeholder="Asset name" value={formName} onChange={(e) => setFormName(e.target.value)} disabled={formLoading} />
          </div>
          {!isEdit && (
            <>
              <div>
                <Label htmlFor="asset-type">Type</Label>
                <select id="asset-type" value={formType} onChange={(e) => setFormType(e.target.value)} disabled={formLoading} className={selectClasses}>
                  {ASSET_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <Label htmlFor="asset-org">Organization</Label>
                <select id="asset-org" value={formOrgId} onChange={(e) => setFormOrgId(e.target.value)} disabled={formLoading} className={selectClasses}>
                  {organizations.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
                </select>
              </div>
              <div>
                <Label htmlFor="asset-acq">Acquisition Date</Label>
                <Input id="asset-acq" type="date" value={formAcquisitionDate} onChange={(e) => setFormAcquisitionDate(e.target.value)} disabled={formLoading} />
              </div>
            </>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="asset-cost">Cost</Label>
              <Input id="asset-cost" type="number" placeholder="0" value={formCost} onChange={(e) => setFormCost(e.target.value)} disabled={formLoading} />
            </div>
            <div>
              <Label htmlFor="asset-cost-type">Cost Type</Label>
              <select id="asset-cost-type" value={formCostType} onChange={(e) => setFormCostType(e.target.value)} disabled={formLoading} className={selectClasses}>
                {COST_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="asset-status">Status</Label>
              <select id="asset-status" value={formStatus} onChange={(e) => setFormStatus(e.target.value)} disabled={formLoading} className={selectClasses}>
                {ASSET_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div>
              <Label htmlFor="asset-supplier">Supplier</Label>
              <Input id="asset-supplier" placeholder="Supplier" value={formSupplier} onChange={(e) => setFormSupplier(e.target.value)} disabled={formLoading} />
            </div>
          </div>
          {formError && <p className="text-sm text-error-500">{formError}</p>}
        </div>
        <div className="mt-6 flex items-center justify-end gap-3">
          <Button variant="outline" size="sm" onClick={formModal.closeModal} disabled={formLoading}>Cancel</Button>
          <Button size="sm" onClick={handleFormSubmit} disabled={formLoading}>{formLoading ? "Saving..." : isEdit ? "Update" : "Create"}</Button>
        </div>
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal isOpen={deleteModal.isOpen} onClose={() => { setDeleteError(""); deleteModal.closeModal(); }} className="max-w-sm p-6">
        <h4 className="mb-2 text-lg font-semibold text-gray-800 dark:text-white/90">Delete Asset</h4>
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
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">
          Version History — {selectedAsset?.name}
        </h4>
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
                {v.description && <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">{v.description}</p>}
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
    </>
  );
}
