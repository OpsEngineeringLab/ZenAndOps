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
import { useServiceApi } from "../../hooks/cmdb/useServiceApi";
import type { ServiceResponse, CreateServiceRequest, UpdateServiceRequest, ServiceFilters } from "../../hooks/cmdb/useServiceApi";
import { useServiceDependencyApi } from "../../hooks/cmdb/useServiceDependencyApi";
import type { ServiceDependencyResponse, CreateServiceDependencyRequest } from "../../hooks/cmdb/useServiceDependencyApi";
import { useOrganizationApi } from "../../hooks/cmdb/useOrganizationApi";
import type { OrganizationResponse } from "../../hooks/cmdb/useOrganizationApi";
import { useModal } from "../../hooks/useModal";

const SERVICE_TYPES = ["DOMAIN", "BUSINESS", "TECHNICAL"];
const CRITICALITY_LEVELS = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
const SERVICE_STATUSES = ["ACTIVE", "INACTIVE", "DEPRECATED"];
const DEPENDENCY_TYPES = ["SYNCHRONOUS", "ASYNCHRONOUS", "CRITICAL"];

export default function ServiceManagement() {
  const { create, update, remove, list } = useServiceApi();
  const { create: createDep, remove: removeDep, listByService } = useServiceDependencyApi();
  const { list: listOrgs } = useOrganizationApi();

  const [services, setServices] = useState<ServiceResponse[]>([]);
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<ServiceFilters>({});

  const formModal = useModal();
  const deleteModal = useModal();
  const depModal = useModal();

  const [selectedService, setSelectedService] = useState<ServiceResponse | null>(null);
  const [isEdit, setIsEdit] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ServiceResponse | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Form state
  const [formName, setFormName] = useState("");
  const [formDescription, setFormDescription] = useState("");
  const [formType, setFormType] = useState("BUSINESS");
  const [formParentId, setFormParentId] = useState<string | null>(null);
  const [formOrgId, setFormOrgId] = useState("");
  const [formBusinessOwner, setFormBusinessOwner] = useState("");
  const [formTechnicalOwner, setFormTechnicalOwner] = useState("");
  const [formCriticality, setFormCriticality] = useState("MEDIUM");
  const [formStatus, setFormStatus] = useState("ACTIVE");
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState("");

  // Dependency state
  const [dependencies, setDependencies] = useState<ServiceDependencyResponse[]>([]);
  const [depServiceId, setDepServiceId] = useState<string | null>(null);
  const [depTargetId, setDepTargetId] = useState("");
  const [depType, setDepType] = useState("SYNCHRONOUS");
  const [depLoading, setDepLoading] = useState(false);

  const fetchServices = useCallback(async () => {
    setLoading(true);
    try {
      const cleanFilters: ServiceFilters = {};
      if (filters.organizationId) cleanFilters.organizationId = filters.organizationId;
      if (filters.type) cleanFilters.type = filters.type;
      if (filters.criticality) cleanFilters.criticality = filters.criticality;
      if (filters.status) cleanFilters.status = filters.status;
      const data = await list(Object.keys(cleanFilters).length > 0 ? cleanFilters : undefined);
      setServices(data);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [list, filters]);

  useEffect(() => {
    fetchServices();
  }, [fetchServices]);

  useEffect(() => {
    listOrgs().then(setOrganizations).catch(() => {});
  }, [listOrgs]);

  const orgMap = new Map(organizations.map((o) => [o.id, o.name]));

  const handleCreate = () => {
    setIsEdit(false);
    setSelectedService(null);
    setFormName(""); setFormDescription(""); setFormType("BUSINESS");
    setFormParentId(null); setFormOrgId(organizations[0]?.id ?? "");
    setFormBusinessOwner(""); setFormTechnicalOwner("");
    setFormCriticality("MEDIUM"); setFormStatus("ACTIVE"); setFormError("");
    formModal.openModal();
  };

  const handleEdit = (svc: ServiceResponse) => {
    setIsEdit(true);
    setSelectedService(svc);
    setFormName(svc.name); setFormDescription(svc.description); setFormType(svc.type);
    setFormParentId(svc.parentId); setFormOrgId(svc.organizationId);
    setFormBusinessOwner(svc.businessOwner); setFormTechnicalOwner(svc.technicalOwner);
    setFormCriticality(svc.criticality); setFormStatus(svc.status); setFormError("");
    formModal.openModal();
  };

  const handleFormSubmit = async () => {
    if (!formName.trim() || !formBusinessOwner.trim() || !formTechnicalOwner.trim()) {
      setFormError("Name, business owner, and technical owner are required.");
      return;
    }
    setFormLoading(true); setFormError("");
    try {
      if (isEdit && selectedService) {
        const data: UpdateServiceRequest = {
          name: formName.trim(), description: formDescription.trim(),
          businessOwner: formBusinessOwner.trim(), technicalOwner: formTechnicalOwner.trim(),
          criticality: formCriticality, status: formStatus,
        };
        await update(selectedService.id, data);
      } else {
        const data: CreateServiceRequest = {
          name: formName.trim(), description: formDescription.trim(), type: formType,
          parentId: formParentId || null, organizationId: formOrgId,
          businessOwner: formBusinessOwner.trim(), technicalOwner: formTechnicalOwner.trim(),
          criticality: formCriticality, status: formStatus,
        };
        await create(data);
      }
      formModal.closeModal();
      await fetchServices();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setFormError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
    } finally {
      setFormLoading(false);
    }
  };

  const handleDeleteClick = (svc: ServiceResponse) => {
    setDeleteTarget(svc); setDeleteError(""); deleteModal.openModal();
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setDeleteLoading(true); setDeleteError("");
    try {
      await remove(deleteTarget.id);
      deleteModal.closeModal();
      await fetchServices();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } } };
      if (axiosErr?.response?.status === 409) {
        setDeleteError(axiosErr.response.data?.error?.message ?? "This service is in use and cannot be deleted.");
      } else {
        setDeleteError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
      }
    } finally {
      setDeleteLoading(false);
    }
  };

  const handleShowDeps = async (svc: ServiceResponse) => {
    setDepServiceId(svc.id);
    setDepTargetId(""); setDepType("SYNCHRONOUS");
    try {
      const deps = await listByService(svc.id);
      setDependencies(deps);
    } catch {
      setDependencies([]);
    }
    depModal.openModal();
  };

  const handleAddDep = async () => {
    if (!depServiceId || !depTargetId) return;
    setDepLoading(true);
    try {
      const data: CreateServiceDependencyRequest = {
        sourceServiceId: depServiceId, targetServiceId: depTargetId, dependencyType: depType,
      };
      await createDep(data);
      const deps = await listByService(depServiceId);
      setDependencies(deps);
      setDepTargetId("");
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setDepLoading(false);
    }
  };

  const handleRemoveDep = async (depId: string) => {
    if (!depServiceId) return;
    try {
      await removeDep(depId);
      const deps = await listByService(depServiceId);
      setDependencies(deps);
    } catch {
      // Error handled by ApiClient interceptor
    }
  };

  const svcNameMap = new Map(services.map((s) => [s.id, s.name]));

  const selectClasses = "h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";
  const filterSelectClasses = "h-9 rounded-lg border border-gray-300 bg-transparent px-3 py-1 text-sm text-gray-800 focus:border-brand-300 focus:outline-hidden dark:border-gray-700 dark:bg-gray-900 dark:text-white/90";

  const criticalityColor = (c: string) => {
    switch (c) {
      case "CRITICAL": return "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500";
      case "HIGH": return "bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-orange-400";
      case "MEDIUM": return "bg-blue-light-50 text-blue-light-500 dark:bg-blue-light-500/15 dark:text-blue-light-500";
      default: return "bg-gray-100 text-gray-700 dark:bg-white/5 dark:text-white/80";
    }
  };

  const statusColor = (s: string) => {
    switch (s) {
      case "ACTIVE": return "bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-500";
      case "DEPRECATED": return "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500";
      default: return "bg-gray-100 text-gray-700 dark:bg-white/5 dark:text-white/80";
    }
  };

  return (
    <>
      <PageMeta title="Service Management | ZenAndOps" description="Manage services and dependencies" />
      <PageBreadcrumb pageTitle="Service Management" />

      <div className="space-y-6">
        <ComponentCard title="Services">
          {/* Filters */}
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <select value={filters.organizationId ?? ""} onChange={(e) => setFilters({ ...filters, organizationId: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Organizations</option>
              {organizations.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </select>
            <select value={filters.type ?? ""} onChange={(e) => setFilters({ ...filters, type: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Types</option>
              {SERVICE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
            <select value={filters.criticality ?? ""} onChange={(e) => setFilters({ ...filters, criticality: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Criticality</option>
              {CRITICALITY_LEVELS.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
            <select value={filters.status ?? ""} onChange={(e) => setFilters({ ...filters, status: e.target.value || undefined })} className={filterSelectClasses}>
              <option value="">All Statuses</option>
              {SERVICE_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
            </select>
            <div className="ml-auto">
              <Button size="sm" onClick={handleCreate}>+ Create Service</Button>
            </div>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-10">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
            </div>
          ) : services.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500 dark:text-gray-400">No services found.</p>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Name</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Type</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Organization</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Criticality</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Status</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Business Owner</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">Technical Owner</TableCell>
                    <TableCell isHeader className="px-4 py-3 text-right text-sm font-medium text-gray-500 dark:text-gray-400">Actions</TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {services.map((svc) => (
                    <TableRow key={svc.id} className="border-t border-gray-100 dark:border-gray-800">
                      <TableCell className="px-4 py-3 text-sm font-medium text-gray-800 dark:text-white/90">{svc.name}</TableCell>
                      <TableCell className="px-4 py-3"><span className="inline-flex items-center rounded-full bg-brand-50 px-2 py-0.5 text-xs font-medium text-brand-600 dark:bg-brand-500/15 dark:text-brand-400">{svc.type}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{orgMap.get(svc.organizationId) ?? svc.organizationId}</TableCell>
                      <TableCell className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${criticalityColor(svc.criticality)}`}>{svc.criticality}</span></TableCell>
                      <TableCell className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(svc.status)}`}>{svc.status}</span></TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{svc.businessOwner}</TableCell>
                      <TableCell className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">{svc.technicalOwner}</TableCell>
                      <TableCell className="px-4 py-3 text-right">
                        <div className="flex items-center justify-end gap-2">
                          <button onClick={() => handleShowDeps(svc)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Dependencies">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M3 10H17M17 10L13 6M17 10L13 14" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleEdit(svc)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Edit">
                            <svg width="18" height="18" viewBox="0 0 20 20" fill="none"><path d="M14.166 2.5L17.499 5.833M1.666 18.333L2.916 13.75L14.166 2.5L17.499 5.833L6.249 17.083L1.666 18.333Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                          </button>
                          <button onClick={() => handleDeleteClick(svc)} className="rounded-lg p-1.5 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:text-gray-400 dark:hover:bg-error-500/15 dark:hover:text-error-400" title="Delete">
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
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">{isEdit ? "Edit Service" : "Create Service"}</h4>
        <div className="space-y-4">
          <div>
            <Label htmlFor="svc-name">Name</Label>
            <Input id="svc-name" placeholder="Service name" value={formName} onChange={(e) => setFormName(e.target.value)} disabled={formLoading} />
          </div>
          <div>
            <Label htmlFor="svc-desc">Description</Label>
            <Input id="svc-desc" placeholder="Description" value={formDescription} onChange={(e) => setFormDescription(e.target.value)} disabled={formLoading} />
          </div>
          {!isEdit && (
            <>
              <div>
                <Label htmlFor="svc-type">Type</Label>
                <select id="svc-type" value={formType} onChange={(e) => setFormType(e.target.value)} disabled={formLoading} className={selectClasses}>
                  {SERVICE_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                </select>
              </div>
              <div>
                <Label htmlFor="svc-org">Organization</Label>
                <select id="svc-org" value={formOrgId} onChange={(e) => setFormOrgId(e.target.value)} disabled={formLoading} className={selectClasses}>
                  {organizations.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
                </select>
              </div>
              <div>
                <Label htmlFor="svc-parent">Parent Service</Label>
                <select id="svc-parent" value={formParentId ?? ""} onChange={(e) => setFormParentId(e.target.value || null)} disabled={formLoading} className={selectClasses}>
                  <option value="">None</option>
                  {services.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
            </>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="svc-bo">Business Owner</Label>
              <Input id="svc-bo" placeholder="Business owner" value={formBusinessOwner} onChange={(e) => setFormBusinessOwner(e.target.value)} disabled={formLoading} />
            </div>
            <div>
              <Label htmlFor="svc-to">Technical Owner</Label>
              <Input id="svc-to" placeholder="Technical owner" value={formTechnicalOwner} onChange={(e) => setFormTechnicalOwner(e.target.value)} disabled={formLoading} />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <Label htmlFor="svc-crit">Criticality</Label>
              <select id="svc-crit" value={formCriticality} onChange={(e) => setFormCriticality(e.target.value)} disabled={formLoading} className={selectClasses}>
                {CRITICALITY_LEVELS.map((c) => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <Label htmlFor="svc-status">Status</Label>
              <select id="svc-status" value={formStatus} onChange={(e) => setFormStatus(e.target.value)} disabled={formLoading} className={selectClasses}>
                {SERVICE_STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
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
        <h4 className="mb-2 text-lg font-semibold text-gray-800 dark:text-white/90">Delete Service</h4>
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

      {/* Dependencies Modal */}
      <Modal isOpen={depModal.isOpen} onClose={depModal.closeModal} className="max-w-lg p-6 lg:p-8">
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">
          Dependencies — {svcNameMap.get(depServiceId ?? "") ?? "Service"}
        </h4>
        {dependencies.length === 0 ? (
          <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">No dependencies found.</p>
        ) : (
          <div className="mb-4 space-y-2">
            {dependencies.map((dep) => (
              <div key={dep.id} className="flex items-center justify-between rounded-lg border border-gray-100 px-3 py-2 dark:border-gray-800">
                <div className="text-sm">
                  <span className="font-medium text-gray-800 dark:text-white/90">{svcNameMap.get(dep.sourceServiceId) ?? dep.sourceServiceId}</span>
                  <span className="mx-2 text-gray-400">→</span>
                  <span className="font-medium text-gray-800 dark:text-white/90">{svcNameMap.get(dep.targetServiceId) ?? dep.targetServiceId}</span>
                  <span className={`ml-2 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${dep.dependencyType === "CRITICAL" ? "bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500" : "bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400"}`}>{dep.dependencyType}</span>
                </div>
                <button onClick={() => handleRemoveDep(dep.id)} className="rounded-lg p-1 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:hover:bg-error-500/15" title="Remove">
                  <svg width="14" height="14" viewBox="0 0 20 20" fill="none"><path d="M6 6L14 14M14 6L6 14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
                </button>
              </div>
            ))}
          </div>
        )}
        <div className="flex items-center gap-2">
          <select value={depTargetId} onChange={(e) => setDepTargetId(e.target.value)} className={selectClasses} style={{ flex: 1 }}>
            <option value="">Select target service</option>
            {services.filter((s) => s.id !== depServiceId).map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
          <select value={depType} onChange={(e) => setDepType(e.target.value)} className={selectClasses} style={{ width: 160 }}>
            {DEPENDENCY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
          <Button size="sm" onClick={handleAddDep} disabled={depLoading || !depTargetId}>Add</Button>
        </div>
      </Modal>
    </>
  );
}
