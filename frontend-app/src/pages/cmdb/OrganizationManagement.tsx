import { useState, useEffect, useCallback } from "react";
import PageMeta from "../../components/common/PageMeta";
import PageBreadcrumb from "../../components/common/PageBreadCrumb";
import ComponentCard from "../../components/common/ComponentCard";
import Button from "../../components/ui/button/Button";
import { Modal } from "../../components/ui/modal";
import Label from "../../components/form/Label";
import Input from "../../components/form/input/InputField";
import { useOrganizationApi } from "../../hooks/cmdb/useOrganizationApi";
import type {
  OrganizationResponse,
  CreateOrganizationRequest,
  UpdateOrganizationRequest,
} from "../../hooks/cmdb/useOrganizationApi";
import { useModal } from "../../hooks/useModal";

const ORG_TYPES = ["ROOT", "BUSINESS_UNIT", "DEPARTMENT", "TEAM"];

interface OrgTreeNode extends OrganizationResponse {
  children: OrgTreeNode[];
}

function buildTree(orgs: OrganizationResponse[]): OrgTreeNode[] {
  const map = new Map<string, OrgTreeNode>();
  const roots: OrgTreeNode[] = [];
  orgs.forEach((o) => map.set(o.id, { ...o, children: [] }));
  orgs.forEach((o) => {
    const node = map.get(o.id)!;
    if (o.parentId && map.has(o.parentId)) {
      map.get(o.parentId)!.children.push(node);
    } else {
      roots.push(node);
    }
  });
  return roots;
}

