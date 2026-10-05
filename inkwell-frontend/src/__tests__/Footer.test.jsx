import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Footer from "../components/Footer.jsx";

describe("Footer", () => {
  it("renders primary navigation links", () => {
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>
    );

    expect(screen.getByRole("link", { name: "Feed" })).toHaveAttribute("href", "/feed");
    expect(screen.getByRole("link", { name: "Login" })).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "Newsletter" })).toHaveAttribute(
      "href",
      "/newsletter"
    );
  });
});
