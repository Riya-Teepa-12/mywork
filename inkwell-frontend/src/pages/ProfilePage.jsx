import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";
import { authService } from "../lib/services.js";

function ProfilePage() {
  const navigate = useNavigate();
  const { token, user, refreshProfile, logout } = useAuth();
  const [profileForm, setProfileForm] = useState({
    fullName: "",
    username: "",
    email: "",
    bio: "",
    avatarUrl: "",
  });
  const [passwordForm, setPasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
  });
  const [loading, setLoading] = useState(true);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [savingAuthorUpgrade, setSavingAuthorUpgrade] = useState(false);
  const [authorForm, setAuthorForm] = useState({
    bio: "",
    motivation: "",
    expertiseCategories: "",
    writingSampleUrls: "",
  });
  const [authorRequest, setAuthorRequest] = useState(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    Promise.allSettled([authService.getProfile(token), authService.getMyAuthorRequest(token)])
      .then((results) => {
        if (!active) {
          return;
        }
        const profileResult = results[0];
        if (profileResult.status !== "fulfilled") {
          throw profileResult.reason;
        }
        const profile = profileResult.value;
        setProfileForm({
          fullName: profile.fullName || "",
          username: profile.username || "",
          email: profile.email || "",
          bio: profile.bio || "",
          avatarUrl: profile.avatarUrl || "",
        });
        setAuthorForm((prev) => ({
          ...prev,
          bio: profile.bio || "",
        }));
        const requestResult = results[1];
        if (requestResult.status === "fulfilled") {
          setAuthorRequest(requestResult.value || null);
          setAuthorForm((prev) => ({
            ...prev,
            bio: requestResult.value?.bio || prev.bio,
            motivation: requestResult.value?.motivation || prev.motivation,
            expertiseCategories: Array.isArray(requestResult.value?.expertiseCategories)
              ? requestResult.value.expertiseCategories.join(", ")
              : prev.expertiseCategories,
            writingSampleUrls: Array.isArray(requestResult.value?.writingSampleUrls)
              ? requestResult.value.writingSampleUrls.join(", ")
              : prev.writingSampleUrls,
          }));
        } else {
          setAuthorRequest(null);
        }
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err.message || "Failed to load profile");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [token]);

  const handleProfileChange = (event) => {
    const { name, value } = event.target;
    setProfileForm((prev) => ({ ...prev, [name]: value }));
  };

  const handlePasswordChange = (event) => {
    const { name, value } = event.target;
    setPasswordForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleAuthorFormChange = (event) => {
    const { name, value } = event.target;
    setAuthorForm((prev) => ({ ...prev, [name]: value }));
  };

  const saveProfile = async () => {
    setSavingProfile(true);
    setError("");
    setSuccess("");
    try {
      await authService.updateProfile(
        {
          bio: profileForm.bio,
          avatarUrl: profileForm.avatarUrl,
        },
        token
      );
      await refreshProfile();
      setSuccess("Profile updated");
    } catch (err) {
      setError(err.message || "Failed to update profile");
    } finally {
      setSavingProfile(false);
    }
  };

  const updatePassword = async () => {
    setSavingPassword(true);
    setError("");
    setSuccess("");
    try {
      await authService.changePassword(passwordForm, token);
      setPasswordForm({ currentPassword: "", newPassword: "" });
      setSuccess("Password updated");
    } catch (err) {
      setError(err.message || "Failed to update password");
    } finally {
      setSavingPassword(false);
    }
  };

  const becomeAuthor = async () => {
    setSavingAuthorUpgrade(true);
    setError("");
    setSuccess("");
    try {
      const expertiseCategories = authorForm.expertiseCategories
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);
      const writingSampleUrls = authorForm.writingSampleUrls
        .split(",")
        .map((item) => item.trim())
        .filter(Boolean);

      const response = await authService.becomeAuthor(
        {
          bio: authorForm.bio,
          motivation: authorForm.motivation,
          expertiseCategories,
          writingSampleUrls,
        },
        token
      );
      setAuthorRequest(response || null);
      setSuccess("Author request submitted. Admin will review and notify you.");
    } catch (err) {
      setError(err.message || "Failed to upgrade account");
    } finally {
      setSavingAuthorUpgrade(false);
    }
  };

  const deactivateAccount = async () => {
    setError("");
    setSuccess("");
    try {
      await authService.deactivateAccount(token);
      logout();
      navigate("/", { replace: true });
    } catch (err) {
      setError(err.message || "Failed to deactivate account");
    }
  };

  const openAuthorPanel = async () => {
    setError("");
    try {
      await refreshProfile();
      navigate("/author");
    } catch (err) {
      setError(err.message || "Unable to open author panel");
    }
  };

  if (loading) {
    return (
      <div className="container-shell pb-16">
        <p className="text-sm text-[var(--muted)]">Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="container-shell pb-16">
      <header className="mb-8">
        <h1 className="hero-title text-3xl">Profile Settings</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Username, email, and full name are fixed. You can update bio, avatar, and password.
        </p>
      </header>

      {user && (
        <p className="mb-4 text-sm text-[var(--muted)]">
          Role: <span className="text-[var(--primary)] ">{user.role}</span>
        </p>
      )}
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
          {error}
        </p>
      )}
      {success && (
        <p className="mb-4 rounded-2xl border border-green-300/30 bg-green-500/10 px-4 py-3 text-sm text-green-500">
          {success}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_420px]">
        <section className="glass rounded-3xl p-5 md:p-6">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                Full Name
              </label>
              <input
                name="fullName"
                value={profileForm.fullName}
                readOnly
                className="w-full cursor-not-allowed rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-[var(--muted)] outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                Username
              </label>
              <input
                name="username"
                value={profileForm.username}
                readOnly
                className="w-full cursor-not-allowed rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-[var(--muted)] outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                Email
              </label>
              <input
                name="email"
                type="email"
                value={profileForm.email}
                readOnly
                className="w-full cursor-not-allowed rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm text-[var(--muted)] outline-none"
              />
            </div>
            <div>
              <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                Avatar URL
              </label>
              <input
                name="avatarUrl"
                value={profileForm.avatarUrl}
                onChange={handleProfileChange}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              />
            </div>
            <div className="md:col-span-2">
              <label className="mb-1 block text-xs uppercase tracking-[0.08em] text-[var(--muted)]">
                Bio
              </label>
              <textarea
                rows={4}
                name="bio"
                value={profileForm.bio}
                onChange={handleProfileChange}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              />
            </div>
          </div>
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              onClick={saveProfile}
              disabled={savingProfile}
              className="btn-primary rounded-full px-5 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
            >
              {savingProfile ? "Saving..." : "Save Profile"}
            </button>
            <button
              onClick={deactivateAccount}
              className="rounded-full border border-red-300/40 bg-red-500/10 px-5 py-2 text-sm text-red-200"
            >
              Deactivate Account
            </button>
          </div>

          {String(user?.role || "").toUpperCase() === "READER" && (
            <div className="mt-6 rounded-2xl border border-[var(--line)] bg-[var(--surface)]/50 p-4">
              <h3 className="text-base font-semibold">Become Author</h3>
              <p className="mt-1 text-xs text-[var(--muted)]">
                Fill required details and submit a request. Admin approval is required before role upgrade.
              </p>
              {authorRequest && (
                <div className="mt-3 rounded-xl border border-white/10 bg-white/5 p-3 text-xs">
                  <p>
                    Current Request Status:{" "}
                    <span className="font-semibold">{authorRequest.status}</span>
                  </p>
                  {authorRequest.decisionReason && (
                    <p className="mt-1 text-[var(--muted)]">
                      Decision note: {authorRequest.decisionReason}
                    </p>
                  )}
                </div>
              )}

              <div className="mt-3 grid gap-3">
                <textarea
                  rows={4}
                  name="bio"
                  value={authorForm.bio}
                  onChange={handleAuthorFormChange}
                  placeholder="Author bio (minimum 120 characters)"
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
                <textarea
                  rows={3}
                  name="motivation"
                  value={authorForm.motivation}
                  onChange={handleAuthorFormChange}
                  placeholder="Why do you want to become an author? (minimum 80 characters)"
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
                <input
                  name="expertiseCategories"
                  value={authorForm.expertiseCategories}
                  onChange={handleAuthorFormChange}
                  placeholder="Expertise categories (comma-separated)"
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
                <input
                  name="writingSampleUrls"
                  value={authorForm.writingSampleUrls}
                  onChange={handleAuthorFormChange}
                  placeholder="Writing sample URLs (comma-separated, http/https)"
                  className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                />
              </div>

              <button
                onClick={becomeAuthor}
                disabled={savingAuthorUpgrade || String(authorRequest?.status || "").toUpperCase() === "PENDING"}
                className="btn-primary mt-4 rounded-full px-5 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
              >
                {savingAuthorUpgrade
                  ? "Submitting..."
                  : String(authorRequest?.status || "").toUpperCase() === "PENDING"
                    ? "Request Pending"
                    : "Submit Author Request"}
              </button>
              {String(authorRequest?.status || "").toUpperCase() === "APPROVED" && (
                <button
                  onClick={openAuthorPanel}
                  className="btn-ghost mt-2 rounded-full px-5 py-2 text-sm"
                >
                  Open Author Panel
                </button>
              )}
            </div>
          )}
        </section>

        <aside className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-lg font-semibold">Change Password</h2>
          <div className="mt-4 space-y-3">
            <input
              placeholder="Current password"
              name="currentPassword"
              value={passwordForm.currentPassword}
              onChange={handlePasswordChange}
              type="password"
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
            <input
              placeholder="New password"
              name="newPassword"
              value={passwordForm.newPassword}
              onChange={handlePasswordChange}
              type="password"
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
            <button
              onClick={updatePassword}
              disabled={savingPassword}
              className="btn-ghost w-full rounded-2xl py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
            >
              {savingPassword ? "Updating..." : "Update Password"}
            </button>
          </div>
        </aside>
      </div>
    </div>
  );
}

export default ProfilePage;
