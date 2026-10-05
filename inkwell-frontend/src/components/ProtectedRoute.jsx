import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext.jsx";

function normalizeRole(role) {
  if (!role) {
    return "";
  }
  return String(role).toUpperCase();
}

function ProtectedRoute({ allowedRoles = [] }) {
  const { isAuthenticated, role } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles.length === 0) {
    return <Outlet />;
  }

  const allowed = allowedRoles.map(normalizeRole);
  const currentRole = normalizeRole(role);

  if (!allowed.includes(currentRole)) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

export function PublicOnlyRoute() {
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

export default ProtectedRoute;
