import { useState, useEffect, useCallback } from "react";
import PageMeta from "../components/common/PageMeta";
import PageBreadcrumb from "../components/common/PageBreadCrumb";
import ComponentCard from "../components/common/ComponentCard";
import Button from "../components/ui/button/Button";
import Badge from "../components/ui/badge/Badge";
import Label from "../components/form/Label";
import Input from "../components/form/input/InputField";
import { useProfileApi } from "../hooks/useProfileApi";
import type { ProfileResponse } from "../hooks/useProfileApi";

export default function Profile() {
  const { getProfile, updateProfile, changePassword } = useProfileApi();

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);

  // Profile form state
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [profileMessage, setProfileMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [profileSaving, setProfileSaving] = useState(false);

  // Password form state
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordMessage, setPasswordMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordMatchError, setPasswordMatchError] = useState(false);

  const fetchProfile = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getProfile();
      setProfile(data);
      setName(data.name);
      setEmail(data.email);
    } catch {
      // Error handled by ApiClient interceptor
    } finally {
      setLoading(false);
    }
  }, [getProfile]);

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  const handleProfileSubmit = async () => {
    setProfileMessage(null);
    setProfileSaving(true);
    try {
      const updated = await updateProfile({ name, email });
      setProfile(updated);
      setProfileMessage({ type: "success", text: "Profile updated successfully." });
    } catch {
      setProfileMessage({ type: "error", text: "Failed to update profile. Please try again." });
    } finally {
      setProfileSaving(false);
    }
  };

  const handlePasswordSubmit = async () => {
    setPasswordMessage(null);
    setPasswordMatchError(false);

    if (newPassword !== confirmPassword) {
      setPasswordMatchError(true);
      return;
    }

    setPasswordSaving(true);
    try {
      await changePassword({ currentPassword, newPassword });
      setPasswordMessage({ type: "success", text: "Password changed successfully." });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch {
      setPasswordMessage({ type: "error", text: "Failed to change password. Please verify your current password." });
    } finally {
      setPasswordSaving(false);
    }
  };

  if (loading) {
    return (
      <>
        <PageMeta title="Profile | ZenAndOps" description="View and edit your profile" />
        <PageBreadcrumb pageTitle="Profile" />
        <div className="flex items-center justify-center py-10">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-500 border-t-transparent" />
        </div>
      </>
    );
  }

  return (
    <>
      <PageMeta title="Profile | ZenAndOps" description="View and edit your profile" />
      <PageBreadcrumb pageTitle="Profile" />

      <div className="space-y-6">
        {/* Profile Information Card */}
        <ComponentCard title="Profile Information">
          <div className="space-y-5">
            {/* Login (read-only) */}
            <div>
              <Label>Login</Label>
              <p className="text-sm text-gray-800 dark:text-white/90">{profile?.login}</p>
            </div>

            {/* Name (editable) */}
            <div>
              <Label htmlFor="profile-name">Name</Label>
              <Input
                id="profile-name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Your name"
              />
            </div>

            {/* Email (editable) */}
            <div>
              <Label htmlFor="profile-email">Email</Label>
              <Input
                id="profile-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="Your email"
              />
            </div>

            {/* Roles (read-only badges) */}
            <div>
              <Label>Roles</Label>
              <div className="flex flex-wrap gap-2">
                {profile?.roles.map((role) => (
                  <Badge key={role} variant="light" size="sm" color="primary">
                    {role}
                  </Badge>
                ))}
                {(!profile?.roles || profile.roles.length === 0) && (
                  <span className="text-sm text-gray-500 dark:text-gray-400">No roles assigned</span>
                )}
              </div>
            </div>

            {/* Tags (read-only badges) */}
            <div>
              <Label>Tags</Label>
              <div className="flex flex-wrap gap-2">
                {profile?.tags.map((tag) => (
                  <Badge key={tag.id} variant="light" size="sm" color="info">
                    {tag.key}:{tag.value}
                  </Badge>
                ))}
                {(!profile?.tags || profile.tags.length === 0) && (
                  <span className="text-sm text-gray-500 dark:text-gray-400">No tags assigned</span>
                )}
              </div>
            </div>

            {/* Profile message */}
            {profileMessage && (
              <p className={`text-sm ${profileMessage.type === "success" ? "text-success-500" : "text-error-500"}`}>
                {profileMessage.text}
              </p>
            )}

            {/* Save button */}
            <div className="flex justify-end">
              <Button size="sm" onClick={handleProfileSubmit} disabled={profileSaving}>
                {profileSaving ? "Saving..." : "Save Profile"}
              </Button>
            </div>
          </div>
        </ComponentCard>

        {/* Change Password Card */}
        <ComponentCard title="Change Password">
          <div className="space-y-5">
            <div>
              <Label htmlFor="current-password">Current Password</Label>
              <Input
                id="current-password"
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="Enter current password"
              />
            </div>

            <div>
              <Label htmlFor="new-password">New Password</Label>
              <Input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Enter new password"
              />
            </div>

            <div>
              <Label htmlFor="confirm-password">Confirm New Password</Label>
              <Input
                id="confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  setPasswordMatchError(false);
                }}
                placeholder="Confirm new password"
                error={passwordMatchError}
                hint={passwordMatchError ? "Passwords do not match." : undefined}
              />
            </div>

            {/* Password message */}
            {passwordMessage && (
              <p className={`text-sm ${passwordMessage.type === "success" ? "text-success-500" : "text-error-500"}`}>
                {passwordMessage.text}
              </p>
            )}

            {/* Change Password button */}
            <div className="flex justify-end">
              <Button size="sm" onClick={handlePasswordSubmit} disabled={passwordSaving}>
                {passwordSaving ? "Changing..." : "Change Password"}
              </Button>
            </div>
          </div>
        </ComponentCard>
      </div>
    </>
  );
}
