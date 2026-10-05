import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuth } from "../context/AuthContext.jsx";
import { useNotification } from "../context/NotificationContext.jsx";

const AUTH_SERVICE_BASE = import.meta.env.VITE_AUTH_SERVICE_BASE_URL || "http://localhost:8081";

function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const notify = useNotification();
  const [form, setForm] = useState({ email: "", password: "" });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const infoMessage = location.state?.registeredMessage || location.state?.message || "";
  const oauthError = searchParams.get("oauthError") || "";

  useEffect(() => {
    if (infoMessage) {
      notify.info(infoMessage, "Info");
    }
  }, [infoMessage, notify]);

  useEffect(() => {
    if (oauthError) {
      notify.error(oauthError, "OAuth Login Failed");
    }
  }, [oauthError, notify]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await login(form);
      notify.success("Login successful", "Welcome Back");
      navigate("/", { replace: true });
    } catch (err) {
      const message = err.message || "Login failed";
      setError(message);
      notify.error(message, "Login Failed");
      if (String(message).toLowerCase().includes("suspend")) {
        navigate("/suspended", { replace: true });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">

      <motion.div
        initial={{ opacity: 0, y: 28 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45 }}
        className="glass edge-glow w-full max-w-md rounded-4xl p-6 md:p-8"
      >
        <h1 className="hero-title text-2xl">Welcome Back</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Continue to your reader, author, or admin workspace.
        </p>
        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          <div>
            <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
              Email
            </label>
            <input
              type="email"
              name="email"
              value={form.email}
              onChange={handleChange}
              placeholder="you@inkwell.app"
              required
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
              Password
            </label>
            <input
              type="password"
              name="password"
              value={form.password}
              onChange={handleChange}
              placeholder="********"
              required
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>
          {error && (
            <p className="rounded-xl border border-red-300/30 bg-red-400/10 px-3 py-2 text-sm text-red-500">
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="btn-primary w-full rounded-2xl py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting ? "Logging in..." : "Login"}
          </button>
          <button
            type="button"
            className="btn-ghost w-full rounded-2xl py-3 text-sm"
            onClick={() => {
              window.location.href = `${AUTH_SERVICE_BASE}/oauth2/authorization/google`;
            }}
          >
            Continue with Google
          </button>
          <button
            type="button"
            className="btn-ghost w-full rounded-2xl py-3 text-sm"
            onClick={() => {
              window.location.href = `${AUTH_SERVICE_BASE}/oauth2/authorization/github`;
            }}
          >
            Continue with GitHub
          </button>
        </form>

        <p className="mt-5 text-center text-sm text-[var(--muted)]">
          <Link to="/forgot-password" className="text-[var(--primary)]  hover:text-white">
            Forgot password?
          </Link>
        </p>
        <p className="mt-3 text-center text-sm text-[var(--muted)]">
          New to InkWell?{" "}
          <Link to="/signup" className="text-[var(--primary)]  hover:text-white">
            Create account
          </Link>
        </p>
      </motion.div>
    </div>
  );
}

export default LoginPage;
