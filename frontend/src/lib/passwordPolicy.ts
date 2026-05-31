export interface PasswordPolicyResult {
  minLength: boolean;
  hasLetter: boolean;
  hasDigit: boolean;
  meetsPolicy: boolean;
  strength: 0 | 1 | 2 | 3 | 4;
  hint: string;
}

export function evaluatePasswordPolicy(password: string): PasswordPolicyResult {
  const minLength = password.length >= 8;
  const hasLetter = /[A-Za-z]/.test(password);
  const hasDigit = /\d/.test(password);
  const meetsPolicy = minLength;

  let strength: 0 | 1 | 2 | 3 | 4 = 0;
  if (password.length > 0) strength = 1;
  if (minLength) strength = 2;
  if (minLength && hasLetter && hasDigit) strength = 3;
  if (minLength && hasLetter && hasDigit && password.length >= 12) strength = 4;

  const hint = meetsPolicy
    ? "8자 이상, 영문·숫자 조합을 권장합니다."
    : "비밀번호는 8자 이상이어야 합니다.";

  return {
    minLength,
    hasLetter,
    hasDigit,
    meetsPolicy,
    strength,
    hint,
  };
}
