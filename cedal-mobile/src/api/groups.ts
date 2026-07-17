import { db } from "@/src/api/firebase";
import {
  addDoc,
  arrayUnion,
  arrayRemove,
  collection,
  doc,
  getDocs,
  increment,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

export type CreateGroupInput = {
  name: string;
  description: string;
  tags: string[];
  isPrivate: boolean;
  serverId: string;
  inviteCode?: string;
};

export type MembershipRole = "owner" | "admin" | "president" | "vp" | "member";

const LEAVE_COOLDOWN_HOURS = 12;

/**
 * Ensure a membership document under:
 * users/{uid}/groups/{groupId}
 */
async function upsertUserGroupMembership(
  uid: string,
  groupId: string,
  role: MembershipRole,
  groupSnapshot?: {
    name: string;
    description?: string;
    tags?: string[];
    isPrivate?: boolean;
    serverId?: string;
  },
) {
  const userGroupRef = doc(db, "users", uid, "groups", groupId);

  await setDoc(
    userGroupRef,
    {
      groupId,
      groupName: groupSnapshot?.name ?? "",
      description: groupSnapshot?.description ?? "",
      tags: groupSnapshot?.tags ?? [],
      isPrivate: groupSnapshot?.isPrivate ?? false,
      region: groupSnapshot?.serverId ?? "EU",

      role,
      isActive: true,

      messageCount: 0,
      reportsMade: 0,
      reportsReceived: 0,

      joinedAt: serverTimestamp(),
    },
    { merge: true },
  );
}

/**
 * Create a new group for a user.
 */
export async function createGroupForUser(
  uid: string,
  input: CreateGroupInput,
) {
  const groupsCol = collection(db, "groups");

  const groupRef = await addDoc(groupsCol, {
    name: input.name,
    description: input.description,
    tags: input.tags,
    isPrivate: input.isPrivate,
    serverId: input.serverId,

    ownerId: uid,
    admins: [uid],

    inviteCode: input.isPrivate ? input.inviteCode ?? null : null,
    createdAt: serverTimestamp(),

    membersCount: 1,
    members: [uid],

    lastMessageAt: null,
    lastMessage: "",
    botsCount: 0,
  });

  const groupId = groupRef.id;

  const userRef = doc(db, "users", uid);
  await updateDoc(userRef, {
    ownedGroups: arrayUnion(groupId),
    ownedGroupCount: increment(1),
  });

  await upsertUserGroupMembership(uid, groupId, "owner", {
    name: input.name,
    description: input.description,
    tags: input.tags,
    isPrivate: input.isPrivate,
    serverId: input.serverId,
  });

  return groupId;
}

/**
 * Join private group via cedal-... invite code.
 */
export async function joinGroupByInviteCode(uid: string, inviteCode: string) {
  const groupsCol = collection(db, "groups");

  const q = query(groupsCol, where("inviteCode", "==", inviteCode));
  const snap = await getDocs(q);

  if (snap.empty) {
    throw new Error("Invite link is invalid or expired.");
  }

  const groupDoc = snap.docs[0];
  const groupId = groupDoc.id;
  const data = groupDoc.data() as any;

  if (!data.isPrivate) {
    throw new Error("This invite is not for a private group.");
  }

  const alreadyMember =
    Array.isArray(data.members) && data.members.includes(uid);

  if (!alreadyMember) {
    const groupRef = doc(db, "groups", groupId);
    await updateDoc(groupRef, {
      members: arrayUnion(uid),
      membersCount: increment(1),
    });
  }

  await upsertUserGroupMembership(uid, groupId, "member", {
    name: data.name,
    description: data.description,
    tags: data.tags ?? [],
    isPrivate: data.isPrivate ?? true,
    serverId: data.serverId ?? "EU",
  });

  return groupId;
}

/**
 * Leave group, and if owner, apply a 12h create cooldown.
 */
export async function leaveGroupWithCooldown(
  uid: string,
  groupId: string,
  isOwner: boolean,
) {
  const groupRef = doc(db, "groups", groupId);
  const userRef = doc(db, "users", uid);
  const userGroupRef = doc(db, "users", uid, "groups", groupId);

  await updateDoc(groupRef, {
    members: arrayRemove(uid),
    membersCount: increment(-1),
  });

  await setDoc(
    userGroupRef,
    {
      isActive: false,
      leftAt: serverTimestamp(),
    },
    { merge: true },
  );

  if (isOwner) {
    const cooldownUntil = new Date(
      Date.now() + LEAVE_COOLDOWN_HOURS * 60 * 60 * 1000,
    );
    await updateDoc(userRef, {
      nextGroupCreateAt: cooldownUntil,
    });
  }
}
