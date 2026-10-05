import { useEffect, useState } from "react";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { Bell, Menu, Moon, Sun, X } from "lucide-react";
import { useAuth } from "../context/AuthContext.jsx";
import { useTheme } from "../context/ThemeContext.jsx";
import { notificationService } from "../lib/services.js";

function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const { isAuthenticated, role, user, token, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const normalizedRole = role ? String(role).toUpperCase() : "";
  const canAccessAuthor = normalizedRole === "AUTHOR" || normalizedRole === "ADMIN";
  const canAccessAdmin = normalizedRole === "ADMIN";

  const links = [
    { label: "Home", to: "/" },
    { label: "Feed", to: "/feed" },
  ];

  if (isAuthenticated) {
    links.push({ label: "Newsletter", to: "/newsletter" });
    links.push({ label: "Subscriptions", to: "/subscriptions" });
    links.push({ label: "Notifications", to: "/notifications" });
    if (canAccessAuthor) {
      links.push({ label: "Author", to: "/author" });
    }
    if (canAccessAdmin) {
      links.push({ label: "Admin", to: "/admin" });
    }
  }

  useEffect(() => {
    if (!isAuthenticated || !user?.userId) {
      setUnreadCount(0);
      return () => {};
    }

    let active = true;

    const loadUnread = async () => {
      try {
        const response = await notificationService.unreadCount(user.userId, token);
        if (active) {
          setUnreadCount(response?.count || 0);
        }
      } catch {
        if (active) {
          setUnreadCount(0);
        }
      }
    };

    const onNotificationsChanged = (event) => {
      const count = event?.detail?.count;
      if (typeof count === "number") {
        setUnreadCount(Math.max(0, count));
        return;
      }
      loadUnread();
    };

    loadUnread();
    const intervalId = window.setInterval(loadUnread, 15000);
    window.addEventListener("inkwell-notifications-changed", onNotificationsChanged);

    return () => {
      active = false;
      window.clearInterval(intervalId);
      window.removeEventListener("inkwell-notifications-changed", onNotificationsChanged);
    };
  }, [isAuthenticated, location.pathname, token, user?.userId]);

  const renderLinkLabel = (link) => {
    const showUnreadDot = link.to === "/notifications" && unreadCount > 0;
    const showUnreadCount = link.to === "/notifications" && unreadCount > 0;
    return (
      <span className="relative inline-flex items-center">
        {link.to === "/notifications" ? (
          <>
            <Bell size={14} className="mr-1" />
            {link.label}
          </>
        ) : (
          link.label
        )}
        {showUnreadDot && (
          <span
            className="absolute -right-2.5 -top-1.5 h-2.5 w-2.5 rounded-full bg-red-500 shadow-[0_0_0_2px_rgba(0,0,0,0.4)]"
            aria-label={`${unreadCount} unread notifications`}
            title={`${unreadCount} unread notifications`}
          />
        )}
        {showUnreadCount && (
          <span className="ml-1 rounded-full border border-red-500/35 bg-red-500/20 px-1.5 py-0.5 text-[10px] text-red-500 dark:text-red-300">
            {unreadCount}
          </span>
        )}
      </span>
    );
  };

  return (
    <header className="fixed left-0 right-0 top-0 z-50">
      <div className="container-shell mt-4">
        <nav className="glass edge-glow rounded-2xl px-4 py-3 md:px-6">
          <div className="flex items-center justify-between gap-4">
            <Link to="/" className="hero-title text-sm tracking-[0.16em] md:text-base">
              INKWELL
            </Link>

            <div className="hidden items-center gap-2 md:flex">
              {links.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) =>
                    `rounded-full px-3 py-2 text-xs transition ${
                      isActive
                        ? "bg-[var(--surface)] text-[var(--text)] shadow-sm dark:bg-white/12 dark:text-white"
                        : "text-[var(--muted)] hover:text-[var(--text)]"
                    }`
                  }
                >
                  {renderLinkLabel(link)}
                </NavLink>
              ))}
            </div>

            <div className="hidden items-center gap-2 md:flex">
              <button
                type="button"
                onClick={toggleTheme}
                className="icon-action inline-flex items-center rounded-full px-3 py-2 text-xs"
                aria-label="Toggle theme"
              >
                {isDark ? <Sun size={14} className="mr-1.5" /> : <Moon size={14} className="mr-1.5" />}
                {isDark ? "Light" : "Dark"}
              </button>
              {isAuthenticated ? (
                <>
                  <Link
                    to="/profile"
                    className="rounded-full border border-[var(--line)] bg-[var(--surface)]/75 px-3 py-2 text-xs text-[var(--muted)] transition hover:text-[var(--text)] dark:bg-white/5 dark:hover:text-white"
                  >
                    {user?.fullName || user?.username || "User"}
                  </Link>
                  <button
                    onClick={() => {
                      logout();
                      navigate("/");
                    }}
                    className="btn-ghost rounded-full px-4 py-2 text-xs"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" className="btn-ghost rounded-full px-4 py-2 text-xs">
                    Login
                  </Link>
                  <Link to="/signup" className="btn-primary rounded-full px-4 py-2 text-xs">
                    Sign up
                  </Link>
                </>
              )}
            </div>

            <button
              onClick={() => setOpen((v) => !v)}
              className="btn-ghost rounded-full p-2 md:hidden"
              aria-label="Toggle navigation"
            >
              {open ? <X size={18} /> : <Menu size={18} />}
            </button>
          </div>

          {open && (
            <div className="fade-up mt-4 space-y-2 border-t border-[var(--line)] pt-4 md:hidden">
              {links.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  onClick={() => setOpen(false)}
                  className="block rounded-xl px-3 py-2 text-sm text-[var(--muted)] hover:bg-[var(--surface)] hover:text-[var(--text)] dark:hover:bg-white/10 dark:hover:text-white"
                >
                  {renderLinkLabel(link)}
                </NavLink>
              ))}
              <button
                type="button"
                onClick={toggleTheme}
                className="icon-action w-full rounded-xl px-3 py-2 text-sm"
              >
                {isDark ? <Sun size={14} className="mr-2 inline" /> : <Moon size={14} className="mr-2 inline" />}
                Switch to {isDark ? "Light" : "Dark"} Mode
              </button>
              {isAuthenticated ? (
                <>
                  <Link
                    to="/profile"
                    onClick={() => setOpen(false)}
                    className="btn-ghost block w-full rounded-xl px-3 py-2 text-center text-sm"
                  >
                    {user?.fullName || user?.username || "Profile"}
                  </Link>
                  <button
                    onClick={() => {
                      logout();
                      setOpen(false);
                      navigate("/");
                    }}
                    className="btn-ghost w-full rounded-xl px-3 py-2 text-center text-sm"
                  >
                    Logout
                  </button>
                </>
              ) : (
                <div className="grid grid-cols-2 gap-2 pt-2">
                  <Link
                    to="/login"
                    onClick={() => setOpen(false)}
                    className="btn-ghost rounded-xl px-3 py-2 text-center text-sm"
                  >
                    Login
                  </Link>
                  <Link
                    to="/signup"
                    onClick={() => setOpen(false)}
                    className="btn-primary rounded-xl px-3 py-2 text-center text-sm"
                  >
                    Sign up
                  </Link>
                </div>
              )}
            </div>
          )}
        </nav>
      </div>
    </header>
  );
}

export default Navbar;
