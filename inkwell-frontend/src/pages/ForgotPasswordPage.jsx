import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { authService } from "../lib/services.js";
import { useNotification } from "../context/NotificationContext.jsx";

function ForgotPasswordPage() {
  const navigate = useNavigate();
  const notify = useNotification();
  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const sendOtp = async (event) => {
    event?.preventDefault?.();
    if (!email.trim()) {
      setError("Email is required");
      notify.info("Email is required");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await authService.requestForgotPasswordOtp(email.trim());
      setOtpSent(true);
      notify.success("If this email exists, OTP has been sent.");
    } catch (err) {
      const message = err.message || "Failed to send OTP";
      setError(message);
      notify.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const resetPassword = async (event) => {
    event.preventDefault();
    if (!otp.trim()) {
      setError("OTP is required");
      notify.info("OTP is required");
      return;
    }
    if (!newPassword) {
      setError("New password is required");
      notify.info("New password is required");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match");
      notify.info("Passwords do not match");
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await authService.resetForgotPassword({
        email: email.trim(),
        otp: otp.trim(),
        newPassword,
      });
      notify.success("Password changed successfully. Please login.");
      navigate("/login", {
        replace: true,
        state: { message: "Password changed successfully. Please login." },
      });
    } catch (err) {
      const message = err.message || "Failed to reset password";
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
        <h1 className="hero-title text-2xl">Forgot Password</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Enter your email, verify OTP, and set a new password.
        </p>

        <form onSubmit={otpSent ? resetPassword : sendOtp} className="mt-6 space-y-4">
          <div>
            <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@inkwell.app"
              required
              readOnly={otpSent}
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>

          {otpSent && (
            <>
              <div>
                <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                  OTP
                </label>
                <input
                  type="text"
                  value={otp}
                  onChange={(event) => setOtp(event.target.value)}
                  placeholder="Enter OTP"
                  required
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                  New Password
                </label>
                <input
                  type="password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  placeholder="********"
                  required
                  minLength={8}
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
              </div>
              <div>
                <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                  Confirm Password
                </label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  placeholder="********"
                  required
                  minLength={8}
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
              </div>
            </>
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
              ? otpSent
                ? "Resetting..."
                : "Sending OTP..."
              : otpSent
                ? "Change Password"
                : "Send OTP"}
          </button>
          {otpSent && (
            <button type="button" onClick={sendOtp} className="btn-ghost w-full rounded-2xl py-3 text-sm">
              Resend OTP
            </button>
          )}
        </form>
      </motion.div>
    </div>
  );
}

export default ForgotPasswordPage;
