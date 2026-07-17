// src/hooks/useUserPowers.ts
import { useUserProfile } from "./useUserProfile";

export function useUserPowers() {
  const { user, profile, loading, error } = useUserProfile();

  const role = profile?.role;

  const isOwner = role === "owner";
  const isDeveloper = isOwner || role === "developer"; // owner has dev power
  const isUser = !!user; // any signed-in account

  return {
    user,
    profile,
    loading,
    error,
    isOwner,
    isDeveloper,
    isUser,
  };
}
