import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { AuthProvider, useAuth } from "../context/AuthContext.jsx";
import { apiRequest } from "../lib/api.js";

jest.mock("../lib/api.js", () => ({
  apiRequest: jest.fn(),
}));

function Probe() {
  const { isAuthenticated, isSuspended, login, logout, loginWithToken, refreshProfile, user } = useAuth();
  return (
    <div>
      <span data-testid="auth">{String(isAuthenticated)}</span>
      <span data-testid="suspended">{String(isSuspended)}</span>
      <span data-testid="user">{user?.fullName || ""}</span>
      <button onClick={() => login({ email: "a@b.com", password: "x" })}>login</button>
      <button onClick={() => loginWithToken("token-2")}>loginWithToken</button>
      <button onClick={() => refreshProfile()}>refresh</button>
      <button onClick={logout}>logout</button>
    </div>
  );
}

describe("AuthContext", () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  it("logs in and logs out", async () => {
    apiRequest
      .mockResolvedValueOnce({ accessToken: "t1", user: { userId: 1, fullName: "Test User", role: "READER" } })
      .mockResolvedValueOnce({ userId: 1, fullName: "Test User", role: "READER" })
      .mockResolvedValueOnce({ ok: true });

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    );

    fireEvent.click(screen.getByText("login"));
    await waitFor(() => expect(screen.getByTestId("auth")).toHaveTextContent("true"));
    expect(screen.getByTestId("user")).toHaveTextContent("Test User");

    fireEvent.click(screen.getByText("logout"));
    await waitFor(() => expect(screen.getByTestId("auth")).toHaveTextContent("false"));
  });

  it("loads stored auth and marks suspended users", async () => {
    localStorage.setItem(
      "inkwell_auth",
      JSON.stringify({ token: "ts", user: { userId: 9, fullName: "Suspended", status: "SUSPENDED" } })
    );
    apiRequest.mockResolvedValueOnce({ userId: 9, fullName: "Suspended", status: "SUSPENDED" });

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    );

    await waitFor(() => expect(screen.getByTestId("auth")).toHaveTextContent("true"));
    expect(screen.getByTestId("suspended")).toHaveTextContent("true");
  });

  it("supports loginWithToken and refreshProfile", async () => {
    apiRequest
      .mockResolvedValueOnce({ userId: 5, fullName: "Token User", role: "AUTHOR", active: true })
      .mockResolvedValueOnce({ userId: 5, fullName: "Token User", role: "AUTHOR", active: true })
      .mockResolvedValueOnce({ userId: 5, fullName: "Updated User", role: "AUTHOR", active: true });

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    );

    fireEvent.click(screen.getByText("loginWithToken"));
    await waitFor(() => expect(screen.getByTestId("auth")).toHaveTextContent("true"));
    expect(screen.getByTestId("user")).toHaveTextContent("Token User");

    fireEvent.click(screen.getByText("refresh"));
    await waitFor(() => expect(screen.getByTestId("user")).toHaveTextContent("Updated User"));
  });
});
