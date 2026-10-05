import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { useNotification } from "../context/NotificationContext.jsx";

function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { loginWithToken } = useAuth();
  const notify = useNotification();
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const hashParams = new URLSearchParams((window.location.hash || "").replace(/^#/, ""));
    const token =
      searchParams.get("token") ||
      searchParams.get("accessToken") ||
      searchParams.get("authToken") ||
      searchParams.get("jwt") ||
      hashParams.get("token") ||
      hashParams.get("accessToken");
    const oauthError =
      searchParams.get("oauthError") ||
      searchParams.get("error") ||
      searchParams.get("message") ||
      hashParams.get("error");
    if (oauthError) {
      setError(oauthError);
      notify.error(oauthError, "OAuth Login Failed");
      return;
    }
    if (!token) {
      const message = "OAuth token is missing";
      setError(message);
      notify.error(message, "OAuth Login Failed");
      return;
    }
    loginWithToken(token)
      .then(() => {
        if (!active) {
          return;
        }
        notify.success("Logged in with OAuth successfully", "Login Successful");
        navigate("/", { replace: true });
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        const message = err.message || "OAuth login failed";
        setError(message);
        notify.error(message, "OAuth Login Failed");
      });
    return () => {
      active = false;
    };
  }, [loginWithToken, navigate, notify, searchParams]);

  return (
    <div className="container-shell flex min-h-screen items-center justify-center pb-16">
      <div className="glass edge-glow w-full max-w-md rounded-3xl p-6 text-center">
        {!error && <p className="text-sm text-[var(--muted)]">Completing OAuth login...</p>}
        {error && (
          <>
            <p className="rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
              {error}
            </p>
            <button
              type="button"
              onClick={() => navigate("/login", { replace: true })}
              className="btn-primary mt-4 rounded-2xl px-4 py-2 text-sm"
            >
              Back to Login
            </button>
          </>
        )}
      </div>
    </div>
  );
}

export default OAuthCallbackPage;
