import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import App from "../App.jsx";

jest.mock("../context/AuthContext.jsx", () => ({
  useAuth: jest.fn(),
}));

jest.mock("../components/Navbar.jsx", () => () => <div>Navbar</div>);
jest.mock("../components/Footer.jsx", () => () => <div>Footer</div>);

jest.mock("../pages/HomePage.jsx", () => () => <div>HomePage</div>);
jest.mock("../pages/FeedPage.jsx", () => () => <div>FeedPage</div>);
jest.mock("../pages/PostPage.jsx", () => () => <div>PostPage</div>);
jest.mock("../pages/LoginPage.jsx", () => () => <div>LoginPage</div>);
jest.mock("../pages/SignupPage.jsx", () => () => <div>SignupPage</div>);
jest.mock("../pages/ForgotPasswordPage.jsx", () => () => <div>ForgotPasswordPage</div>);
jest.mock("../pages/OAuthCallbackPage.jsx", () => () => <div>OAuthCallbackPage</div>);
jest.mock("../pages/AuthorDashboardPage.jsx", () => () => <div>AuthorDashboardPage</div>);
jest.mock("../pages/AuthorProfilePage.jsx", () => () => <div>AuthorProfilePage</div>);
jest.mock("../pages/AdminPanelPage.jsx", () => () => <div>AdminPanelPage</div>);
jest.mock("../pages/ProfilePage.jsx", () => () => <div>ProfilePage</div>);
jest.mock("../pages/NotificationsPage.jsx", () => () => <div>NotificationsPage</div>);
jest.mock("../pages/NewsletterPage.jsx", () => () => <div>NewsletterPage</div>);
jest.mock("../pages/SubscriptionsPage.jsx", () => () => <div>SubscriptionsPage</div>);
jest.mock("../pages/NotFoundPage.jsx", () => () => <div>NotFoundPage</div>);
jest.mock("../pages/SuspendedAccountPage.jsx", () => () => <div>SuspendedAccountPage</div>);

import { useAuth } from "../context/AuthContext.jsx";

describe("App", () => {
  it("renders home route for normal users", () => {
    useAuth.mockReturnValue({ isAuthenticated: false, isSuspended: false });
    render(
      <MemoryRouter initialEntries={["/"]}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText("HomePage")).toBeInTheDocument();
    expect(screen.getByText("Navbar")).toBeInTheDocument();
    expect(screen.getByText("Footer")).toBeInTheDocument();
  });

  it("renders suspended account screen when suspended", () => {
    useAuth.mockReturnValue({ isAuthenticated: true, isSuspended: true });
    render(
      <MemoryRouter initialEntries={["/feed"]}>
        <App />
      </MemoryRouter>
    );
    expect(screen.getByText("SuspendedAccountPage")).toBeInTheDocument();
  });
});
