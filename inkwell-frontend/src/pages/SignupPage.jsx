import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { useAuth } from "../context/AuthContext.jsx";
import { useNotification } from "../context/NotificationContext.jsx";
import { authService } from "../lib/services.js";

const EMAIL_REGEX = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
const PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,120}$/;

function validateSignupForm(form) {
  if (!String(form.fullName || "").trim()) {
    return "Full name is required";
  }
  if (!String(form.username || "").trim()) {
    return "Username is required";
  }
  if (!EMAIL_REGEX.test(String(form.email || "").trim())) {
    return "Enter a valid email address";
  }
  if (!PASSWORD_REGEX.test(String(form.password || ""))) {
    return "Password must contain 1 uppercase letter, 1 number, and 1 special character";
  }
  return "";
}

function SignupPage() {
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const notify = useNotification();
  const [form, setForm] = useState({
    fullName: "",
    username: "",
    email: "",
    password: "",
  });
  const [otp, setOtp] = useState("");
  const [otpRequested, setOtpRequested] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const requestOtp = async (event) => {
    event?.preventDefault?.();
    const validationError = validateSignupForm(form);
    if (validationError) {
      setError(validationError);
      notify.info(validationError);
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await authService.requestSignupOtp(form);
      setOtpRequested(true);
      notify.success("OTP sent to your email. Enter OTP to finish signup.");
    } catch (err) {
      const message = err.message || "Failed to send OTP";
      setError(message);
      notify.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const verifyOtpAndSignup = async (event) => {
    event.preventDefault();
    if (!otp.trim()) {
      setError("Enter OTP");
      notify.info("Please enter the OTP");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      const response = await authService.verifySignupOtp({
        email: form.email,
        otp: otp.trim(),
      });
      if (response?.accessToken) {
        await loginWithToken(response.accessToken);
      }
      notify.success("Account created successfully", "Welcome");
      navigate("/", { replace: true });
    } catch (err) {
      const message = err.message || "OTP verification failed";
      setError(message);
      notify.error(message);
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
        <h1 className="hero-title text-2xl">Create Account</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Signup now with email OTP verification.
        </p>

        <form onSubmit={otpRequested ? verifyOtpAndSignup : requestOtp} className="mt-6 space-y-4">
          <div>
            <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
              Full Name
            </label>
            <input
              type="text"
              name="fullName"
              value={form.fullName}
              onChange={handleChange}
              placeholder="Your name"
              required
              readOnly={otpRequested}
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
              Username
            </label>
            <input
              type="text"
              name="username"
              value={form.username}
              onChange={handleChange}
              placeholder="@username"
              required
              readOnly={otpRequested}
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>
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
              pattern="[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"
              title="Enter a valid email address, for example name@example.com"
              readOnly={otpRequested}
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
              minLength={8}
              pattern="(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,120}"
              title="Use 8+ characters with at least 1 uppercase letter, 1 number, and 1 special character"
              readOnly={otpRequested}
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>
          {otpRequested && (
            <div>
              <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                OTP
              </label>
              <input
                type="text"
                value={otp}
                onChange={(event) => setOtp(event.target.value)}
                placeholder="Enter 6-digit OTP"
                required
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              />
            </div>
          )}
          {error && (
            <p className="rounded-xl border border-red-300/30 bg-red-400/10 px-3 py-2 text-sm text-red-200">
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="btn-primary w-full rounded-2xl py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            {submitting
              ? otpRequested
                ? "Verifying..."
                : "Sending OTP..."
              : otpRequested
                ? "Verify OTP & Create Account"
                : "Send OTP"}
          </button>
          {otpRequested && (
            <button
              type="button"
              onClick={requestOtp}
              disabled={submitting}
              className="btn-ghost w-full rounded-2xl py-3 text-sm"
            >
              Resend OTP
            </button>
          )}
        </form>

        <p className="mt-5 text-center text-sm text-[var(--muted)]">
          Already have an account?{" "}
          <Link to="/login" className="text-[var(--primary)]  hover:text-white">
            Login
          </Link>
        </p>
      </motion.div>
    </div>
  );
}

export default SignupPage;
