import { useEffect, useMemo, useRef, useState } from "react";
import {
  FileText,
  Eye,
  Heart,
  MessageSquare,
  Upload,
  Rocket,
  X,
  Check,
  Trash2,
  Paperclip,
} from "lucide-react";
import { useAuth } from "../context/AuthContext.jsx";
import {
  commentService,
  mediaService,
  postService,
  taxonomyService,
} from "../lib/services.js";
import { useNotification } from "../context/NotificationContext.jsx";
import RichTextEditor from "../components/RichTextEditor.jsx";

const defaultPostForm = {
  title: "",
  content: "",
  excerpt: "",
  featuredImageUrl: "",
  status: "DRAFT",
};

function formatDate(value) {
  if (!value) {
    return "";
  }
  return new Date(value).toLocaleString();
}

function AuthorDashboardPage() {
  const { token, user } = useAuth();
  const notify = useNotification();
  const [posts, setPosts] = useState([]);
  const [media, setMedia] = useState([]);
  const [pendingCommentsCount, setPendingCommentsCount] = useState(0);
  const [categories, setCategories] = useState([]);
  const [tags, setTags] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [postForm, setPostForm] = useState(defaultPostForm);
  const [editingPostId, setEditingPostId] = useState(null);
  const [mediaFile, setMediaFile] = useState(null);
  const [mediaAltText, setMediaAltText] = useState("");
  const [selectedMediaId, setSelectedMediaId] = useState("");
  const [selectedPostId, setSelectedPostId] = useState("");
  const [selectedCategoryId, setSelectedCategoryId] = useState("");
  const [selectedTagId, setSelectedTagId] = useState("");
  const [newCategoryName, setNewCategoryName] = useState("");
  const [newTagName, setNewTagName] = useState("");
  const [selectedPostDetails, setSelectedPostDetails] = useState(null);
  const [postComments, setPostComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentActionLoadingId, setCommentActionLoadingId] = useState(null);
  const [postSubmitting, setPostSubmitting] = useState(false);
  const [uploadingMedia, setUploadingMedia] = useState(false);
  const [attachingMedia, setAttachingMedia] = useState(false);
  const [deletingMediaId, setDeletingMediaId] = useState(null);
  const [localMediaPreviewUrl, setLocalMediaPreviewUrl] = useState("");
  const postFormRef = useRef(null);
  const editorRef = useRef(null);

  const load = async () => {
    if (!user?.userId) {
      return;
    }
    const [postRows, mediaRows, categoryRows, tagRows] = await Promise.all([
      postService.getByAuthor(user.userId, token),
      mediaService.getByUploader(user.userId, token),
      taxonomyService.getCategories(),
      taxonomyService.getTags(),
    ]);
    const ownedPosts = Array.isArray(postRows) ? postRows : [];
    setPosts(ownedPosts);
    setMedia(Array.isArray(mediaRows) ? mediaRows : []);
    setCategories(Array.isArray(categoryRows) ? categoryRows : []);
    setTags(Array.isArray(tagRows) ? tagRows : []);

    const commentResults = await Promise.allSettled(
      ownedPosts.map((post) => commentService.getByPost(post.postId, token))
    );
    let pending = 0;
    commentResults.forEach((result) => {
      if (result.status !== "fulfilled" || !Array.isArray(result.value)) {
        return;
      }
      pending += result.value.filter((comment) => comment.status === "PENDING").length;
    });
    setPendingCommentsCount(pending);
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
        setError(err.message || "Failed to load author dashboard");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [token, user?.userId]);

  const postTitleById = useMemo(() => {
    const map = {};
    posts.forEach((post) => {
      map[post.postId] = post.title;
    });
    return map;
  }, [posts]);

  const selectedMedia = useMemo(
    () => media.find((item) => String(item.mediaId) === String(selectedMediaId)) || null,
    [media, selectedMediaId]
  );

  useEffect(() => {
    if (!mediaFile) {
      setLocalMediaPreviewUrl("");
      return;
    }
    const objectUrl = URL.createObjectURL(mediaFile);
    setLocalMediaPreviewUrl(objectUrl);
    return () => {
      URL.revokeObjectURL(objectUrl);
    };
  }, [mediaFile]);

  const stats = useMemo(() => {
    const postCount = posts.length;
    const viewCount = posts.reduce((sum, item) => sum + (item.viewCount || 0), 0);
    const likesCount = posts.reduce((sum, item) => sum + (item.likesCount || 0), 0);
    return [
      { label: "My Posts", value: String(postCount), icon: FileText },
      { label: "Views", value: String(viewCount), icon: Eye },
      { label: "Likes", value: String(likesCount), icon: Heart },
      { label: "Pending Comments", value: String(pendingCommentsCount), icon: MessageSquare },
    ];
  }, [pendingCommentsCount, posts]);

  const handlePostFormChange = (event) => {
    const { name, value } = event.target;
    setPostForm((prev) => ({ ...prev, [name]: value }));
  };

  const resetPostForm = () => {
    setPostForm(defaultPostForm);
    setEditingPostId(null);
    setSelectedPostId("");
  };

  const submitPost = async () => {
    if (!user?.userId || postSubmitting) {
      return;
    }
    setPostSubmitting(true);
    setError("");
    try {
      const payload = {
        ...postForm,
        authorId: user.userId,
        authorName: user.fullName || user.username || `User #${user.userId}`,
      };
      const savedPost = editingPostId
        ? await postService.update(editingPostId, payload, token)
        : await postService.create(payload, token);

      if (selectedCategoryId) {
        await taxonomyService.addCategoryToPost(savedPost.postId, Number(selectedCategoryId), token);
      }
      if (selectedTagId) {
        await taxonomyService.addTagToPost(savedPost.postId, Number(selectedTagId), token);
      }

      if (selectedMediaId) {
        try {
          await mediaService.linkToPost(Number(selectedMediaId), savedPost.postId, token);
        } catch (linkErr) {
          notify.info(linkErr?.message || "Post saved, but selected media could not be linked");
        }
      }

      notify.success(editingPostId ? "Post updated successfully" : "Post created successfully");
      resetPostForm();
      setSelectedPostId("");
      await load();
    } catch (err) {
      const message = err.message || "Failed to save post";
      setError(message);
      notify.error(message);
    } finally {
      setPostSubmitting(false);
    }
  };

  const startEdit = (post) => {
    setEditingPostId(post.postId);
    setSelectedPostId(String(post.postId));
    setPostForm({
      title: post.title || "",
      content: post.content || "",
      excerpt: post.excerpt || "",
      featuredImageUrl: post.featuredImageUrl || "",
      status: post.status || "DRAFT",
    });
    requestAnimationFrame(() => {
      postFormRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
    });
  };

  const publishPost = async (postId) => {
    setError("");
    try {
      await postService.publish(postId, token);
      await load();
    } catch (err) {
      setError(err.message || "Failed to publish post");
    }
  };

  const unpublishPost = async (postId) => {
    setError("");
    try {
      await postService.unpublish(postId, token);
      await load();
    } catch (err) {
      setError(err.message || "Failed to unpublish post");
    }
  };

  const deletePost = async (postId) => {
    setError("");
    try {
      await postService.delete(postId, token);
      if (selectedPostDetails?.postId === postId) {
        setSelectedPostDetails(null);
        setPostComments([]);
      }
      await load();
    } catch (err) {
      setError(err.message || "Failed to delete post");
    }
  };

  const uploadMedia = async () => {
    if (!mediaFile || !user?.userId || uploadingMedia) {
      if (!mediaFile) {
        notify.info("Choose a file before uploading");
      }
      return;
    }
    setUploadingMedia(true);
    setError("");
    try {
      const uploaded = await mediaService.upload(mediaFile, user.userId, mediaAltText, token);
      setMediaFile(null);
      setMediaAltText("");
      if (uploaded?.mediaId) {
        setSelectedMediaId(String(uploaded.mediaId));
      }
      notify.success("Media uploaded successfully");
      await load();
    } catch (err) {
      const message = err.message || "Failed to upload media";
      setError(message);
      notify.error(message);
    } finally {
      setUploadingMedia(false);
    }
  };

  const linkMedia = async () => {
    if (!selectedMedia) {
      notify.info("Select uploaded media first");
      return;
    }
    if (attachingMedia) {
      return;
    }

    setAttachingMedia(true);
    setError("");
    try {
      const targetPostId = selectedPostId ? Number(selectedPostId) : editingPostId ? Number(editingPostId) : null;
      if (targetPostId) {
        await mediaService.linkToPost(Number(selectedMediaId), targetPostId, token);
      }
      const mediaUrl = String(selectedMedia.url || "").trim();
      if (mediaUrl) {
        const isImage = String(selectedMedia.mimeType || "").startsWith("image/");
        if (isImage) {
          editorRef.current?.insertImage(mediaUrl, selectedMedia.altText || selectedMedia.originalName || "image");
          if (!postForm.featuredImageUrl) {
            setPostForm((prev) => ({ ...prev, featuredImageUrl: mediaUrl }));
          }
        } else {
          editorRef.current?.insertLink(mediaUrl, selectedMedia.originalName || "Open media");
        }
      }
      await load();
      notify.success(targetPostId ? "Media attached and linked to post" : "Media attached to editor");
    } catch (err) {
      const message = err.message || "Failed to link media";
      setError(message);
      notify.error(message);
    } finally {
      setAttachingMedia(false);
    }
  };

  const deleteMedia = async (mediaId) => {
    if (deletingMediaId === mediaId) {
      return;
    }
    setDeletingMediaId(mediaId);
    setError("");
    try {
      await mediaService.delete(mediaId, token);
      await load();
      notify.success("Media deleted");
    } catch (err) {
      const message = err.message || "Failed to delete media";
      setError(message);
      notify.error(message);
    } finally {
      setDeletingMediaId(null);
    }
  };

  const createCategory = async () => {
    if (!newCategoryName.trim()) {
      notify.info("Category name is required");
      return;
    }
    setError("");
    try {
      await taxonomyService.createCategory(
        { name: newCategoryName.trim(), description: null, parentCategoryId: null },
        token
      );
      setNewCategoryName("");
      await load();
      notify.success("Category created");
    } catch (err) {
      const message = err.message || "Failed to create category";
      setError(message);
      notify.error(message);
    }
  };

  const createTag = async () => {
    if (!newTagName.trim()) {
      notify.info("Tag name is required");
      return;
    }
    setError("");
    try {
      await taxonomyService.createTag({ name: newTagName.trim() }, token);
      setNewTagName("");
      await load();
      notify.success("Tag created");
    } catch (err) {
      const message = err.message || "Failed to create tag";
      setError(message);
      notify.error(message);
    }
  };

  const loadPostComments = async (postId) => {
    const rows = await commentService.getByPost(postId, token);
    const list = Array.isArray(rows) ? rows : [];
    list.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0));
    setPostComments(list);
  };

  const openPostDetails = async (post) => {
    if (!post?.postId) {
      return;
    }
    setSelectedPostDetails(post);
    setCommentsLoading(true);
    setError("");
    try {
      await loadPostComments(post.postId);
    } catch (err) {
      setError(err.message || "Failed to load post comments");
      setPostComments([]);
    } finally {
      setCommentsLoading(false);
    }
  };

  const closePostDetails = () => {
    setSelectedPostDetails(null);
    setPostComments([]);
    setCommentActionLoadingId(null);
  };

  const approveComment = async (commentId) => {
    if (!selectedPostDetails) {
      return;
    }
    setError("");
    setCommentActionLoadingId(commentId);
    try {
      await commentService.approve(commentId, token);
      await loadPostComments(selectedPostDetails.postId);
      await load();
    } catch (err) {
      setError(err.message || "Failed to approve comment");
    } finally {
      setCommentActionLoadingId(null);
    }
  };

  const rejectComment = async (commentId) => {
    if (!selectedPostDetails) {
      return;
    }
    setError("");
    setCommentActionLoadingId(commentId);
    try {
      await commentService.reject(commentId, token);
      await loadPostComments(selectedPostDetails.postId);
      await load();
    } catch (err) {
      setError(err.message || "Failed to reject comment");
    } finally {
      setCommentActionLoadingId(null);
    }
  };

  const deleteComment = async (commentId) => {
    if (!selectedPostDetails || !user?.userId) {
      return;
    }
    setError("");
    setCommentActionLoadingId(commentId);
    try {
      await commentService.delete(commentId, user.userId, token);
      await loadPostComments(selectedPostDetails.postId);
      await load();
    } catch (err) {
      setError(err.message || "Failed to delete comment");
    } finally {
      setCommentActionLoadingId(null);
    }
  };

  return (
    <div className="container-shell pb-16">
      <header className="mb-8 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="hero-title text-3xl">Author Studio</h1>
          <p className="mt-2 text-sm text-[var(--muted)]">
            Create posts, manage media, and moderate comments inside each published post.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={uploadMedia}
            disabled={uploadingMedia}
            className="btn-ghost rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Upload size={14} className="mr-2 inline" />
            {uploadingMedia ? "Uploading..." : "Upload Media"}
          </button>
          {/*<button*/}
          {/*  type="button"*/}
          {/*  onClick={linkMedia}*/}
          {/*  disabled={attachingMedia}*/}
          {/*  className="btn-ghost rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"*/}
          {/*>*/}
          {/*  <Paperclip size={14} className="mr-2 inline" />*/}
          {/*  {attachingMedia ? "Attaching..." : "Attach Media to Post"}*/}
          {/*</button>*/}
          <button
            type="button"
            onClick={submitPost}
            disabled={postSubmitting}
            className="btn-primary rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            <Rocket size={14} className="mr-2 inline" />
            {postSubmitting ? "Saving..." : editingPostId ? "Update Post" : "New Post"}
          </button>
        </div>
      </header>

      {loading && <p className="mb-4 text-sm text-[var(--muted)]">Loading dashboard...</p>}
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
          {error}
        </p>
      )}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {stats.map((card) => (
          <article key={card.label} className="glass edge-glow rounded-3xl p-5">
            <card.icon size={18} className="text-cyan-300" />
            <p className="mt-4 text-2xl font-semibold">{card.value}</p>
            <p className="text-sm text-[var(--muted)]">{card.label}</p>
          </article>
        ))}
      </section>

      <section className="mt-8 grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <div ref={postFormRef} className="glass rounded-3xl p-5 md:p-6">
            <h2 className="text-xl font-semibold">
              {editingPostId ? "Edit Post" : "Create Post"}
            </h2>
            <div className="mt-4 grid gap-3">
              <input
                name="title"
                value={postForm.title}
                onChange={handlePostFormChange}
                placeholder="Post title"
                className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              />
              <RichTextEditor
                ref={editorRef}
                value={postForm.content}
                onChange={(content) => setPostForm((prev) => ({ ...prev, content }))}
                placeholder="Write your post with formatting, code blocks, and inline media..."
              />
              <input
                name="excerpt"
                value={postForm.excerpt}
                onChange={handlePostFormChange}
                placeholder="Excerpt"
                className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              />
              <input
                name="featuredImageUrl"
                value={postForm.featuredImageUrl}
                onChange={handlePostFormChange}
                placeholder="Featured image URL"
                className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              />
              <select
                name="status"
                value={postForm.status}
                onChange={handlePostFormChange}
                className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
              >
                <option value="DRAFT">DRAFT</option>
                <option value="PUBLISHED">PUBLISHED</option>
                <option value="UNPUBLISHED">UNPUBLISHED</option>
                <option value="ARCHIVED">ARCHIVED</option>
              </select>
              <div className="grid gap-3 md:grid-cols-2">
                <select
                  value={selectedCategoryId}
                  onChange={(event) => setSelectedCategoryId(event.target.value)}
                  className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                >
                  <option value="">Select category</option>
                  {categories.map((item) => (
                    <option key={item.categoryId} value={item.categoryId}>
                      {item.name}
                    </option>
                  ))}
                </select>
                <select
                  value={selectedTagId}
                  onChange={(event) => setSelectedTagId(event.target.value)}
                  className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none focus:border-cyan-300/70"
                >
                  <option value="">Select tag</option>
                  {tags.map((item) => (
                    <option key={item.tagId} value={item.tagId}>
                      {item.name}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-3 md:grid-cols-2">
                <div className="grid gap-2">
                  <input
                    value={newCategoryName}
                    onChange={(event) => setNewCategoryName(event.target.value)}
                    placeholder="New category name"
                    className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"
                  />
                  <button onClick={createCategory} className="btn-ghost rounded-full px-4 py-2 text-sm">
                    Create Category
                  </button>
                </div>
                <div className="grid gap-2">
                  <input
                    value={newTagName}
                    onChange={(event) => setNewTagName(event.target.value)}
                    placeholder="New tag name"
                    className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"
                  />
                  <button onClick={createTag} className="btn-ghost rounded-full px-4 py-2 text-sm">
                    Create Tag
                  </button>
                </div>
              </div>
              {categories.length === 0 && tags.length === 0 && (
                <p className="text-xs text-[var(--muted)]">
                  No categories/tags yet. Create one using fields above.
                </p>
              )}
              {editingPostId && (
                <button onClick={resetPostForm} className="btn-ghost rounded-full px-4 py-2 text-sm">
                  Cancel Edit
                </button>
              )}
            </div>
          </div>

          <div className="glass rounded-3xl p-5 md:p-6">
            <h2 className="text-xl font-semibold">Recent Posts</h2>
            <div className="mt-4 space-y-3">
              {posts.map((post) => (
                <div key={post.postId} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    {post.status === "PUBLISHED" ? (
                      <button
                        type="button"
                        onClick={() => openPostDetails(post)}
                        className="text-left font-medium text-[var(--primary)]  transition hover:text-cyan-100"
                      >
                        {post.title}
                      </button>
                    ) : (
                      <h3 className="font-medium text-white">{post.title}</h3>
                    )}
                    <span className="chip rounded-full px-2 py-1 text-[10px]">{post.status}</span>
                  </div>
                  <p className="mt-2 text-sm text-[var(--muted)]">{post.excerpt}</p>
                  <div className="mt-3 flex flex-wrap gap-2 text-xs text-[var(--muted)]">
                    <span>{post.readTimeMin || 1} min</span>
                    <span>|</span>
                    <span>{post.likesCount || 0} likes</span>
                    <span>|</span>
                    <span>{post.viewCount || 0} views</span>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-2">
                    {post.status === "PUBLISHED" && (
                      <button
                        type="button"
                        onClick={() => openPostDetails(post)}
                        className="btn-ghost rounded-full px-3 py-1 text-xs"
                      >
                        View Details
                      </button>
                    )}
                    <button type="button" onClick={() => startEdit(post)} className="btn-ghost rounded-full px-3 py-1 text-xs">
                      Edit
                    </button>
                    <button type="button" onClick={() => publishPost(post.postId)} className="btn-ghost rounded-full px-3 py-1 text-xs">
                      Publish
                    </button>
                    <button type="button" onClick={() => unpublishPost(post.postId)} className="btn-ghost rounded-full px-3 py-1 text-xs">
                      Unpublish
                    </button>
                    <button
                      type="button"
                      onClick={() => deletePost(post.postId)}
                      className="rounded-full border border-red-500/70 bg-red-500/20 px-3 py-1 text-xs font-semibold text-red-700 dark:text-red-200"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
              {posts.length === 0 && (
                <p className="text-sm text-[var(--muted)]">No posts yet.</p>
              )}
            </div>
          </div>
        </div>

        <aside>
          <div className="glass rounded-3xl p-5 md:p-6">
            <h2 className="text-xl font-semibold">Media</h2>
            <div className="mt-4 space-y-3">
              <input
                type="file"
                onChange={(event) => setMediaFile(event.target.files?.[0] || null)}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"
              />
              {mediaFile && (
                <div className="rounded-2xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-3">
                  <p className="text-xs text-[var(--muted)]">Selected file: {mediaFile.name}</p>
                  {localMediaPreviewUrl && String(mediaFile.type || "").startsWith("image/") && (
                    <img
                      src={localMediaPreviewUrl}
                      alt={mediaFile.name}
                      className="mt-2 h-28 w-full rounded-xl border border-[var(--line)] object-cover"
                    />
                  )}
                </div>
              )}
              <input
                value={mediaAltText}
                onChange={(event) => setMediaAltText(event.target.value)}
                placeholder="Alt text"
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"
              />
              <button
                type="button"
                onClick={uploadMedia}
                disabled={uploadingMedia}
                className="btn-primary w-full rounded-2xl py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"
              >
                {uploadingMedia ? "Uploading..." : "Upload Media"}
              </button>
              <select
                value={selectedMediaId}
                onChange={(event) => setSelectedMediaId(event.target.value)}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"
              >
                <option value="">Select media</option>
                {media.map((item) => (
                  <option key={item.mediaId} value={item.mediaId}>
                    {item.originalName}
                  </option>
                ))}
              </select>
              <select
                value={selectedPostId}
                onChange={(event) => setSelectedPostId(event.target.value)}
                className="w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"
              >
                <option value="">Select post for media link</option>
                {posts.map((item) => (
                  <option key={item.postId} value={item.postId}>
                    {item.title}
                  </option>
                ))}
              </select>
              {selectedMedia && (
                <div className="rounded-2xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-3 text-xs text-[var(--muted)]">
                  <p className="text-sm text-[var(--text)]">{selectedMedia.originalName}</p>
                  {String(selectedMedia.mimeType || "").startsWith("image/") && (
                    <img
                      src={selectedMedia.url}
                      alt={selectedMedia.altText || selectedMedia.originalName}
                      className="mt-2 h-28 w-full rounded-xl border border-[var(--line)] object-cover"
                      loading="lazy"
                    />
                  )}
                  {!String(selectedMedia.mimeType || "").startsWith("image/") && (
                    <a
                      href={selectedMedia.url}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-2 inline-block text-[var(--primary)] underline"
                    >
                      Preview media
                    </a>
                  )}
                </div>
              )}
              {/*<button*/}
              {/*  type="button"*/}
              {/*  onClick={linkMedia}*/}
              {/*  disabled={attachingMedia}*/}
              {/*  className="btn-ghost w-full rounded-2xl py-3 text-sm disabled:cursor-not-allowed disabled:opacity-60"*/}
              {/*>*/}
              {/*  {attachingMedia ? "Attaching..." : "Attach Media to Post"}*/}
              {/*</button>*/}
              <p className="text-xs text-[var(--muted)]">
                Attach inserts selected media into editor. If a post is selected, it also links media on the server.
              </p>
              {media.length > 0 && (
                <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
                  <p className="mb-2 text-xs uppercase text-[var(--muted)]">Uploaded Media</p>
                  <div className="max-h-52 space-y-2 overflow-auto pr-1">
                    {media.map((item) => (
                      <div key={item.mediaId} className="rounded-xl border border-white/10 p-2 text-xs text-[var(--muted)]">
                        <p className="text-white">{item.originalName}</p>
                        {String(item.mimeType || "").startsWith("image/") && (
                          <img
                            src={item.url}
                            alt={item.altText || item.originalName}
                            className="mt-2 h-20 w-full rounded-lg border border-white/10 object-cover"
                            loading="lazy"
                          />
                        )}
                        <p>
                          Post:{" "}
                          {item.linkedPostId
                            ? postTitleById[item.linkedPostId] || `Post ${item.linkedPostId}`
                            : "Not linked"}
                        </p>
                        <div className="mt-2 flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => setSelectedMediaId(String(item.mediaId))}
                            className="btn-ghost rounded-full px-2 py-1 text-[11px]"
                          >
                            Select
                          </button>
                          <button
                            type="button"
                            onClick={() => deleteMedia(item.mediaId)}
                            disabled={deletingMediaId === item.mediaId}
                            className="rounded-full border border-red-500/70 bg-red-500/20 px-2 py-1 text-[11px] font-semibold text-red-700 disabled:opacity-60 dark:text-red-200"
                          >
                            {deletingMediaId === item.mediaId ? "Deleting..." : "Delete"}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </aside>
      </section>

      {selectedPostDetails && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          onClick={closePostDetails}
        >
          <div
            className="glass edge-glow w-full max-w-4xl rounded-3xl p-5 md:p-6"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-4">
              <div>
                <h2 className="text-2xl font-semibold text-white">{selectedPostDetails.title}</h2>
                <p className="mt-2 text-sm text-[var(--muted)]">{selectedPostDetails.excerpt}</p>
              </div>
              <button
                type="button"
                onClick={closePostDetails}
                className="rounded-full border border-white/20 bg-white/5 p-2 text-[var(--muted)] transition hover:text-white"
              >
                <X size={16} />
              </button>
            </div>

            <div className="mt-4 grid gap-3 rounded-2xl border border-white/10 bg-white/5 p-4 text-xs text-[var(--muted)] md:grid-cols-4">
              <p>Status: {selectedPostDetails.status}</p>
              <p>Views: {selectedPostDetails.viewCount || 0}</p>
              <p>Likes: {selectedPostDetails.likesCount || 0}</p>
              <p>Comments: {postComments.length}</p>
            </div>

            <div className="mt-5">
              <h3 className="text-lg font-semibold text-white">Recent Comments</h3>
              {commentsLoading && (
                <p className="mt-3 text-sm text-[var(--muted)]">Loading comments...</p>
              )}
              {!commentsLoading && postComments.length === 0 && (
                <p className="mt-3 text-sm text-[var(--muted)]">No comments on this post.</p>
              )}
              {!commentsLoading && postComments.length > 0 && (
                <div className="mt-3 max-h-[420px] space-y-3 overflow-auto pr-1">
                  {postComments.map((comment) => {
                    const statusLabel =
                      comment.status === "PENDING"
                        ? "Pending"
                        : comment.status === "REJECTED"
                          ? "Rejected"
                          : comment.status === "DELETED"
                            ? "Deleted"
                            : "";

                    return (
                      <div
                        key={comment.commentId}
                        className="rounded-2xl border border-white/10 bg-white/5 p-3"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <p className="text-sm font-medium text-white">
                            {comment.authorName || "Unknown User"}
                          </p>
                          {statusLabel && (
                            <span className="chip rounded-full px-2 py-1 text-[10px]">
                              {statusLabel}
                            </span>
                          )}
                        </div>
                        <p className="mt-2 text-sm text-[var(--muted)]">{comment.content}</p>
                        <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[var(--muted)]">
                          <span>{formatDate(comment.createdAt)}</span>
                          {comment.parentCommentId && <span>Reply</span>}
                        </div>
                        <div className="mt-3 flex flex-wrap gap-2">
                          {comment.status === "PENDING" && (
                            <button
                              type="button"
                              onClick={() => approveComment(comment.commentId)}
                              disabled={commentActionLoadingId === comment.commentId}
                              className="btn-ghost inline-flex items-center rounded-full px-3 py-1 text-xs disabled:opacity-60"
                            >
                              <Check size={13} className="mr-1" />
                              Approve
                            </button>
                          )}
                          {comment.status !== "REJECTED" && comment.status !== "DELETED" && (
                            <button
                              type="button"
                              onClick={() => rejectComment(comment.commentId)}
                              disabled={commentActionLoadingId === comment.commentId}
                              className="btn-ghost inline-flex items-center rounded-full px-3 py-1 text-xs disabled:opacity-60"
                            >
                              Reject
                            </button>
                          )}
                          {comment.status !== "DELETED" && (
                            <button
                              type="button"
                              onClick={() => deleteComment(comment.commentId)}
                              disabled={commentActionLoadingId === comment.commentId}
                              className="inline-flex items-center rounded-full border border-red-500/70 bg-red-500/20 px-3 py-1 text-xs font-semibold text-red-700 disabled:opacity-60 dark:text-red-200"
                            >
                              <Trash2 size={13} className="mr-1" />
                              Delete
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AuthorDashboardPage;
