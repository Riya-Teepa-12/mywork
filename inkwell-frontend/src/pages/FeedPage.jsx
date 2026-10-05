import { startTransition, useDeferredValue, useEffect, useMemo, useState } from "react";
import { Filter, Search, X } from "lucide-react";
import { motion } from "framer-motion";
import PostCard from "../components/PostCard.jsx";
import { authService, commentService, postService, taxonomyService } from "../lib/services.js";
import { useAuth } from "../context/AuthContext.jsx";

function FeedPage() {
  const { token } = useAuth();
  const [query, setQuery] = useState("");
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [selectedTags, setSelectedTags] = useState([]);
  const [showFilters, setShowFilters] = useState(false);
  const [posts, setPosts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const deferredQuery = useDeferredValue(query);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");

    Promise.all([
      deferredQuery.trim()
        ? postService.search(deferredQuery.trim())
        : postService.getPublished(),
      taxonomyService.getCategories(),
    ])
      .then(async ([postRows, categoryRows]) => {
        if (!active) {
          return;
        }
        const normalizedCategories = Array.isArray(categoryRows) ? categoryRows : [];
        setCategories(normalizedCategories);

        const postList = Array.isArray(postRows) ? postRows : [];
        const authorIds = Array.from(
          new Set(
            postList
              .map((post) => Number(post.authorId))
              .filter((id) => Number.isFinite(id) && id > 0)
          )
        );
        const authorResults = await Promise.allSettled(
          authorIds.map((authorId) => authService.getPublicUser(authorId))
        );
        const authorMap = {};
        authorIds.forEach((authorId, index) => {
          const result = authorResults[index];
          if (result?.status === "fulfilled" && result.value) {
            authorMap[authorId] = result.value;
          }
        });

        const enriched = await Promise.all(
          postList.map(async (post) => {
            const [tagsRes, catRes, commentRes] = await Promise.allSettled([
              taxonomyService.getTagsByPost(post.postId),
              taxonomyService.getCategoriesByPost(post.postId),
              commentService.count(post.postId, token),
            ]);
            const tagNames =
              tagsRes.status === "fulfilled" && Array.isArray(tagsRes.value)
                ? tagsRes.value.map((item) => item.name)
                : [];
            const categoryNames =
              catRes.status === "fulfilled" && Array.isArray(catRes.value)
                ? catRes.value.map((item) => item.name)
                : [];
            const categoryName = categoryNames[0] || "General";
            const commentCount =
              commentRes.status === "fulfilled" ? commentRes.value.count || 0 : 0;
            const authorId = Number(post.authorId);
            const authorProfile =
              Number.isFinite(authorId) && authorId > 0 ? authorMap[authorId] : null;
            return {
              ...post,
              tags: tagNames,
              categories: categoryNames,
              category: categoryName,
              commentCount,
              authorName:
                post.authorName ||
                post.author ||
                authorProfile?.fullName ||
                authorProfile?.username ||
                (Number.isFinite(authorId) && authorId > 0 ? `User #${authorId}` : "Unknown author"),
              authorAvatarUrl: post.authorAvatarUrl || authorProfile?.avatarUrl || "",
            };
          })
        );
        if (!active) {
          return;
        }
        setPosts(enriched);
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err.message || "Failed to load feed");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [deferredQuery, token]);

  const categoryOptions = useMemo(
    () => Array.from(new Set(categories.map((item) => item.name).filter(Boolean))),
    [categories]
  );

  const tagOptions = useMemo(() => {
    const tags = new Set();
    posts.forEach((post) => {
      (post.tags || []).forEach((item) => tags.add(item));
    });
    return Array.from(tags);
  }, [posts]);

  const filteredPosts = useMemo(() => {
    return posts.filter((post) => {
      const postCategories = Array.isArray(post.categories) && post.categories.length > 0
        ? post.categories
        : [post.category].filter(Boolean);
      const categoryMatch =
        selectedCategories.length === 0 ||
        selectedCategories.some((value) => postCategories.includes(value));
      const tagMatch =
        selectedTags.length === 0 ||
        selectedTags.some((value) => (post.tags || []).includes(value));
      return categoryMatch && tagMatch;
    });
  }, [selectedCategories, selectedTags, posts]);

  const selectedFiltersCount = selectedCategories.length + selectedTags.length;

  const toggleSelection = (setItems, value) => {
    setItems((prev) =>
      prev.includes(value) ? prev.filter((item) => item !== value) : [...prev, value]
    );
  };

  return (
    <div className="container-shell pb-16">
      <header className="mb-8">
        <h1 className="hero-title text-3xl">Public Feed</h1>
        <p className="mt-2 text-sm text-[var(--muted)]">
          Discover published stories, filter by category, and search by keywords.
        </p>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        className="glass mb-8 rounded-3xl p-4 md:p-5"
      >
        <div className="grid gap-4 md:grid-cols-[1fr_auto] md:items-center">
          <label className="relative block">
            <Search
              size={16}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--muted)]"
            />
            <input
              value={query}
              onChange={(event) =>
                startTransition(() => setQuery(event.target.value))
              }
              placeholder="Search by title or excerpt"
              className="w-full rounded-2xl border py-3 pl-10 pr-4 text-sm outline-none focus:border-[var(--line-strong)]"
            />
          </label>
          <div className="flex flex-wrap items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowFilters(true)}
              className="btn-ghost inline-flex items-center rounded-full px-4 py-2 text-xs"
            >
              <Filter size={14} className="mr-2" />
              Filters
              {selectedFiltersCount > 0 && (
                <span className="ml-2 rounded-full bg-[var(--primary)]/15 px-2 py-0.5 text-[10px] text-[var(--primary)]">
                  {selectedFiltersCount}
                </span>
              )}
            </button>
            {selectedFiltersCount > 0 && (
              <button
                type="button"
                onClick={() => {
                  setSelectedCategories([]);
                  setSelectedTags([]);
                }}
                className="btn-ghost inline-flex items-center rounded-full px-4 py-2 text-xs"
              >
                <X size={14} className="mr-1" />
                Clear
              </button>
            )}
          </div>
        </div>
      </motion.div>

      {loading && (
        <p className="mb-4 text-sm text-[var(--muted)]">Loading feed...</p>
      )}
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
          {error}
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {filteredPosts.map((post) => (
          <PostCard key={post.postId} post={post} />
        ))}
      </div>
      {!loading && filteredPosts.length === 0 && (
        <p className="mt-6 text-sm text-[var(--muted)]">No posts found.</p>
      )}

      {showFilters && (
        <div
          className="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm"
          onClick={() => setShowFilters(false)}
        >
          <motion.div
            initial={{ opacity: 0, y: 14, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 14, scale: 0.98 }}
            className="glass edge-glow w-full max-w-2xl rounded-3xl p-5 md:p-6"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-lg font-semibold">Filter Posts</h2>
              <button
                type="button"
                onClick={() => setShowFilters(false)}
                className="icon-action rounded-full p-2"
                aria-label="Close filters"
              >
                <X size={14} />
              </button>
            </div>

            <div className="mt-5 grid gap-5 md:grid-cols-2">
              <section>
                <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[var(--muted)]">
                  Categories
                </p>
                <div className="mt-3 max-h-56 space-y-2 overflow-auto pr-1">
                  {categoryOptions.length === 0 && (
                    <p className="text-xs text-[var(--muted)]">No categories available.</p>
                  )}
                  {categoryOptions.map((item) => (
                    <label
                      key={item}
                      className="flex cursor-pointer items-center justify-between rounded-xl border border-[var(--line)] bg-[var(--surface-2)]/70 px-3 py-2"
                    >
                      <span className="text-sm">{item}</span>
                      <input
                        type="checkbox"
                        checked={selectedCategories.includes(item)}
                        onChange={() => toggleSelection(setSelectedCategories, item)}
                        className="h-4 w-4 accent-blue-500"
                      />
                    </label>
                  ))}
                </div>
              </section>

              <section>
                <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[var(--muted)]">
                  Tags
                </p>
                <div className="mt-3 max-h-56 space-y-2 overflow-auto pr-1">
                  {tagOptions.length === 0 && (
                    <p className="text-xs text-[var(--muted)]">No tags available.</p>
                  )}
                  {tagOptions.map((item) => (
                    <label
                      key={item}
                      className="flex cursor-pointer items-center justify-between rounded-xl border border-[var(--line)] bg-[var(--surface-2)]/70 px-3 py-2"
                    >
                      <span className="text-sm">#{item}</span>
                      <input
                        type="checkbox"
                        checked={selectedTags.includes(item)}
                        onChange={() => toggleSelection(setSelectedTags, item)}
                        className="h-4 w-4 accent-blue-500"
                      />
                    </label>
                  ))}
                </div>
              </section>
            </div>

            <div className="mt-5 flex items-center justify-between gap-2">
              <p className="text-xs text-[var(--muted)]">
                Filters are applied instantly to the feed.
              </p>
              <button
                type="button"
                onClick={() => setShowFilters(false)}
                className="btn-primary rounded-full px-4 py-2 text-xs"
              >
                Done
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}

export default FeedPage;
