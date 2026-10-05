import { MemoryRouter, Route, Routes } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import ProtectedRoute, { PublicOnlyRoute } from "../components/ProtectedRoute.jsx";

jest.mock("../context/AuthContext.jsx", () => ({
  useAuth: jest.fn(),
}));

import { useAuth } from "../context/AuthContext.jsx";

function renderProtected(allowedRoles = []) {
  return render(
    <MemoryRouter initialEntries={["/private"]}>
      <Routes>
        <Route element={<ProtectedRoute allowedRoles={allowedRoles} />}>
          <Route path="/private" element={<div>Private Content</div>} />
        </Route>
        <Route path="/login" element={<div>Login Page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe("ProtectedRoute", () => {
  it("redirects unauthenticated users to login", () => {
    useAuth.mockReturnValue({ isAuthenticated: false, role: null });
    renderProtected();
    expect(screen.getByText("Login Page")).toBeInTheDocument();
  });

  it("allows authenticated users without role restriction", () => {
    useAuth.mockReturnValue({ isAuthenticated: true, role: "reader" });
    renderProtected();
    expect(screen.getByText("Private Content")).toBeInTheDocument();
  });

  it("blocks users with role mismatch", () => {
    useAuth.mockReturnValue({ isAuthenticated: true, role: "READER" });
    renderProtected(["ADMIN"]);
    expect(screen.queryByText("Private Content")).not.toBeInTheDocument();
  });
});

describe("PublicOnlyRoute", () => {
  it("blocks authenticated users", () => {
    useAuth.mockReturnValue({ isAuthenticated: true });
    render(
      <MemoryRouter initialEntries={["/login"]}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<div>Login Public</div>} />
          </Route>
          <Route path="/" element={<div>Home</div>} />
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText("Home")).toBeInTheDocument();
  });

  it("allows unauthenticated users on public routes", () => {
    useAuth.mockReturnValue({ isAuthenticated: false });
    render(
      <MemoryRouter initialEntries={["/login"]}>
        <Routes>
          <Route element={<PublicOnlyRoute />}>
            <Route path="/login" element={<div>Login Public</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );
    expect(screen.getByText("Login Public")).toBeInTheDocument();
  });
});
