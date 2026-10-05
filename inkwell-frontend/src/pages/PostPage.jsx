import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import {
  Check,
  Clock3,
  Heart,
  MessageCircle,
  MessageSquareReply,
  Pencil,
  SendHorizontal,
  ThumbsUp,
  Trash2,
  X,
} from "lucide-react";
import { motion } from "framer-motion";
import { useAuth } from "../context/AuthContext.jsx";
import {
  commentService,
  mediaService,
  notificationService,
  postService,
  taxonomyService,
} from "../lib/services.js";

const SESSION_KEY = "inkwell_session_id";

function getSessionId() {
  const existing = localStorage.getItem(SESSION_KEY);
  if (existing) {
    return existing;
  }
  const generated = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  localStorage.setItem(SESSION_KEY, generated);
  return generated;
}

function formatDate(value) {
  if (!value) {
    return "";
  }
  return new Date(value).toLocaleString();
}

function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function sanitizeHtml(html) {
  if (!html) {
    return "";
  }
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  doc.querySelectorAll("script,style,iframe,object,embed").forEach((node) => node.remove());
  doc.querySelectorAll("*").forEach((node) => {
    [...node.attributes].forEach((attr) => {
      const name = attr.name.toLowerCase();
      const value = String(attr.value || "").toLowerCase();
      if (name.startsWith("on")) {
        node.removeAttribute(attr.name);
        return;
      }
      if ((name === "href" || name === "src") && value.startsWith("javascript:")) {
        node.removeAttribute(attr.name);
      }
    });
  });
  return doc.body.innerHTML;
}

