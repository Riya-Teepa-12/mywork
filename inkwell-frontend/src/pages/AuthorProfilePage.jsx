import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { authService, postService } from "../lib/services.js";

function AuthorProfilePage() {
  const { authorId } = useParams();
  const [author, setAuthor] = useState(null);
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const numericAuthorId = Number(authorId);
  const validAuthorId = Number.isFinite(numericAuthorId) && numericAuthorId > 0;

  const load = async () => {
    if (!validAuthorId) {
      throw new Error("Invalid author id");
    }
    const [authorRes, postsRes] = await Promise.allSettled([
      authService.getPublicUser(numericAuthorId),
      postService.getPublishedByAuthor(numericAuthorId),
    ]);

    if (authorRes.status !== "fulfilled" || !authorRes.value) {
      throw new Error("Failed to load author profile");
    }

    setAuthor(authorRes.value);
    if (postsRes.status === "fulfilled" && Array.isArray(postsRes.value)) {
      setPosts(postsRes.value);
      return;
    }
    setPosts([]);
  };

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    load()
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err.message || "Failed to load author profile");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [authorId, validAuthorId]);

  if (loading) {
    return (
      <div className="container-shell pb-16">
        <p className="text-sm text-[var(--muted)]">Loading author profile...</p>
      </div>
    );
  }

  if (!author) {
    return (
      <div className="container-shell pb-16">
        <p className="rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error || "Author not found"}
        </p>
      </div>
    );
  }

  return (
    <div className="container-shell pb-16">
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </p>
      )}
      <section className="glass edge-glow rounded-3xl p-6 md:p-8">
        <div>
          <h1 className="hero-title text-3xl">{author.fullName || author.username}</h1>
          <p className="mt-2 text-sm text-[var(--muted)]">@{author.username}</p>
          <p className="mt-2 text-xs text-[var(--primary)] ">Posts Created: {posts.length}</p>
        </div>
        <p className="mt-4 text-sm text-[var(--muted)]">{author.bio || "No bio added yet."}</p>
      </section>

      <section className="mt-8">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="hero-title text-2xl">Published Posts</h2>
          <Link to="/feed" className="btn-ghost rounded-full px-4 py-2 text-xs">
            Back to Feed
          </Link>
        </div>
        <div className="space-y-3">
          {posts.map((post) => (
            <article key={post.postId} className="glass rounded-2xl p-4">
              <h3 className="text-base font-semibold">{post.title}</h3>
              <p className="mt-1 text-sm text-[var(--muted)]">{post.excerpt || "No excerpt"}</p>
              <div className="mt-3 flex items-center gap-3 text-xs text-[var(--muted)]">
                <span>{post.readTimeMin || 1} min read</span>
                <span>{post.likesCount || 0} likes</span>
                <span>{post.viewCount || 0} views</span>
              </div>
              <Link to={`/post/${post.slug}`} className="btn-ghost mt-3 inline-flex rounded-full px-3 py-1 text-xs">
                Read Post
              </Link>
            </article>
          ))}
        </div>
        {posts.length === 0 && (
          <p className="text-sm text-[var(--muted)]">No published posts yet.</p>
        )}
      </section>
    </div>
  );
}

export default AuthorProfilePage;
