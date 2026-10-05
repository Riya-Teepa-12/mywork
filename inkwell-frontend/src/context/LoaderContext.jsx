import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { LoaderCircle } from "lucide-react";

const LoaderContext = createContext(null);

export function LoaderProvider({ children }) {
  const [pendingCount, setPendingCount] = useState(0);
  const [visible, setVisible] = useState(false);
  const [label, setLabel] = useState("Please wait, processing...");

  useEffect(() => {
    const showDelay = window.setTimeout(() => {
      setVisible(pendingCount > 0);
    }, 160);

    if (pendingCount === 0) {
      setVisible(false);
    }

    return () => {
      window.clearTimeout(showDelay);
    };
  }, [pendingCount]);

  useEffect(() => {
    const onStart = (event) => {
      if (event?.detail?.trackLoader === false) {
        return;
      }
      setPendingCount((prev) => prev + 1);
      if (event?.detail?.label) {
        setLabel(String(event.detail.label));
      }
    };

    const onEnd = (event) => {
      if (event?.detail?.trackLoader === false) {
        return;
      }
      setPendingCount((prev) => Math.max(0, prev - 1));
    };

    window.addEventListener("inkwell:api:start", onStart);
    window.addEventListener("inkwell:api:end", onEnd);
    return () => {
      window.removeEventListener("inkwell:api:start", onStart);
      window.removeEventListener("inkwell:api:end", onEnd);
    };
  }, []);

  const value = useMemo(
    () => ({
      visible,
      pendingCount,
      isLoading: pendingCount > 0,
    }),
    [pendingCount, visible]
  );

  return (
    <LoaderContext.Provider value={value}>
      {children}
      <AnimatePresence>
        {visible && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="pointer-events-auto fixed inset-0 z-[90] flex items-center justify-center bg-slate-950/45 backdrop-blur-md"
          >
            <motion.div
              initial={{ opacity: 0, y: 10, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.96 }}
              className="glass edge-glow rounded-3xl px-7 py-6 text-center"
            >
              <LoaderCircle className="mx-auto h-8 w-8 animate-spin text-[var(--primary)]" />
              <p className="mt-3 text-sm font-medium text-[var(--text)]">{label}</p>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </LoaderContext.Provider>
  );
}

export function useLoader() {
  const value = useContext(LoaderContext);
  if (!value) {
    throw new Error("useLoader must be used within LoaderProvider");
  }
  return value;
}