export default function OrganizationManagement() {
  const { create, update, remove, list } = useOrganizationApi();
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [loading, setLoading] = useState(true);

  const formModal = useModal();
  const deleteModal = useModal();
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editFields, setEditFields] = useState<{ name: string; responsiblePerson: string; costCenter: string }>({ name: "", responsiblePerson: "", costCenter: "" });
  const [deleteTarget, setDeleteTarget] = useState<OrganizationResponse | null>(null);
  const [deleteError, setDeleteError] = useState("");
  const [deleteLoading, setDeleteLoading] = useState(false);

  // Create form state
  const [formName, setFormName] = useState("");
  const [formType, setFormType] = useState("BUSINESS_UNIT");
  const [formParentId, setFormParentId] = useState<string | null>(null);
  const [formResponsible, setFormResponsible] = useState("");
  const [formCostCenter, setFormCostCenter] = useState("");
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState("");

  const fetchOrganizations = useCallback(async () => {
    setLoading(true);
    try {
      const data = await list();
      setOrganizations(data);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [list]);

  useEffect(() => {
    fetchOrganizations();
  }, [fetchOrganizations]);

  const tree = buildTree(organizations);

  const handleCreate = () => {
    setFormName("");
    setFormType("BUSINESS_UNIT");
    setFormParentId(null);
    setFormResponsible("");
    setFormCostCenter("");
    setFormError("");
    formModal.openModal();
  };

  const handleFormSubmit = async () => {
    if (!formName.trim()) {
      setFormError("Name is required.");
      return;
    }
    setFormLoading(true);
    setFormError("");
    try {
      const data: CreateOrganizationRequest = {
        name: formName.trim(),
        type: formType,
        parentId: formParentId || null,
        responsiblePerson: formResponsible.trim(),
        costCenter: formCostCenter.trim(),
      };
      await create(data);
      formModal.closeModal();
      await fetchOrganizations();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { error?: { message?: string } } } };
      setFormError(axiosErr?.response?.data?.error?.message ?? "An error occurred.");
    } finally {
      setFormLoading(false);
    }
  };

  const startInlineEdit = (org: OrganizationResponse) => {
    setEditingId(org.id);
    setEditFields({ name: org.name, responsiblePerson: org.responsiblePerson, costCenter: org.costCenter });
  };

  const cancelInlineEdit = () => {
    setEditingId(null);
  };

  const saveInlineEdit = async (id: string) => {
    try {
      const data: UpdateOrganizationRequest = {
        name: editFields.name.trim(),
        responsiblePerson: editFields.responsiblePerson.trim(),
        costCenter: editFields.costCenter.trim(),
      };
      await update(id, data);
      setEditingId(null);
      await fetchOrganizations();
    } catch {
      // Error handled by ApiClient interceptor
    }
  };

  const handleDeleteClick = (org: OrganizationResponse) => {
    setDeleteTarget(org);
    setDeleteError("");
    deleteModal.openModal();
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    setDeleteLoading(true);
    setDeleteError("");
    try {
      await remove(deleteTarget.id);
      deleteModal.closeModal();
      await fetchOrganizations();
    } catch (err: unknown) {
      const axiosErr = err as { response?: { status?: number; data?: { error?: { message?: string } } } };
      if (axiosErr?.response?.status === 409) {
        setDeleteError(axiosErr.response.data?.error?.message ?? "This organization is in use and cannot be deleted.");
      } else {
        setDeleteError(axiosErr?.response?.data?.error?.message ?? "An error occurred while deleting.");
      }
    } finally {
      setDeleteLoading(false);
    }
  };

  const renderTreeNode = (node: OrgTreeNode, depth: number = 0) => {
    const isEditing = editingId === node.id;
    return (
      <div key={node.id} style={{ marginLeft: depth * 24 }} className="py-2">
        <div className="flex items-center gap-3 rounded-lg border border-gray-100 bg-white px-4 py-3 dark:border-gray-800 dark:bg-white/[0.03]">
          {/* Type badge */}
          <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
            node.type === "ROOT"
              ? "bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400"
              : node.type === "BUSINESS_UNIT"
              ? "bg-blue-light-50 text-blue-light-500 dark:bg-blue-light-500/15 dark:text-blue-light-500"
              : node.type === "DEPARTMENT"
              ? "bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-orange-400"
              : "bg-success-50 text-success-600 dark:bg-success-500/15 dark:text-success-500"
          }`}>
            {node.type}
          </span>

          {isEditing ? (
            <div className="flex flex-1 items-center gap-2">
              <input
                className="h-8 rounded border border-gray-300 px-2 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
                value={editFields.name}
                onChange={(e) => setEditFields({ ...editFields, name: e.target.value })}
                placeholder="Name"
              />
              <input
                className="h-8 rounded border border-gray-300 px-2 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
                value={editFields.responsiblePerson}
                onChange={(e) => setEditFields({ ...editFields, responsiblePerson: e.target.value })}
                placeholder="Responsible"
              />
              <input
                className="h-8 rounded border border-gray-300 px-2 text-sm dark:border-gray-700 dark:bg-gray-900 dark:text-white/90"
                value={editFields.costCenter}
                onChange={(e) => setEditFields({ ...editFields, costCenter: e.target.value })}
                placeholder="Cost Center"
              />
              <button onClick={() => saveInlineEdit(node.id)} className="rounded-lg p-1.5 text-success-500 hover:bg-success-50 dark:hover:bg-success-500/15" title="Save">
                <svg width="16" height="16" viewBox="0 0 20 20" fill="none"><path d="M4 10L8 14L16 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
              </button>
              <button onClick={cancelInlineEdit} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800" title="Cancel">
                <svg width="16" height="16" viewBox="0 0 20 20" fill="none"><path d="M6 6L14 14M14 6L6 14" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
              </button>
            </div>
          ) : (
            <>
              <span className="flex-1 text-sm font-medium text-gray-800 dark:text-white/90">{node.name}</span>
              <span className="text-xs text-gray-500 dark:text-gray-400">{node.responsiblePerson}</span>
              <span className="text-xs text-gray-500 dark:text-gray-400">{node.costCenter}</span>
              <div className="flex items-center gap-1">
                <button onClick={() => startInlineEdit(node)} className="rounded-lg p-1.5 text-gray-500 hover:bg-gray-100 hover:text-brand-500 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-brand-400" title="Edit">
                  <svg width="16" height="16" viewBox="0 0 20 20" fill="none"><path d="M14.166 2.5L17.499 5.833M1.666 18.333L2.916 13.75L14.166 2.5L17.499 5.833L6.249 17.083L1.666 18.333Z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                </button>
                <button onClick={() => handleDeleteClick(node)} className="rounded-lg p-1.5 text-gray-500 hover:bg-error-50 hover:text-error-500 dark:text-gray-400 dark:hover:bg-error-500/15 dark:hover:text-error-400" title="Delete">
                  <svg width="16" height="16" viewBox="0 0 20 20" fill="none"><path d="M2.5 5H17.5M8.333 9.167V14.167M11.667 9.167V14.167M3.333 5L4.167 16.667C4.167 17.108 4.342 17.531 4.655 17.845C4.967 18.158 5.391 18.333 5.833 18.333H14.167C14.608 18.333 15.032 18.158 15.345 17.845C15.658 17.531 15.833 17.108 15.833 16.667L16.667 5M6.667 5V2.5C6.667 2.279 6.755 2.067 6.911 1.911C7.067 1.755 7.279 1.667 7.5 1.667H12.5C12.721 1.667 12.933 1.755 13.089 1.911C13.245 2.067 13.333 2.279 13.333 2.5V5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>
                </button>
              </div>
            </>
          )}
        </div>
        {node.children.map((child) => renderTreeNode(child, depth + 1))}
      </div>
    );
  };

  return (
    <>
      <PageMeta title="Organization Management | ZenAndOps" description="Manage organizational hierarchy" />
      <PageBreadcrumb pageTitle="Organization Management" />

      <div className="space-y-6">
        <ComponentCard title="Organizations">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              {organizations.length} organization{organizations.length !== 1 ? "s" : ""} total
            </p>
            <Button size="sm" onClick={handleCreate}>+ Create Organization</Button>
          </div>

          {loading ? (
            <div className="flex items-center justify-center py-10">
              <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
            </div>
          ) : tree.length === 0 ? (
            <p className="py-6 text-center text-sm text-gray-500 dark:text-gray-400">
              No organizations found. Create one to get started.
            </p>
          ) : (
            <div>{tree.map((node) => renderTreeNode(node))}</div>
          )}
        </ComponentCard>
      </div>

      {/* Create Modal */}
      <Modal isOpen={formModal.isOpen} onClose={formModal.closeModal} className="max-w-md p-6 lg:p-8">
        <h4 className="mb-5 text-lg font-semibold text-gray-800 dark:text-white/90">Create Organization</h4>
        <div className="space-y-4">
          <div>
            <Label htmlFor="org-name">Name</Label>
            <Input id="org-name" placeholder="Organization name" value={formName} onChange={(e) => setFormName(e.target.value)} disabled={formLoading} />
          </div>
          <div>
            <Label htmlFor="org-type">Type</Label>
            <select id="org-type" value={formType} onChange={(e) => setFormType(e.target.value)} disabled={formLoading}
              className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90">
              {ORG_TYPES.map((t) => (<option key={t} value={t}>{t}</option>))}
            </select>
          </div>
          <div>
            <Label htmlFor="org-parent">Parent Organization</Label>
            <select id="org-parent" value={formParentId ?? ""} onChange={(e) => setFormParentId(e.target.value || null)} disabled={formLoading}
              className="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/20 dark:border-gray-700 dark:bg-gray-900 dark:text-white/90">
              <option value="">None (Root level)</option>
              {organizations.map((o) => (<option key={o.id} value={o.id}>{o.name}</option>))}
            </select>
          </div>
          <div>
            <Label htmlFor="org-responsible">Responsible Person</Label>
            <Input id="org-responsible" placeholder="Responsible person" value={formResponsible} onChange={(e) => setFormResponsible(e.target.value)} disabled={formLoading} />
          </div>
          <div>
            <Label htmlFor="org-cost-center">Cost Center</Label>
            <Input id="org-cost-center" placeholder="Cost center" value={formCostCenter} onChange={(e) => setFormCostCenter(e.target.value)} disabled={formLoading} />
          </div>
          {formError && <p className="text-sm text-error-500">{formError}</p>}
        </div>
        <div className="mt-6 flex items-center justify-end gap-3">
          <Button variant="outline" size="sm" onClick={formModal.closeModal} disabled={formLoading}>Cancel</Button>
          <Button size="sm" onClick={handleFormSubmit} disabled={formLoading}>{formLoading ? "Creating..." : "Create"}</Button>
        </div>
      </Modal>

      {/* Delete Confirm Modal */}
      <Modal isOpen={deleteModal.isOpen} onClose={() => { setDeleteError(""); deleteModal.closeModal(); }} className="max-w-sm p-6">
        <h4 className="mb-2 text-lg font-semibold text-gray-800 dark:text-white/90">Delete Organization</h4>
        <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
          Are you sure you want to delete <span className="font-medium text-gray-700 dark:text-white/80">{deleteTarget?.name}</span>? This action cannot be undone.
        </p>
        {deleteError && (
          <div className="mb-4 rounded-lg border border-error-500 bg-error-50 p-3 text-sm text-error-500 dark:border-error-500/30 dark:bg-error-500/15">{deleteError}</div>
        )}
        <div className="flex items-center justify-end gap-3">
          <Button variant="outline" size="sm" onClick={() => { setDeleteError(""); deleteModal.closeModal(); }} disabled={deleteLoading}>Cancel</Button>
          <Button size="sm" onClick={handleDeleteConfirm} disabled={deleteLoading} className="bg-error-500 hover:bg-error-600 disabled:bg-error-300">
            {deleteLoading ? "Deleting..." : "Delete"}
          </Button>
        </div>
      </Modal>
    </>
  );
}
