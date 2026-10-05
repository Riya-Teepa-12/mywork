import {
  createContext,
  useContext,
  useEffect,
  useState,
  startTransition,
} from "react";
import { apiRequest } from "../lib/api.js";

const AuthContext = createContext(null);
const AUTH_STORAGE_KEY = "inkwell_auth";

function isSuspendedUser(user) {
  if (!user) {
    return false;
  }
  if (typeof user.active === "boolean" && user.active === false) {
    return true;
  }
  const status = String(user.status || user.accountStatus || "").toUpperCase();
  return status === "SUSPENDED";
}

function readStoredAuth() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
      return { token: null, user: null };
    }
    const data = JSON.parse(raw);
    return { token: data.token ?? null, user: data.user ?? null };
  } catch {
    return { token: null, user: null };
  }
}

export function AuthProvider({ children }) {
  const [{ token, user }, setAuth] = useState(readStoredAuth);

  useEffect(() => {
    if (!token || !user) {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      return;
    }
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token, user }));
  }, [token, user]);

  useEffect(() => {
    let active = true;
    if (!token) {
      return () => {
        active = false;
      };
    }

    apiRequest("/auth/profile", { token })
      .then((profile) => {
        if (!active) {
          return;
        }
        startTransition(() => {
          setAuth((prev) => ({ ...prev, user: profile }));
        });
      })
      .catch(() => {
        if (!active) {
          return;
        }
        startTransition(() => {
          setAuth({ token: null, user: null });
        });
      });

    return () => {
      active = false;
    };
  }, [token]);

  const login = async (credentials) => {
    const result = await apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
      loaderLabel: "Logging in...",
    });
    startTransition(() => {
      setAuth({ token: result.accessToken, user: result.user });
    });
    return result;
  };

  const signup = async (payload) => {
    return apiRequest("/auth/register", {
      method: "POST",
      body: JSON.stringify(payload),
      loaderLabel: "Creating account...",
    });
  };

  const loginWithToken = async (accessToken) => {
    if (!accessToken) {
      throw new Error("Token is required");
    }
    const profile = await apiRequest("/auth/profile", { token: accessToken });
    startTransition(() => {
      setAuth({ token: accessToken, user: profile });
    });
    return { accessToken, user: profile };
  };

  const logout = () => {
    if (token) {
      apiRequest("/auth/logout", { method: "POST", token, trackLoader: false }).catch(() => null);
    }
    startTransition(() => {
      setAuth({ token: null, user: null });
    });
  };

  const refreshProfile = async () => {
    if (!token) {
      return null;
    }
    const profile = await apiRequest("/auth/profile", { token });
    startTransition(() => {
      setAuth((prev) => ({ ...prev, user: profile }));
    });
    return profile;
  };

  const value = {
    token,
    user,
    role: user?.role || null,
    isAuthenticated: Boolean(token && user),
    isSuspended: isSuspendedUser(user),
    login,
    loginWithToken,
    signup,
    logout,
    refreshProfile,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return value;
}