function markdownToHtml(markdown) {
  let html = escapeHtml(markdown || "");
  const codeBlocks = [];
  html = html.replace(/```([\s\S]*?)```/g, (_, code) => {
    const key = `__CODE_BLOCK_${codeBlocks.length}__`;
    codeBlocks.push(`<pre class="rounded-xl border border-white/10 bg-black/30 p-3 overflow-auto"><code>${code}</code></pre>`);
    return key;
  });

  html = html.replace(/^### (.*)$/gm, "<h3>$1</h3>");
  html = html.replace(/^## (.*)$/gm, "<h2>$1</h2>");
  html = html.replace(/^# (.*)$/gm, "<h1>$1</h1>");
  html = html.replace(/^> (.*)$/gm, "<blockquote class=\"border-l-2 border-cyan-300/50 pl-3 italic\">$1</blockquote>");
  html = html.replace(/!\[([^\]]*)\]\((https?:\/\/[^\s)]+)\)/g, "<img src=\"$2\" alt=\"$1\" class=\"my-3 rounded-xl border border-white/10\" />");
  html = html.replace(/\[([^\]]+)\]\((https?:\/\/[^\s)]+)\)/g, "<a href=\"$2\" target=\"_blank\" rel=\"noreferrer\" class=\"text-[var(--primary)]  underline\">$1</a>");
  html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
  html = html.replace(/\*(?!\*)([^*]+)\*/g, "<em>$1</em>");
  html = html.replace(/`([^`]+)`/g, "<code class=\"rounded bg-white/10 px-1 py-0.5\">$1</code>");

  html = html.replace(/\n\n+/g, "</p><p>");
  html = `<p>${html.replace(/\n/g, "<br/>")}</p>`;
  codeBlocks.forEach((block, index) => {
    html = html.replaceAll(`__CODE_BLOCK_${index}__`, block);
  });
  return html;
}

function renderContentHtml(content) {
  const raw = String(content || "");
  const looksLikeHtml = /<[^>]+>/.test(raw);
  return looksLikeHtml ? sanitizeHtml(raw) : markdownToHtml(raw);
}

function PostPage() {
  const { slug } = useParams();
  const { token, user, isAuthenticated } = useAuth();
  const [post, setPost] = useState(null);
  const [tags, setTags] = useState([]);
  const [categories, setCategories] = useState([]);
  const [mediaItems, setMediaItems] = useState([]);
  const [comments, setComments] = useState([]);
  const [commentText, setCommentText] = useState("");
  const [replyTo, setReplyTo] = useState(null);
  const [editingCommentId, setEditingCommentId] = useState(null);
  const [editingCommentText, setEditingCommentText] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const normalizedRole = String(user?.role || "").toUpperCase();
  const isAdmin = normalizedRole === "ADMIN";

  const loadComments = async (postId) => {
    const rows = await commentService.getByPost(postId, token);
    setComments(Array.isArray(rows) ? rows : []);
  };

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    Promise.resolve()
      .then(async () => {
        const row = await postService.getBySlug(slug);
        if (!active) {
          return;
        }
        setPost(row);

        Promise.allSettled([
          postService.incrementViews(row.postId, getSessionId(), token),
          taxonomyService.getTagsByPost(row.postId),
          taxonomyService.getCategoriesByPost(row.postId),
          mediaService.getByPost(row.postId, token),
          loadComments(row.postId),
        ]).then((results) => {
          if (!active) {
            return;
          }
          const tagsRes = results[1];
          const categoriesRes = results[2];
          const mediaRes = results[3];
          if (tagsRes.status === "fulfilled" && Array.isArray(tagsRes.value)) {
            setTags(tagsRes.value);
          } else {
            setTags([]);
          }
          if (
            categoriesRes.status === "fulfilled" &&
            Array.isArray(categoriesRes.value)
          ) {
            setCategories(categoriesRes.value);
          } else {
            setCategories([]);
          }
          if (mediaRes.status === "fulfilled" && Array.isArray(mediaRes.value)) {
            setMediaItems(mediaRes.value);
          } else {
            setMediaItems([]);
          }
        });
      })
      .catch((err) => {
        if (!active) {
          return;
        }
        setError(err.message || "Failed to load post");
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [slug, token]);

  const visibleComments = useMemo(() => {
    if (!isAuthenticated || !user) {
      return comments.filter((item) => item.status === "APPROVED");
    }
    const isPostOwner = post && user.userId === post.authorId;
    const isAdmin = String(user.role || "").toUpperCase() === "ADMIN";
    if (isPostOwner || isAdmin) {
      return comments;
    }
    return comments.filter(
      (item) => item.status === "APPROVED" || item.authorId === user.userId
    );
  }, [comments, isAuthenticated, post, user]);

  const topComments = useMemo(
    () => visibleComments.filter((item) => !item.parentCommentId),
    [visibleComments]
  );

  const repliesByParent = useMemo(() => {
    const map = new Map();
    visibleComments
      .filter((item) => item.parentCommentId)
      .forEach((item) => {
        const list = map.get(item.parentCommentId) || [];
        list.push(item);
        map.set(item.parentCommentId, list);
      });
    return map;
  }, [visibleComments]);

  const handleCreateComment = async () => {
    if (!post || !user) {
      return;
    }
    const messageText = commentText.trim();
    if (!messageText) {
      return;
    }
    setSubmitting(true);
    setError("");
    try {
      await commentService.add(
        {
          postId: post.postId,
          authorId: user.userId,
          authorName: user.fullName || user.username || `User #${user.userId}`,
          parentCommentId: replyTo,
          content: messageText,
        },
        token
      );
      setCommentText("");
      setReplyTo(null);
      await loadComments(post.postId);
    } catch (err) {
      setError(err.message || "Failed to add comment");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteComment = async (commentId) => {
    if (!post || !user) {
      return;
    }
    setError("");
    try {
      await commentService.delete(commentId, user.userId, token);
      await loadComments(post.postId);
    } catch (err) {
      setError(err.message || "Failed to delete comment");
    }
  };

  const startEditComment = (comment) => {
    setEditingCommentId(comment.commentId);
    setEditingCommentText(comment.content || "");
  };

  const cancelEditComment = () => {
    setEditingCommentId(null);
    setEditingCommentText("");
  };

  const saveEditComment = async (commentId) => {
    if (!post || !user || !editingCommentText.trim()) {
      return;
    }
    setError("");
    try {
      await commentService.update(
        commentId,
        { authorId: user.userId, content: editingCommentText.trim() },
        token
      );
      cancelEditComment();
      await loadComments(post.postId);
    } catch (err) {
      setError(err.message || "Failed to update comment");
    }
  };

  const handleLikePost = async () => {
    if (!post || !user) {
      return;
    }
    try {
      await postService.like(post.postId, user.userId, token);
      const updated = await postService.getBySlug(post.slug);
      setPost(updated);
    } catch (err) {
      setError(err.message || "Failed to like post");
    }
  };

  const handleUnlikePost = async () => {
    if (!post || !user) {
      return;
    }
    try {
      await postService.unlike(post.postId, user.userId, token);
      const updated = await postService.getBySlug(post.slug);
      setPost(updated);
    } catch (err) {
      setError(err.message || "Failed to unlike post");
    }
  };

  const handleLikeComment = async (commentId) => {
    if (!post || !user) {
      return;
    }
    try {
      const targetComment = comments.find((item) => item.commentId === commentId) || null;
      await commentService.like(commentId, user.userId, token);
      if (targetComment?.authorId && targetComment.authorId !== user.userId) {
        notificationService
          .send(
            {
              recipientId: targetComment.authorId,
              actorId: user.userId,
              type: "LIKE",
              title: `${user.fullName || user.username || "Someone"} liked your comment`,
              message: targetComment.content,
              relatedId: commentId,
              relatedType: "COMMENT",
            },
            token
          )
          .catch(() => null);
      }
      await loadComments(post.postId);
    } catch (err) {
      setError(err.message || "Failed to like comment");
    }
  };

  const handleUnlikeComment = async (commentId) => {
    if (!post || !user) {
      return;
    }
    try {
      await commentService.unlike(commentId, user.userId, token);
      await loadComments(post.postId);
    } catch (err) {
      setError(err.message || "Failed to unlike comment");
    }
  };

  if (loading) {
    return (
      <div className="container-shell pb-16">
        <p className="text-sm text-[var(--muted)]">Loading post...</p>
      </div>
    );
  }

  if (!post) {
    return (
      <div className="container-shell pb-16">
        <p className="rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
          {error || "Post not found"}
        </p>
      </div>
    );
  }

  return (
    <div className="container-shell pb-16">
      {error && (
        <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">
          {error}
        </p>
      )}

      <article className="glass edge-glow rounded-4xl p-6 md:p-9">
        <span className="chip rounded-full px-3 py-1 text-xs uppercase">
          {categories[0]?.name || post.status}
        </span>
        <h1 className="mt-4 text-3xl font-semibold leading-tight md:text-5xl">
          {post.title}
        </h1>
        <div className="mt-4 flex flex-wrap gap-4 text-sm text-[var(--muted)]">
          {Number.isFinite(Number(post.authorId)) && Number(post.authorId) > 0 ? (
            <Link to={`/authors/${post.authorId}`} className="text-[var(--primary)]  hover:text-white">
              {post.authorName || `User #${post.authorId}`}
            </Link>
          ) : (
            <span>{post.authorName || "Unknown author"}</span>
          )}
          {/*<span className="flex items-center gap-1">*/}
          {/*  <Clock3 size={14} /> {post.readTimeMin || 1} min*/}
          {/*</span>*/}
          <span className="flex items-center gap-1">
            <Heart size={14} /> {post.likesCount || 0}
          </span>
          <span className="flex items-center gap-1">
            <MessageCircle size={14} /> {topComments.length}
          </span>
          <span>Views: {post.viewCount || 0}</span>
        </div>
        {tags.length > 0 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {tags.map((tag) => (
              <span key={tag.tagId} className="chip rounded-full px-3 py-1 text-xs">
                #{tag.name}
              </span>
            ))}
          </div>
        )}

        <div
          className="mt-8 text-[15px] leading-relaxed text-[var(--muted)] [&_h1]:mb-3 [&_h1]:text-3xl [&_h1]:text-[var(--text)] [&_h2]:mb-3 [&_h2]:text-2xl [&_h2]:text-[var(--text)] [&_h3]:mb-2 [&_h3]:text-xl [&_h3]:text-[var(--text)] [&_p]:mb-3"
          dangerouslySetInnerHTML={{ __html: renderContentHtml(post.content) }}
        />
        {post.featuredImageUrl && (
          <div className="mt-6 overflow-hidden rounded-2xl border border-white/10">
            <img
              src={post.featuredImageUrl}
              alt={post.title}
              className="h-auto w-full object-cover"
            />
          </div>
        )}
        {mediaItems.length > 0 && (
          <div className="mt-6 grid gap-3 md:grid-cols-2">
            {mediaItems.map((item) => {
              const isImage = String(item.mimeType || "").startsWith("image/");
              if (isImage) {
                return (
                  <div key={item.mediaId} className="overflow-hidden rounded-2xl border border-white/10">
                    <img src={item.url} alt={item.altText || item.originalName} className="h-52 w-full object-cover" />
                  </div>
                );
              }
              return (
                <a
                  key={item.mediaId}
                  href={item.url}
                  target="_blank"
                  rel="noreferrer"
                  className="glass rounded-2xl p-4 text-sm text-[var(--primary)] "
                >
                  {item.originalName}
                </a>
              );
            })}
          </div>
        )}

        {isAuthenticated && (
          <div className="mt-6 flex gap-2">
            <motion.button
              whileTap={{ scale: 0.92 }}
              onClick={handleLikePost}
              className="icon-action inline-flex items-center rounded-full px-4 py-2 text-xs"
              title="Like post"
            >
              <ThumbsUp size={14} className="mr-2 text-[var(--primary)]" />
              Like
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.92 }}
              onClick={handleUnlikePost}
              className="icon-action inline-flex items-center rounded-full px-4 py-2 text-xs"
              title="Unlike post"
            >
              <X size={14} className="mr-2" />
              Unlike
            </motion.button>
          </div>
        )}
      </article>

      <section className="mt-8 grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Discussion</h2>
          <div className="mt-4 space-y-4">
            {topComments.map((comment) => {
              const commentDeleted = comment.status === "DELETED";
              const canDelete =
                Boolean(user?.userId) &&
                (user.userId === comment.authorId || isAdmin || user.userId === post.authorId);
              const canEdit = Boolean(user?.userId) && user.userId === comment.authorId;

              return (
                <div key={comment.commentId} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-medium text-white">
                      {comment.authorName || `User #${comment.authorId}`}
                    </p>
                    <span className="text-xs text-[var(--muted)]">{comment.status}</span>
                  </div>
                  <p className="mt-2 text-sm text-[var(--muted)]">{comment.content}</p>
                  <p className="mt-2 text-xs text-[var(--muted)]">
                    Likes: {comment.likesCount || 0} | {formatDate(comment.createdAt)}
                  </p>

                  <div className="mt-3 flex flex-wrap gap-2">
                    {isAuthenticated && !commentDeleted && (
                      <>
                        <motion.button
                          whileTap={{ scale: 0.92 }}
                          onClick={() => handleLikeComment(comment.commentId)}
                          className="icon-action inline-flex items-center rounded-full px-3 py-1 text-xs"
                          title="Like comment"
                        >
                          <ThumbsUp size={12} className="mr-1 text-[var(--primary)]" />
                          Like
                        </motion.button>
                        <motion.button
                          whileTap={{ scale: 0.92 }}
                          onClick={() => handleUnlikeComment(comment.commentId)}
                          className="icon-action inline-flex items-center rounded-full px-3 py-1 text-xs"
                          title="Unlike comment"
                        >
                          <X size={12} className="mr-1" />
                          Unlike
                        </motion.button>
                        <motion.button
                          whileTap={{ scale: 0.92 }}
                          onClick={() => setReplyTo(comment.commentId)}
                          className="icon-action inline-flex items-center rounded-full px-3 py-1 text-xs"
                          title="Reply"
                        >
                          <MessageSquareReply size={12} className="mr-1" />
                          Reply
                        </motion.button>
                      </>
                    )}

                    {canEdit && !commentDeleted && (
                      <motion.button
                        whileTap={{ scale: 0.92 }}
                        onClick={() => startEditComment(comment)}
                        className="icon-action inline-flex items-center rounded-full px-3 py-1 text-xs"
                      >
                        <Pencil size={12} className="mr-1" />
                        Edit
                      </motion.button>
                    )}

                    {canDelete && !commentDeleted && (
                      <motion.button
                        whileTap={{ scale: 0.92 }}
                        onClick={() => handleDeleteComment(comment.commentId)}
                        className="inline-flex items-center rounded-full border border-red-300/40 bg-red-500/10 px-3 py-1 text-xs text-red-600 dark:text-red-200"
                      >
                        <Trash2 size={12} className="mr-1" />
                        Delete
                      </motion.button>
                    )}
                  </div>

                  {editingCommentId === comment.commentId && !commentDeleted && (
                    <div className="mt-3 space-y-2 rounded-xl border border-white/10 bg-black/20 p-3">
                      <textarea
                        rows={3}
                        value={editingCommentText}
                        onChange={(event) => setEditingCommentText(event.target.value)}
                        className="w-full rounded-xl border border-white/15 bg-white/5 p-2 text-sm outline-none focus:border-cyan-300/70"
                      />
                      <div className="flex gap-2">
                        <motion.button
                          whileTap={{ scale: 0.92 }}
                          onClick={() => saveEditComment(comment.commentId)}
                          className="btn-primary inline-flex items-center rounded-full px-3 py-1 text-xs"
                        >
                          <Check size={12} className="mr-1" />
                          Save
                        </motion.button>
                        <motion.button
                          whileTap={{ scale: 0.92 }}
                          onClick={cancelEditComment}
                          className="icon-action inline-flex items-center rounded-full px-3 py-1 text-xs"
                        >
                          <X size={12} className="mr-1" />
                          Cancel
                        </motion.button>
                      </div>
                    </div>
                  )}

                  {(repliesByParent.get(comment.commentId) || []).length > 0 && (
                    <div className="mt-3 rounded-xl border border-white/10 bg-black/20 p-3">
                      {(repliesByParent.get(comment.commentId) || []).map((reply) => {
                        const replyDeleted = reply.status === "DELETED";
                        const canEditReply = Boolean(user?.userId) && user.userId === reply.authorId;
                        const canDeleteReply =
                          Boolean(user?.userId) &&
                          (user.userId === reply.authorId || isAdmin || user.userId === post.authorId);

                        return (
                          <div key={reply.commentId} className="mb-2 last:mb-0">
                            <p className="text-xs font-medium text-white">
                              {reply.authorName || `User #${reply.authorId}`}
                            </p>
                            <p className="mt-1 text-xs text-[var(--muted)]">{reply.content}</p>

                            {canEditReply && !replyDeleted && (
                              <div className="mt-2 flex gap-2">
                                <motion.button
                                  whileTap={{ scale: 0.92 }}
                                  onClick={() => startEditComment(reply)}
                                  className="icon-action inline-flex items-center rounded-full px-2 py-1 text-[11px]"
                                >
                                  <Pencil size={11} className="mr-1" />
                                  Edit
                                </motion.button>
                              </div>
                            )}

                            {canDeleteReply && !replyDeleted && (
                              <div className="mt-2">
                                <motion.button
                                  whileTap={{ scale: 0.92 }}
                                  onClick={() => handleDeleteComment(reply.commentId)}
                                  className="inline-flex items-center rounded-full border border-red-300/40 bg-red-500/10 px-2 py-1 text-[11px] text-red-600 dark:text-red-200"
                                >
                                  <Trash2 size={11} className="mr-1" />
                                  Delete
                                </motion.button>
                              </div>
                            )}

                            {editingCommentId === reply.commentId && !replyDeleted && (
                              <div className="mt-2 space-y-2 rounded-xl border border-white/10 bg-black/20 p-2">
                                <textarea
                                  rows={2}
                                  value={editingCommentText}
                                  onChange={(event) => setEditingCommentText(event.target.value)}
                                  className="w-full rounded-xl border border-white/15 bg-white/5 p-2 text-xs outline-none focus:border-cyan-300/70"
                                />
                                <div className="flex gap-2">
                                  <motion.button
                                    whileTap={{ scale: 0.92 }}
                                    onClick={() => saveEditComment(reply.commentId)}
                                    className="btn-primary inline-flex items-center rounded-full px-2 py-1 text-[11px]"
                                  >
                                    <Check size={11} className="mr-1" />
                                    Save
                                  </motion.button>
                                  <motion.button
                                    whileTap={{ scale: 0.92 }}
                                    onClick={cancelEditComment}
                                    className="icon-action inline-flex items-center rounded-full px-2 py-1 text-[11px]"
                                  >
                                    <X size={11} className="mr-1" />
                                    Cancel
                                  </motion.button>
                                </div>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              );
            })}
            {topComments.length === 0 && (
              <p className="text-sm text-[var(--muted)]">No comments yet.</p>
            )}
          </div>
        </div>

        <aside className="glass rounded-3xl p-5 md:p-6">
          <h3 className="text-lg font-semibold">
            {replyTo ? "Add Reply" : "Add Comment"}
          </h3>
          {!isAuthenticated && (
            <p className="mt-3 text-sm text-[var(--muted)]">
              Please <Link to="/login" className="text-[var(--primary)] ">login</Link> to comment.
            </p>
          )}
          <textarea
            rows={5}
            value={commentText}
            onChange={(event) => setCommentText(event.target.value)}
            placeholder="Write your thoughts..."
            disabled={!isAuthenticated}
            className="mt-3 w-full rounded-2xl border border-white/15 bg-white/5 p-3 text-sm outline-none focus:border-cyan-300/70 disabled:opacity-60"
          />
          <button
            onClick={handleCreateComment}
            disabled={!isAuthenticated || !commentText.trim() || submitting}
            className="btn-primary mt-3 inline-flex items-center rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60"
          >
            Send <SendHorizontal size={14} className="ml-2" />
          </button>
          {replyTo && (
            <button
              onClick={() => setReplyTo(null)}
              className="btn-ghost mt-3 block rounded-full px-4 py-2 text-center text-sm"
            >
              Cancel Reply
            </button>
          )}
          <Link to="/feed" className="btn-ghost mt-3 block rounded-full px-4 py-2 text-center text-sm">
            Back to Feed
          </Link>
        </aside>
      </section>
    </div>
  );
}

export default PostPage;
