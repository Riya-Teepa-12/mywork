import { useEffect, useState } from "react";
import { notificationService } from "../lib/services.js";
import { useAuth } from "../context/AuthContext.jsx";

function formatDate(value) {
  if (!value) {
    return "";
  }
  return new Date(value).toLocaleString();
}

function NotificationsPage() {
  const { token, user } = useAuth();
  const [items, setItems] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const publishUnreadCount = (count) => {
    window.dispatchEvent(
      new CustomEvent("inkwell-notifications-changed", {
        detail: { count: Math.max(0, Number(count) || 0) },
      })
    );
  };

  const load = async () => {
    if (!user?.userId) {
      return;
    }
    const [rows, unread] = await Promise.all([
      notificationService.getByRecipient(user.userId, token),
      notificationService.unreadCount(user.userId, token),
    ]);
    const sorted = Array.isArray(rows)
      ? [...rows].sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
      : [];
    const nextUnreadCount = unread?.count || 0;
    setItems(sorted);
    setUnreadCount(nextUnreadCount);
    publishUnreadCount(nextUnreadCount);
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
        setError(err.message || "Failed to load notifications");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [token, user?.userId]);

  const markAllRead = async () => {
    if (!user?.userId) {
      return;
    }
    setError("");
    try {
      await notificationService.markAllRead(user.userId, token);
      await load();
    } catch (err) {
      setError(err.message || "Failed to mark all read");
    }
  };

  const deleteRead = async () => {
    if (!user?.userId) {
      return;
    }
    setError("");
    try {
      await notificationService.deleteRead(user.userId, token);
      await load();
    } catch (err) {
      setError(err.message || "Failed to delete read notifications");
    }
  };

  const markOneRead = async (notificationId) => {
    setError("");
    try {
      await notificationService.markRead(notificationId, token);
      await load();
    } catch (err) {
      setError(err.message || "Failed to mark notification read");
    }
  };

  const deleteOne = async (notificationId) => {
    setError("");
    try {
      await notificationService.delete(notificationId, token);
      await load();
    } catch (err) {
      setError(err.message || "Failed to delete notification");
    }
  };

  return (
    <div className="container-shell pb-16">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="hero-title text-3xl">Notification Center</h1>
          <p className="mt-2 text-sm text-[var(--muted)]">
            Track replies, mentions, and platform alerts.
          </p>
          <p className="mt-2 text-xs text-[var(--primary)] ">Unread: {unreadCount}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button onClick={markAllRead} className="btn-primary rounded-full px-4 py-2 text-xs">
            Mark all read
          </button>
          <button onClick={deleteRead} className="btn-ghost rounded-full px-4 py-2 text-xs">
            Delete read
          </button>
        </div>
      </header>

      {loading && <p className="mb-3 text-sm text-[var(--muted)]">Loading notifications...</p>}
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </p>
      )}

      <div className="space-y-3">
        {items.map((item) => (
          <article
            key={item.notificationId}
            className={`glass rounded-2xl p-4 ${item.read ? "opacity-65" : "edge-glow"}`}
          >
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-semibold">{item.title}</p>
              <p className="text-xs text-[var(--muted)]">{item.type}</p>
            </div>
            <p className="mt-2 text-sm text-[var(--muted)]">{item.message}</p>
            <p className="mt-2 text-xs text-[var(--muted)]">{formatDate(item.createdAt)}</p>
            <div className="mt-3 flex gap-2">
              {!item.read && (
                <button
                  onClick={() => markOneRead(item.notificationId)}
                  className="btn-ghost rounded-full px-3 py-1 text-xs"
                >
                  Mark read
                </button>
              )}
              <button
                onClick={() => deleteOne(item.notificationId)}
                className="rounded-full border border-red-500/70 bg-red-500/20 px-3 py-1 text-xs font-semibold text-red-700 dark:text-red-200"
              >
                Delete
              </button>
            </div>
          </article>
        ))}
      </div>
      {!loading && items.length === 0 && (
        <p className="text-sm text-[var(--muted)]">No notifications found.</p>
      )}
    </div>
  );
}

export default NotificationsPage;
