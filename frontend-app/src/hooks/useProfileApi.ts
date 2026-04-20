import { useCallback } from "react";
import apiClient from "../api/ApiClient";

export interface ProfileTagResponse {
  id: string;
  key: string;
  value: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProfileResponse {
  login: string;
  name: string;
  email: string;
  roles: string[];
  tags: ProfileTagResponse[];
}

export interface UpdateProfileRequest {
  name: string;
  email: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export function useProfileApi() {
  const getProfile = useCallback(async (): Promise<ProfileResponse> => {
    const response = await apiClient.get<ProfileResponse>("/api/v1/profile");
    return response.data;
  }, []);

  const updateProfile = useCallback(async (data: UpdateProfileRequest): Promise<ProfileResponse> => {
    const response = await apiClient.put<ProfileResponse>("/api/v1/profile", data);
    return response.data;
  }, []);

  const changePassword = useCallback(async (data: ChangePasswordRequest): Promise<void> => {
    await apiClient.post("/api/v1/profile/password", data);
  }, []);

  return { getProfile, updateProfile, changePassword };
}
