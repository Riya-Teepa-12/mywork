import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { CheckCircle2, Info, TriangleAlert, X } from "lucide-react";

const NotificationContext = createContext(null);

const toneStyles = {
  success: "border-emerald-200/70 bg-emerald-100/95 text-emerald-900 dark:border-emerald-700/70 dark:bg-emerald-950/85 dark:text-emerald-100",
  error: "border-rose-200/70 bg-rose-100/95 text-rose-900 dark:border-rose-700/70 dark:bg-rose-950/85 dark:text-rose-100",
  info: "border-sky-200/70 bg-sky-100/95 text-sky-900 dark:border-sky-700/70 dark:bg-sky-950/85 dark:text-sky-100",
};

function iconForType(type) {
  if (type === "success") return CheckCircle2;
  if (type === "error") return TriangleAlert;
  return Info;
}

function dispatchGlobalNotification(detail) {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new CustomEvent("inkwell:notify", { detail }));
}

export function notifySuccess(message, title = "Success", durationMs = 3600) {
  dispatchGlobalNotification({ type: "success", title, message, durationMs });
}

export function notifyError(message, title = "Error", durationMs = 4200) {
  dispatchGlobalNotification({ type: "error", title, message, durationMs });
}

export function notifyInfo(message, title = "Info", durationMs = 3400) {
  dispatchGlobalNotification({ type: "info", title, message, durationMs });
}

export function NotificationProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const recentToastsRef = useRef(new Map());

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((item) => item.id !== id));
  }, []);

  const notify = useCallback(({ type = "info", title = "", message = "", durationMs = 3600 }) => {
    const safeMessage = String(message || "").trim();
    if (!safeMessage) {
      return;
    }
    const now = Date.now();
    const dedupeKey = `${String(type || "info").toLowerCase()}:${safeMessage.toLowerCase()}`;
    const recentToasts = recentToastsRef.current;
    const lastShownAt = recentToasts.get(dedupeKey);
    if (lastShownAt && now - lastShownAt < 1800) {
      return;
    }
    recentToasts.set(dedupeKey, now);
    recentToasts.forEach((shownAt, key) => {
      if (now - shownAt > 15000) {
        recentToasts.delete(key);
      }
    });
    const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    setToasts((prev) => [...prev, { id, type, title, message: safeMessage }]);
    window.setTimeout(() => removeToast(id), Math.max(1600, Number(durationMs) || 3600));
  }, [removeToast]);

  useEffect(() => {
    const onNotify = (event) => {
      notify(event?.detail || {});
    };
    window.addEventListener("inkwell:notify", onNotify);
    return () => {
      window.removeEventListener("inkwell:notify", onNotify);
    };
  }, [notify]);

  const value = useMemo(
    () => ({
      notify,
      success: (message, title) => notify({ type: "success", title: title || "Success", message }),
      error: (message, title) => notify({ type: "error", title: title || "Error", message }),
      info: (message, title) => notify({ type: "info", title: title || "Info", message }),
    }),
    [notify]
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[95] flex w-[min(92vw,360px)] flex-col gap-2">
        <AnimatePresence initial={false}>
          {toasts.map((toast) => {
            const Icon = iconForType(toast.type);
            return (
              <motion.div
                key={toast.id}
                initial={{ opacity: 0, y: -8, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -10, scale: 0.96 }}
                className={`pointer-events-auto rounded-2xl border px-4 py-3 shadow-2xl backdrop-blur ${toneStyles[toast.type] || toneStyles.info}`}
              >
                <div className="flex items-start gap-3">
                  <Icon className="mt-0.5 h-4 w-4 flex-shrink-0" />
                  <div className="min-w-0 flex-1">
                    {toast.title && <p className="text-xs font-semibold uppercase tracking-[0.06em]">{toast.title}</p>}
                    <p className="mt-0.5 text-sm leading-relaxed">{toast.message}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeToast(toast.id)}
                    className="rounded-full p-1 opacity-70 transition hover:opacity-100"
                    aria-label="Dismiss notification"
                  >
                    <X size={14} />
                  </button>
                </div>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </NotificationContext.Provider>
  );
}

export function useNotification() {
  const value = useContext(NotificationContext);
  if (!value) {
    throw new Error("useNotification must be used within NotificationProvider");
  }
  return value;
}
