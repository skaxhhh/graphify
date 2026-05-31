import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { resolvePostAuthPath } from "@/lib/authRedirect";
import type { AuthUser } from "@/types/auth";

interface LoginLocationState {
  from?: string;
}

export function usePostLoginRedirect() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();

  return (user: AuthUser) => {
    const target = resolvePostAuthPath(user);
    const fromState = (location.state as LoginLocationState | null)?.from;

    if (fromState && fromState.startsWith("/") && target === "/") {
      navigate(fromState, { replace: true });
      return;
    }

    if (target === "/") {
      const returnUrl = searchParams.get("returnUrl");
      if (returnUrl && returnUrl.startsWith("/")) {
        navigate(returnUrl, { replace: true });
        return;
      }
    }

    navigate(target, { replace: true });
  };
}
