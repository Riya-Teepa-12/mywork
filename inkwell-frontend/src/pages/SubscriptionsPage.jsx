import { useEffect, useMemo, useState } from "react";
import { CheckCircle2, CreditCard, ShieldCheck, Sparkles } from "lucide-react";
import { Link } from "react-router-dom";
import { authService } from "../lib/services.js";
import { useAuth } from "../context/AuthContext.jsx";
import { useNotification } from "../context/NotificationContext.jsx";

function formatCurrency(amountPaise, currency) {
  const amount = Number(amountPaise || 0) / 100;
  try {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: currency || "INR",
      maximumFractionDigits: 2,
    }).format(amount);
  } catch {
    return `${currency || "INR"} ${amount.toFixed(2)}`;
  }
}

function loadRazorpayScript() {
  if (window.Razorpay) {
    return Promise.resolve(true);
  }
  return new Promise((resolve) => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.async = true;
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

function SubscriptionsPage() {
  const { token, user } = useAuth();
  const notify = useNotification();
  const [plansData, setPlansData] = useState(null);
  const [entitlements, setEntitlements] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busyPlanType, setBusyPlanType] = useState("");
  const [error, setError] = useState("");

  const role = String(user?.role || "").toUpperCase();
  const isAdmin = role === "ADMIN";
  const isAuthor = role === "AUTHOR";

  const visiblePlans = useMemo(() => {
    const plans = Array.isArray(plansData?.plans) ? plansData.plans : [];
    if (isAdmin) {
      return [];
    }
    if (isAuthor) {
      return plans;
    }
    return plans.filter((plan) => String(plan.planType) === "NEWSLETTER");
  }, [isAdmin, isAuthor, plansData]);

  const load = async () => {
    const [plansRes, entitlementsRes] = await Promise.all([
      authService.getSubscriptionPlans(),
      authService.getMySubscriptionEntitlements(token),
    ]);
    setPlansData(plansRes || null);
    setEntitlements(entitlementsRes || null);
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
        setError(err.message || "Failed to load subscriptions");
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

  const isPlanActive = (planType) => {
    if (!entitlements) {
      return false;
    }
    if (planType === "AUTHOR_POSTS") {
      return Boolean(entitlements.authorPostSubscriptionActive);
    }
    return Boolean(entitlements.newsletterSubscriptionActive);
  };

  const isNewsletterIncluded = (planType) => {
    if (planType !== "NEWSLETTER") {
      return false;
    }
    return Boolean(entitlements?.authorPostSubscriptionActive);
  };

  const buyPlan = async (planType) => {
    if (isAdmin) {
      notify.info("Admin has full access. No purchase required.");
      return;
    }
    if (busyPlanType) {
      return;
    }
    if (isNewsletterIncluded(planType)) {
      notify.info("Newsletter entitlement is already included in your author post subscription.");
      return;
    }

    setBusyPlanType(planType);
    setError("");
    try {
      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded) {
        throw new Error("Unable to load Razorpay checkout script");
      }

      const order = await authService.createSubscriptionOrder({ planType }, token);
      if (!order?.providerOrderId || !order?.keyId) {
        throw new Error("Invalid payment order response");
      }

      await new Promise((resolve, reject) => {
        const razorpay = new window.Razorpay({
          key: order.keyId,
          amount: Number(order.amountPaise || 0),
          currency: order.currency || "INR",
          name: order.displayName || "InkWell",
          description: order.description || "Subscription",
          order_id: order.providerOrderId,
          prefill: {
            name: order.prefillName || user?.fullName || user?.username || "",
            email: order.prefillEmail || user?.email || "",
          },
          handler: async (response) => {
            try {
              await authService.verifySubscriptionPayment(
                {
                  providerOrderId: response.razorpay_order_id,
                  providerPaymentId: response.razorpay_payment_id,
                  providerSignature: response.razorpay_signature,
                },
                token
              );
              notify.success("Subscription activated successfully");
              await load();
              resolve();
            } catch (verifyErr) {
              reject(verifyErr);
            }
          },
          modal: {
            ondismiss: () => {
              reject(new Error("Payment cancelled"));
            },
          },
          theme: {
            color: "#2563eb",
          },
        });
        razorpay.open();
      });
    } catch (err) {
      const message = err.message || "Subscription payment failed";
      if (message !== "Payment cancelled") {
        setError(message);
        notify.error(message);
      } else {
        notify.info("Payment was cancelled");
      }
    } finally {
      setBusyPlanType("");
    }
  };

  return (
    <div className="container-shell pb-16">
      <header className="mb-8">
        <h1 className="hero-title text-3xl">Subscriptions</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Manage paid access for author posting limits and newsletter email delivery.
        </p>
      </header>

      {loading && <p className="mb-4 text-sm text-[var(--muted)]">Loading subscriptions...</p>}
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </p>
      )}

      {isAdmin && (
        <section className="glass edge-glow rounded-3xl p-6">
          <div className="flex items-center gap-3">
            <ShieldCheck className="h-5 w-5 text-emerald-500" />
            <h2 className="text-lg font-semibold">Admin Full Access</h2>
          </div>
          <p className="mt-3 text-sm text-[var(--muted)]">
            Admin accounts are exempt from subscription payments and can manage all paid features.
          </p>
          <Link to="/admin" className="btn-primary mt-4 inline-flex rounded-full px-4 py-2 text-xs">
            Go to Admin Panel
          </Link>
        </section>
      )}

      {!isAdmin && entitlements && (
        <section className="mb-6 grid gap-4 md:grid-cols-3">
          <article className="glass rounded-3xl p-5">
            <p className="text-xs uppercase tracking-[0.08em] text-[var(--muted)]">Role</p>
            <p className="mt-2 text-lg font-semibold">{entitlements.role}</p>
          </article>
          <article className="glass rounded-3xl p-5">
            <p className="text-xs uppercase tracking-[0.08em] text-[var(--muted)]">Free Posts Used</p>
            <p className="mt-2 text-lg font-semibold">
              {entitlements.freePostsUsed ?? 0} / {entitlements.freeAuthorPostLimit ?? 5}
            </p>
            <p className="mt-1 text-xs text-[var(--muted)]">
              Remaining: {entitlements.freePostsRemaining ?? 0}
            </p>
          </article>
          <article className="glass rounded-3xl p-5">
            <p className="text-xs uppercase tracking-[0.08em] text-[var(--muted)]">Newsletter Entitlement</p>
            <p className="mt-2 text-lg font-semibold">
              {entitlements.newsletterEntitled ? "Active" : "Inactive"}
            </p>
          </article>
        </section>
      )}

      {!isAdmin && (
        <section className="grid gap-4 md:grid-cols-2">
          {visiblePlans.map((plan) => {
            const active = isPlanActive(plan.planType);
            const included = isNewsletterIncluded(plan.planType);
            const disabled = active || included || busyPlanType === plan.planType;
            const buttonLabel = active
              ? "Already Active"
              : included
                ? "Included with Author Plan"
                : busyPlanType === plan.planType
                  ? "Processing..."
                  : "Buy Subscription";

            return (
              <article key={plan.planType} className="glass edge-glow rounded-3xl p-6">
                <div className="flex items-center justify-between gap-3">
                  <h2 className="text-lg font-semibold">{plan.label}</h2>
                  {active ? (
                    <span className="inline-flex items-center rounded-full bg-emerald-500/15 px-3 py-1 text-[11px] font-semibold text-emerald-600 dark:text-emerald-300">
                      <CheckCircle2 size={12} className="mr-1" />
                      Active
                    </span>
                  ) : (
                    <span className="chip rounded-full px-3 py-1 text-[11px]">Paid Plan</span>
                  )}
                </div>
                <p className="mt-3 text-sm text-[var(--muted)]">{plan.description}</p>
                <p className="mt-4 text-2xl font-semibold">
                  {formatCurrency(plan.amountPaise, plan.currency)}
                  <span className="ml-1 text-xs font-normal text-[var(--muted)]">/{plan.durationDays} days</span>
                </p>

                {plan.includesNewsletter && (
                  <p className="mt-2 inline-flex items-center text-xs text-[var(--primary)] ">
                    <Sparkles size={12} className="mr-1" />
                    Includes newsletter entitlement by default
                  </p>
                )}

                <button
                  type="button"
                  onClick={() => buyPlan(plan.planType)}
                  disabled={disabled}
                  className="btn-primary mt-5 inline-flex items-center rounded-full px-4 py-2 text-xs disabled:cursor-not-allowed disabled:opacity-60"
                >
                  <CreditCard size={13} className="mr-2" />
                  {buttonLabel}
                </button>
              </article>
            );
          })}
        </section>
      )}
    </div>
  );
}

export default SubscriptionsPage;

