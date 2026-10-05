import { MailWarning, ShieldAlert } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

function SuspendedAccountPage() {
  const navigate = useNavigate();
  const { logout } = useAuth();

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden px-4">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_25%_20%,rgba(244,63,94,0.24),transparent_45%),radial-gradient(circle_at_90%_10%,rgba(59,130,246,0.2),transparent_40%)]" />
      <div className="glass edge-glow relative z-10 w-full max-w-lg rounded-3xl p-8 text-center">
        <div className="mx-auto mb-4 inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-500/15 text-rose-500">
          <ShieldAlert size={26} />
        </div>
        <h1 className="hero-title text-2xl md:text-3xl">Your account has been suspended</h1>
        <p className="mt-3 text-sm text-[var(--muted)]">
          Use another account to continue, or contact support to review this action.
        </p>
        <div className="mt-6 grid gap-2 sm:grid-cols-2">
          <button
            type="button"
            onClick={() => {
              logout();
              navigate("/login", { replace: true });
            }}
            className="btn-primary rounded-2xl px-4 py-3 text-sm"
          >
            Login with another account
          </button>
          <a
            href="mailto:support@inkwell.app"
            className="btn-ghost inline-flex items-center justify-center rounded-2xl px-4 py-3 text-sm"
          >
            <MailWarning size={15} className="mr-2" />
            Contact Us
          </a>
        </div>
      </div>
    </div>
  );
}

export default SuspendedAccountPage;
