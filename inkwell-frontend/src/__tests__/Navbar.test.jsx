import { MemoryRouter } from "react-router-dom";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import Navbar from "../components/Navbar.jsx";
import { notificationService } from "../lib/services.js";

jest.mock("../context/AuthContext.jsx", () => ({
  useAuth: jest.fn(),
}));
jest.mock("../context/ThemeContext.jsx", () => ({
  useTheme: jest.fn(),
}));
jest.mock("../lib/services.js", () => ({
  notificationService: {
    unreadCount: jest.fn(),
  },
}));

import { useAuth } from "../context/AuthContext.jsx";
import { useTheme } from "../context/ThemeContext.jsx";

describe("Navbar", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders authenticated links and unread count", async () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      role: "ADMIN",
      user: { userId: 7, fullName: "Admin User" },
      token: "t",
      logout: jest.fn(),
    });
    useTheme.mockReturnValue({ isDark: false, toggleTheme: jest.fn() });
    notificationService.unreadCount.mockResolvedValue({ count: 3 });

    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );

    expect(screen.getAllByText("Admin").length).toBeGreaterThan(0);
    await waitFor(() => expect(screen.getAllByText("3").length).toBeGreaterThan(0));
  });

  it("renders guest actions and toggles mobile menu", () => {
    useAuth.mockReturnValue({
      isAuthenticated: false,
      role: null,
      user: null,
      token: null,
      logout: jest.fn(),
    });
    useTheme.mockReturnValue({ isDark: true, toggleTheme: jest.fn() });
    notificationService.unreadCount.mockResolvedValue({ count: 0 });

    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );

    expect(screen.getAllByText("Login").length).toBeGreaterThan(0);
    fireEvent.click(screen.getByLabelText("Toggle navigation"));
    expect(screen.getByText("Switch to Light Mode")).toBeInTheDocument();
  });

  it("uses notification event count when provided", async () => {
    useAuth.mockReturnValue({
      isAuthenticated: true,
      role: "AUTHOR",
      user: { userId: 8, username: "writer" },
      token: "t",
      logout: jest.fn(),
    });
    useTheme.mockReturnValue({ isDark: false, toggleTheme: jest.fn() });
    notificationService.unreadCount.mockResolvedValue({ count: 1 });

    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getAllByText("1").length).toBeGreaterThan(0));
    window.dispatchEvent(
      new CustomEvent("inkwell-notifications-changed", { detail: { count: 6 } })
    );
    await waitFor(() => expect(screen.getAllByText("6").length).toBeGreaterThan(0));
  });
});
