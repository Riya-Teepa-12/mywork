import { render, screen } from "@testing-library/react";
import AnimatedSection from "../components/AnimatedSection.jsx";

describe("AnimatedSection", () => {
  it("renders children", () => {
    render(
      <AnimatedSection>
        <div>Child Content</div>
      </AnimatedSection>
    );
    expect(screen.getByText("Child Content")).toBeInTheDocument();
  });
});
