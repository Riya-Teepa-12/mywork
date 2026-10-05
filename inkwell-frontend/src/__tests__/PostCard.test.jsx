import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import PostCard from "../components/PostCard.jsx";

describe("PostCard", () => {
  it("renders post details and links", () => {
    render(
      <MemoryRouter>
        <PostCard
          post={{
            slug: "hello-world",
            title: "Hello",
            excerpt: "Excerpt",
            category: "Tech",
            tags: ["react"],
            authorId: 11,
            authorName: "Jane Doe",
            likesCount: 4,
            commentCount: 2,
          }}
        />
      </MemoryRouter>
    );

    expect(screen.getByText("Hello")).toBeInTheDocument();
    expect(screen.getByText("#react")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Read Article" })).toHaveAttribute(
      "href",
      "/post/hello-world"
    );
  });

  it("renders fallback author and initials for missing profile", () => {
    render(
      <MemoryRouter>
        <PostCard
          post={{
            slug: "untitled",
            title: "No Profile",
            excerpt: "E",
            authorId: 0,
            author: "single",
            featured: true,
            tags: [],
          }}
        />
      </MemoryRouter>
    );

    expect(screen.getByText("Featured")).toBeInTheDocument();
    expect(screen.getByText("S")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "single" })).not.toBeInTheDocument();
  });
});
