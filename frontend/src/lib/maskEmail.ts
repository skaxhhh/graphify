/** UI_SPEC: 성공 시 이메일 마스킹 표시 */
export function maskEmail(email: string): string {
  const trimmed = email.trim();
  const at = trimmed.indexOf("@");
  if (at <= 0 || at >= trimmed.length - 1) {
    return "***@***";
  }
  const local = trimmed.slice(0, at);
  const domain = trimmed.slice(at + 1);
  return `${local.slice(0, 1)}***@${domain}`;
}
