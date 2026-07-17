// src/member/utils/groupInvite.ts
const GROUP_INVITE_CHARS =
  "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

export function generateGroupInviteCode(length: number = 10): string {
  let body = "";
  for (let i = 0; i < length; i++) {
    const idx = Math.floor(Math.random() * GROUP_INVITE_CHARS.length);
    body += GROUP_INVITE_CHARS[idx];
  }
  return `cedal-${body}`;
}
