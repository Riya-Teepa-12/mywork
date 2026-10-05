import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { newsletterService } from "../lib/services.js";
import { useAuth } from "../context/AuthContext.jsx";
import { useNotification } from "../context/NotificationContext.jsx";
import { authService } from "../lib/services.js";

const preferenceKeys = ["engineering", "product", "growth", "community"];

function NewsletterPage() {
  const { token, user } = useAuth();
  const notify = useNotification();
  const normalizedRole = String(user?.role || "").toUpperCase();
  const isAdmin = normalizedRole === "ADMIN";
  const [email, setEmail] = useState(user?.email || "");
  const [fullName, setFullName] = useState(user?.fullName || "");
  const [preferences, setPreferences] = useState({
    engineering: true,
    product: true,
    growth: false,
    community: true,
  });
  const [subscriber, setSubscriber] = useState(null);
  const [activeCount, setActiveCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [tokenInput, setTokenInput] = useState("");
  const [pendingAction, setPendingAction] = useState("");
  const [entitlements, setEntitlements] = useState(null);

  const selectedPreferences = useMemo(
    () => preferenceKeys.filter((key) => preferences[key]),
    [preferences]
  );
  const canUseNewsletter = isAdmin || Boolean(entitlements?.newsletterEntitled);
  const subscriptionStatusLabel = loading
    ? "Loading..."
    : subscriber?.status || "NOT_SUBSCRIBED";

  const load = async () => {
    const entitlementRes = await authService.getMySubscriptionEntitlements(token);
    setEntitlements(entitlementRes || null);

    const [mySubscription, countRes] = await Promise.all([
      newsletterService.getMe(token),
      isAdmin ? newsletterService.count("ACTIVE", token) : Promise.resolve(null),
    ]);

    setSubscriber(mySubscription || null);
    setActiveCount(isAdmin ? countRes?.count || 0 : 0);

    const cacheEmail = (mySubscription?.email || email || "").toLowerCase();
    const cachedToken =
      localStorage.getItem(`inkwell_newsletter_token_${cacheEmail}`) || "";
    const effectiveToken = mySubscription?.token || cachedToken || "";
    setTokenInput(effectiveToken);
    if (mySubscription?.token) {
      localStorage.setItem(
        `inkwell_newsletter_token_${cacheEmail}`,
        mySubscription.token
      );
    }

    if (mySubscription?.preferences) {
      const next = { engineering: false, product: false, growth: false, community: false };
      mySubscription.preferences.forEach((value) => {
        if (value in next) {
          next[value] = true;
        }
      });
      setPreferences(next);
    }
  };

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    load()
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err.message || "Failed to load newsletter data");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [token, isAdmin]);

  const togglePreference = (key) => {
    setPreferences((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const subscribe = async () => {
    if (pendingAction) {
      return;
    }
    if (!canUseNewsletter) {
      notify.info("Buy newsletter subscription first");
      return;
    }
    setError("");
    if (!email.trim()) {
      setError("Email is required");
      notify.info("Email is required");
      return;
    }
    setPendingAction("subscribe");
    try {
      const response = await newsletterService.subscribe(
        {
          email,
          userId: user?.userId || null,
          fullName,
          preferences: selectedPreferences,
        },
        token
      );
      setSubscriber(response);
      setTokenInput(response.token || "");
      if (response?.token) {
        localStorage.setItem(`inkwell_newsletter_token_${(email || "").toLowerCase()}`, response.token);
      }
      notify.success("Subscription initiated. Check your email for confirmation.");
      await load();
    } catch (err) {
      const message = err.message || "Failed to subscribe";
      setError(message);
      notify.error(message);
    } finally {
      setPendingAction("");
    }
  };

  const savePreferences = async () => {
    if (pendingAction) {
      return;
    }
    if (!canUseNewsletter) {
      notify.info("Buy newsletter subscription first");
      return;
    }
    setError("");
    setPendingAction("preferences");
    try {
      const response = await newsletterService.updatePreferences(
        {
          email,
          preferences: selectedPreferences,
        },
        token
      );
      setSubscriber(response);
      notify.success("Preferences saved");
      await load();
    } catch (err) {
      const message = err.message || "Failed to update preferences";
      setError(message);
      notify.error(message);
    } finally {
      setPendingAction("");
    }
  };

  const confirmSubscription = async () => {
    if (pendingAction) {
      return;
    }
    if (!canUseNewsletter) {
      notify.info("Buy newsletter subscription first");
      return;
    }
    setError("");
    if (!tokenInput.trim()) {
      setError("Confirmation token is required");
      notify.info("Confirmation token is required");
      return;
    }
    setPendingAction("confirm");
    try {
      const response = await newsletterService.confirm(tokenInput);
      setSubscriber(response);
      if (response?.token) {
        localStorage.setItem(`inkwell_newsletter_token_${(email || "").toLowerCase()}`, response.token);
      }
      notify.success("Subscription confirmed");
      await load();
    } catch (err) {
      const message = err.message || "Failed to confirm subscription";
      setError(message);
      notify.error(message);
    } finally {
      setPendingAction("");
    }
  };

  const unsubscribe = async () => {
    if (pendingAction) {
      return;
    }
    if (!canUseNewsletter) {
      notify.info("Buy newsletter subscription first");
      return;
    }
    setError("");
    if (!tokenInput.trim()) {
      setError("Unsubscribe token is required");
      notify.info("Unsubscribe token is required");
      return;
    }
    setPendingAction("unsubscribe");
    try {
      await newsletterService.unsubscribe(tokenInput);
      setSubscriber((prev) => (prev ? { ...prev, status: "UNSUBSCRIBED" } : prev));
      notify.success("Unsubscribed successfully");
      await load();
    } catch (err) {
      const message = err.message || "Failed to unsubscribe";
      setError(message);
      notify.error(message);
    } finally {
      setPendingAction("");
    }
  };

  return (
    <div className="container-shell pb-16">
      <header className="mb-8">
        <h1 className="hero-title text-3xl">Newsletter Preferences</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Manage double opt-in subscription preferences and campaign topics.
        </p>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Flow: Subscribe, then confirm token, then status becomes ACTIVE, then admin sends newsletters and post alerts.
        </p>
        <p className="mt-2 text-xs text-[var(--primary)] ">
          Active Subscribers: {isAdmin ? activeCount : "Available for admin"}
        </p>
      </header>

      {loading && <p className="mb-3 text-sm text-[var(--muted)]">Loading newsletter data...</p>}
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
          {error}
        </p>
      )}
      {!canUseNewsletter && (
        <p className="mb-4 rounded-2xl border border-amber-300/40 bg-amber-500/10 px-4 py-3 text-sm text-amber-700 dark:text-amber-200">
          Newsletter entitlement is inactive.{" "}
          <Link to="/subscriptions" className="underline">
            Buy subscription
          </Link>{" "}
          to continue.
        </p>
      )}

      <section className="grid gap-6 lg:grid-cols-[1fr_400px]">
        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Subscription</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="you@inkwell.app"
              className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
            <input
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              placeholder="Full name"
              className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <button
              onClick={subscribe}
              disabled={Boolean(pendingAction) || !canUseNewsletter}
              className="btn-primary rounded-2xl px-4 py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
            >
              {pendingAction === "subscribe" ? "Subscribing..." : "Subscribe"}
            </button>
            <button
              onClick={savePreferences}
              disabled={Boolean(pendingAction) || !canUseNewsletter}
              className="btn-ghost rounded-2xl px-4 py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
            >
              {pendingAction === "preferences" ? "Saving..." : "Save Preferences"}
            </button>
          </div>
          <p className="mt-4 text-xs text-[var(--muted)]">
            Subscription status:{" "}
            <span className="text-[var(--primary)] ">{subscriptionStatusLabel}</span>
          </p>
          <div className="mt-4 space-y-2">
            <input
              value={tokenInput}
              onChange={(event) => setTokenInput(event.target.value)}
              placeholder="Confirmation token"
              className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
            />
            <div className="flex flex-wrap gap-2">
              <button
                onClick={confirmSubscription}
                disabled={Boolean(pendingAction) || !canUseNewsletter}
                className="btn-ghost rounded-2xl px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
              >
                {pendingAction === "confirm" ? "Confirming..." : "Confirm"}
              </button>
              <button
                onClick={unsubscribe}
                disabled={Boolean(pendingAction) || !canUseNewsletter}
                className="rounded-2xl border border-red-500/70 bg-red-500/20 px-4 py-2 text-sm font-semibold text-red-700 disabled:cursor-not-allowed disabled:opacity-60 dark:text-red-200"
              >
                {pendingAction === "unsubscribe" ? "Unsubscribing..." : "Unsubscribe"}
              </button>
            </div>
          </div>
        </article>

        <aside className="glass rounded-3xl p-5 md:p-6">
          <h3 className="text-lg font-semibold">Preference Tags</h3>
          <div className="mt-4 space-y-3">
            {preferenceKeys.map((key) => (
              <label
                key={key}
                className="flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-4 py-3"
              >
                <span className="text-sm capitalize">{key}</span>
                <button
                  onClick={() => togglePreference(key)}
                  className={`rounded-full px-3 py-1 text-xs ${
                    preferences[key] ? "btn-primary" : "btn-ghost"
                  }`}
                >
                  {preferences[key] ? "On" : "Off"}
                </button>
              </label>
            ))}
          </div>
        </aside>
      </section>
    </div>
  );
}

export default NewsletterPage;
