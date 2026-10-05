import { useEffect, useMemo, useState } from "react";
import { BarChart3, FileChartColumn, ImageIcon, X } from "lucide-react";
import {
  authService,
  commentService,
  mediaService,
  newsletterService,
  notificationService,
  postService,
  taxonomyService,
} from "../lib/services.js";
import { useAuth } from "../context/AuthContext.jsx";
import { useNotification } from "../context/NotificationContext.jsx";

function formatDate(value) {
  return value ? new Date(value).toLocaleString() : "";
}

function AdminPanelPage() {
  const { token, user } = useAuth();
  const notify = useNotification();
  const [users, setUsers] = useState([]);
  const [allUsers, setAllUsers] = useState([]);
  const [posts, setPosts] = useState([]);
  const [mostViewed, setMostViewed] = useState([]);
  const [comments, setComments] = useState([]);
  const [categories, setCategories] = useState([]);
  const [tags, setTags] = useState([]);
  const [trendingTags, setTrendingTags] = useState([]);
  const [subscribers, setSubscribers] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [mediaRows, setMediaRows] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [counts, setCounts] = useState({ users: 0, posts: 0, comments: 0 });

  const [searchText, setSearchText] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [activeFilter, setActiveFilter] = useState("ALL");
  const [commentStatus, setCommentStatus] = useState("ALL");

  const [categoryForm, setCategoryForm] = useState({ name: "", description: "", parentCategoryId: "" });
  const [editingCategoryId, setEditingCategoryId] = useState(null);
  const [tagName, setTagName] = useState("");
  const [editingTagId, setEditingTagId] = useState(null);

  const [newsletterSubject, setNewsletterSubject] = useState("");
  const [newsletterContent, setNewsletterContent] = useState("");
  const [newsletterStatus, setNewsletterStatus] = useState("ACTIVE");
  const [newsletterPreferences, setNewsletterPreferences] = useState("");
  const [postForNotification, setPostForNotification] = useState("");

  const [broadcastRole, setBroadcastRole] = useState("ALL");
  const [broadcastTitle, setBroadcastTitle] = useState("Admin Broadcast");
  const [broadcastMessage, setBroadcastMessage] = useState("");

  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyAction, setBusyAction] = useState(false);
  const [authorRequestsOpen, setAuthorRequestsOpen] = useState(false);
  const [authorRequestStatusFilter, setAuthorRequestStatusFilter] = useState("PENDING");
  const [authorRequests, setAuthorRequests] = useState([]);
  const [authorRequestsLoading, setAuthorRequestsLoading] = useState(false);
  const [authorRequestsError, setAuthorRequestsError] = useState("");
  const [selectedAuthorRequestId, setSelectedAuthorRequestId] = useState(null);
  const [selectedAuthorRequest, setSelectedAuthorRequest] = useState(null);
  const [authorDecisionReason, setAuthorDecisionReason] = useState("");
  const [authorDecisionBusy, setAuthorDecisionBusy] = useState(false);

  const userMap = useMemo(() => {
    const map = {};
    allUsers.forEach((entry) => {
      map[entry.userId] = entry;
    });
    return map;
  }, [allUsers]);
  const postTitleMap = useMemo(() => {
    const map = {};
    posts.forEach((entry) => {
      if (entry?.postId != null) {
        map[entry.postId] = entry.title || `Post #${entry.postId}`;
      }
    });
    return map;
  }, [posts]);

  const mostActiveAuthors = useMemo(() => {
    const byAuthor = {};
    posts.forEach((post) => {
      if (!post.authorId) return;
      if (!byAuthor[post.authorId]) {
        byAuthor[post.authorId] = { authorId: post.authorId, postsCount: 0, totalViews: 0, name: post.authorName || "" };
      }
      byAuthor[post.authorId].postsCount += 1;
      byAuthor[post.authorId].totalViews += post.viewCount || 0;
    });
    return Object.values(byAuthor)
      .map((entry) => ({
        ...entry,
        name: userMap[entry.authorId]?.fullName || userMap[entry.authorId]?.username || entry.name || `User #${entry.authorId}`,
      }))
      .sort((a, b) => (b.postsCount - a.postsCount) || (b.totalViews - a.totalViews))
      .slice(0, 5);
  }, [posts, userMap]);

  const subscriberStats = useMemo(() => {
    const out = { ACTIVE: 0, PENDING: 0, UNSUBSCRIBED: 0 };
    subscribers.forEach((row) => {
      const key = String(row.status || "").toUpperCase();
      if (key in out) out[key] += 1;
    });
    return out;
  }, [subscribers]);

  const activeMediaCount = useMemo(
    () => mediaRows.filter((entry) => !entry.deleted).length,
    [mediaRows]
  );
  const deletedMediaCount = useMemo(
    () => mediaRows.filter((entry) => entry.deleted).length,
    [mediaRows]
  );
  const maxPostViews = useMemo(
    () => Math.max(1, ...mostViewed.map((entry) => entry.viewCount || 0)),
    [mostViewed]
  );

  const load = async () => {
    const active = activeFilter === "ALL" ? undefined : activeFilter === "ACTIVE";
    const commentFilter = commentStatus === "ALL" ? undefined : commentStatus;
    const [
      filteredUsers,
      allUsersRows,
      postRows,
      mostViewedRows,
      commentRows,
      categoryRows,
      tagRows,
      trendingRows,
      subscriberRows,
      subscriptionRows,
      mediaList,
      logs,
      userCountRes,
      postCountRes,
      commentCountRes,
    ] = await Promise.all([
      authService.getUsers({ q: searchText || undefined, role: roleFilter || undefined, active }, token),
      authService.getUsers({}, token),
      postService.getAll(token),
      postService.getMostViewed(5, token),
      commentService.getAll(commentFilter, token),
      taxonomyService.getCategories(),
      taxonomyService.getTags(),
      taxonomyService.getTrendingTags(),
      newsletterService.getAll(token),
      authService.getAllSubscriptions(token),
      mediaService.getAll(false, token),
      authService.getAuditLogs(120, token),
      authService.countUsers({}, token),
      postService.count(undefined, token),
      commentService.count(undefined, token),
    ]);
    setUsers(Array.isArray(filteredUsers) ? filteredUsers : []);
    setAllUsers(Array.isArray(allUsersRows) ? allUsersRows : []);
    setPosts(Array.isArray(postRows) ? postRows : []);
    setMostViewed(Array.isArray(mostViewedRows) ? mostViewedRows : []);
    setComments(Array.isArray(commentRows) ? commentRows : []);
    setCategories(Array.isArray(categoryRows) ? categoryRows : []);
    setTags(Array.isArray(tagRows) ? tagRows : []);
    setTrendingTags(Array.isArray(trendingRows) ? trendingRows : []);
    setSubscribers(Array.isArray(subscriberRows) ? subscriberRows : []);
    setSubscriptions(Array.isArray(subscriptionRows) ? subscriptionRows : []);
    setMediaRows(Array.isArray(mediaList) ? mediaList : []);
    setAuditLogs(Array.isArray(logs) ? logs : []);
    setCounts({
      users: userCountRes?.count || 0,
      posts: postCountRes?.count || 0,
      comments: commentCountRes?.count || 0,
    });
  };

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    load()
      .catch((err) => active && setError(err.message || "Failed to load admin data"))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [token, searchText, roleFilter, activeFilter, commentStatus]);

  const loadAuthorRequests = async () => {
    setAuthorRequestsLoading(true);
    setAuthorRequestsError("");
    try {
      const status =
        authorRequestStatusFilter === "ALL" ? undefined : authorRequestStatusFilter;
      const rows = await authService.getAuthorRequests({ status }, token);
      const list = Array.isArray(rows) ? rows : [];
      setAuthorRequests(list);
      if (list.length === 0) {
        setSelectedAuthorRequestId(null);
        setSelectedAuthorRequest(null);
        setAuthorDecisionReason("");
        return;
      }
      if (!selectedAuthorRequestId || !list.some((entry) => entry.requestId === selectedAuthorRequestId)) {
        const first = list[0];
        setSelectedAuthorRequestId(first.requestId);
        setSelectedAuthorRequest(first);
        setAuthorDecisionReason(first.decisionReason || "");
      }
    } catch (err) {
      setAuthorRequestsError(err.message || "Failed to load author requests");
    } finally {
      setAuthorRequestsLoading(false);
    }
  };

  const loadAuthorRequestDetails = async (requestId) => {
    setSelectedAuthorRequestId(requestId);
    setAuthorRequestsError("");
    try {
      const details = await authService.getAuthorRequestById(requestId, token);
      setSelectedAuthorRequest(details || null);
      setAuthorDecisionReason(details?.decisionReason || "");
    } catch (err) {
      setAuthorRequestsError(err.message || "Failed to load request details");
    }
  };

  useEffect(() => {
    if (!authorRequestsOpen) {
      return;
    }
    loadAuthorRequests();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [authorRequestsOpen, authorRequestStatusFilter, token]);

  const decideAuthorRequest = async (approve) => {
    if (!selectedAuthorRequest?.requestId) {
      return;
    }
    if (!approve && !authorDecisionReason.trim()) {
      const message = "Reason is required when rejecting author request";
      setAuthorRequestsError(message);
      notify.info(message);
      return;
    }
    setAuthorDecisionBusy(true);
    setAuthorRequestsError("");
    try {
      const payload = {
        approve,
        reason: authorDecisionReason.trim() || undefined,
      };
      const updated = await authService.decideAuthorRequest(
        selectedAuthorRequest.requestId,
        payload,
        token
      );
      setSelectedAuthorRequest(updated || null);
      setAuthorDecisionReason(updated?.decisionReason || "");
      notify.success(approve ? "Author request approved" : "Author request rejected");
      await loadAuthorRequests();
      await load();
    } catch (err) {
      const message = err.message || "Failed to review author request";
      setAuthorRequestsError(message);
      notify.error(message);
    } finally {
      setAuthorDecisionBusy(false);
    }
  };

  const audit = async (payload) => {
    try {
      await authService.recordAudit(payload, token);
    } catch {
      return;
    }
  };

  const runAction = async (action, message, auditPayload) => {
    if (busyAction) {
      return;
    }
    setBusyAction(true);
    setError("");
    try {
      await action();
      if (auditPayload) await audit(auditPayload);
      notify.success(message);
      await load();
    } catch (err) {
      const errorMessage = err.message || "Action failed";
      setError(errorMessage);
      notify.error(errorMessage);
    } finally {
      setBusyAction(false);
    }
  };

  return (
    <div className="container-shell pb-16">
      <header className="mb-8">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="hero-title text-3xl">Admin Panel</h1>
            <p className="mt-2 text-sm text-[var(--muted)]">All platform admin operations in one place.</p>
          </div>
          <button
            type="button"
            onClick={() => setAuthorRequestsOpen(true)}
            className="btn-primary rounded-full px-4 py-2 text-sm"
          >
            Author Requests
          </button>
        </div>
      </header>

      {loading && <p className="mb-3 text-sm text-[var(--muted)]">Loading admin panel...</p>}
      {error && <p className="mb-4 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-3 text-sm text-red-500">{error}</p>}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <article className="glass edge-glow rounded-3xl p-5"><p className="text-2xl font-semibold">{counts.users}</p><p className="text-sm text-[var(--muted)]">Users</p></article>
        <article className="glass edge-glow rounded-3xl p-5"><p className="text-2xl font-semibold">{counts.posts}</p><p className="text-sm text-[var(--muted)]">Posts</p></article>
        <article className="glass edge-glow rounded-3xl p-5"><p className="text-2xl font-semibold">{counts.comments}</p><p className="text-sm text-[var(--muted)]">Comments</p></article>
        <article className="glass edge-glow rounded-3xl p-5"><p className="text-2xl font-semibold">{mediaRows.length}</p><p className="text-sm text-[var(--muted)]">Media</p></article>
      </section>

      <section className="mt-8 grid gap-6 lg:grid-cols-2">
        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Users</h2>
          <div className="mt-4 grid gap-2 md:grid-cols-3">
            <input value={searchText} onChange={(e) => setSearchText(e.target.value)} placeholder="Search users" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none md:col-span-2" />
            <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="">All roles</option><option value="READER">READER</option><option value="AUTHOR">AUTHOR</option><option value="ADMIN">ADMIN</option></select>
            <select value={activeFilter} onChange={(e) => setActiveFilter(e.target.value)} className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="ALL">All status</option><option value="ACTIVE">Active</option><option value="SUSPENDED">Suspended</option></select>
          </div>
          <div className="mt-3 max-h-[360px] space-y-2 overflow-auto pr-1">
            {users.map((entry) => (
              <div key={entry.userId} className="rounded-xl border border-white/10 bg-white/5 p-3 text-xs">
                <p className="text-white">{entry.fullName} ({entry.email})</p>
                <p className="text-[var(--muted)]">@{entry.username} | {entry.active ? "ACTIVE" : "SUSPENDED"}</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <select value={entry.role} onChange={(e) => runAction(() => authService.changeUserRole(entry.userId, e.target.value, token), "Role updated", { action: "CHANGE_ROLE", targetType: "USER", targetId: entry.userId, details: e.target.value })} className="rounded-xl border border-white/15 bg-white/5 px-2 py-1 text-xs"><option value="READER">READER</option><option value="AUTHOR">AUTHOR</option><option value="ADMIN">ADMIN</option></select>
                  {entry.active ? <button onClick={() => runAction(() => authService.suspendUser(entry.userId, token), "User suspended", { action: "SUSPEND_USER", targetType: "USER", targetId: entry.userId })} className="rounded-full border border-orange-500/70 bg-orange-500/20 px-3 py-1 text-xs font-semibold text-orange-700 dark:text-orange-200">Suspend</button> : <button onClick={() => runAction(() => authService.reactivateUser(entry.userId, token), "User reactivated", { action: "REACTIVATE_USER", targetType: "USER", targetId: entry.userId })} className="btn-ghost rounded-full px-3 py-1 text-xs">Reactivate</button>}
                  <button onClick={() => runAction(() => authService.deleteUser(entry.userId, token), "User deleted", { action: "DELETE_USER", targetType: "USER", targetId: entry.userId })} className="rounded-full border border-red-500/70 bg-red-500/20 px-3 py-1 text-xs font-semibold text-red-700 dark:text-red-200">Delete</button>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Posts</h2>
          <div className="mt-3 max-h-[360px] space-y-2 overflow-auto pr-1">
            {posts.map((entry) => (
              <div key={entry.postId} className="rounded-xl border border-white/10 bg-white/5 p-3 text-xs">
                <p className="text-white">{entry.title}</p>
                <p className="text-[var(--muted)]">{entry.status} | {entry.featured ? "FEATURED" : "NORMAL"} | {entry.viewCount || 0} views</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <button onClick={() => runAction(() => postService.feature(entry.postId, !entry.featured, token), "Post feature updated", { action: entry.featured ? "UNFEATURE_POST" : "FEATURE_POST", targetType: "POST", targetId: entry.postId })} className="btn-ghost rounded-full px-3 py-1 text-xs">{entry.featured ? "Unfeature" : "Feature"}</button>
                  <button onClick={() => runAction(() => postService.publish(entry.postId, token), "Post published", { action: "PUBLISH_POST", targetType: "POST", targetId: entry.postId })} className="btn-ghost rounded-full px-3 py-1 text-xs">Publish</button>
                  <button onClick={() => runAction(() => postService.unpublish(entry.postId, token), "Post unpublished", { action: "UNPUBLISH_POST", targetType: "POST", targetId: entry.postId })} className="btn-ghost rounded-full px-3 py-1 text-xs">Unpublish</button>
                  <button onClick={() => runAction(() => postService.delete(entry.postId, token), "Post deleted", { action: "DELETE_POST", targetType: "POST", targetId: entry.postId })} className="rounded-full border border-red-500/70 bg-red-500/20 px-3 py-1 text-xs font-semibold text-red-700 dark:text-red-200">Delete</button>
                </div>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="mt-6 grid gap-6 lg:grid-cols-2">
        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Categories & Tags</h2>
          <div className="mt-4 grid gap-2">
            <input value={categoryForm.name} onChange={(e) => setCategoryForm((p) => ({ ...p, name: e.target.value }))} placeholder="Category name" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <input value={categoryForm.description} onChange={(e) => setCategoryForm((p) => ({ ...p, description: e.target.value }))} placeholder="Description" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <select value={categoryForm.parentCategoryId} onChange={(e) => setCategoryForm((p) => ({ ...p, parentCategoryId: e.target.value }))} className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="">No parent</option>{categories.filter((c) => c.categoryId !== editingCategoryId).map((c) => <option key={c.categoryId} value={c.categoryId}>{c.name}</option>)}</select>
            <button onClick={() => runAction(() => editingCategoryId ? taxonomyService.updateCategory(editingCategoryId, { name: categoryForm.name, description: categoryForm.description || null, parentCategoryId: categoryForm.parentCategoryId ? Number(categoryForm.parentCategoryId) : null }, token) : taxonomyService.createCategory({ name: categoryForm.name, description: categoryForm.description || null, parentCategoryId: categoryForm.parentCategoryId ? Number(categoryForm.parentCategoryId) : null }, token), editingCategoryId ? "Category updated" : "Category created", { action: editingCategoryId ? "UPDATE_CATEGORY" : "CREATE_CATEGORY", targetType: "CATEGORY", targetId: editingCategoryId || null, details: categoryForm.name })} className="btn-primary rounded-full px-4 py-2 text-sm">{editingCategoryId ? "Update Category" : "Create Category"}</button>
            <input value={tagName} onChange={(e) => setTagName(e.target.value)} placeholder="Tag name" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <button onClick={() => runAction(() => editingTagId ? taxonomyService.updateTag(editingTagId, { name: tagName }, token) : taxonomyService.createTag({ name: tagName }, token), editingTagId ? "Tag updated" : "Tag created", { action: editingTagId ? "UPDATE_TAG" : "CREATE_TAG", targetType: "TAG", targetId: editingTagId || null, details: tagName })} className="btn-ghost rounded-full px-4 py-2 text-sm">{editingTagId ? "Update Tag" : "Create Tag"}</button>
          </div>
          <div className="mt-3 max-h-36 space-y-2 overflow-auto pr-1 text-xs">
            {categories.map((entry) => <div key={entry.categoryId} className="rounded-xl border border-white/10 bg-white/5 p-2"><p>{entry.name} {entry.parentCategoryId ? `(Parent: #${entry.parentCategoryId})` : ""}</p><div className="mt-1 flex gap-2"><button onClick={() => { setEditingCategoryId(entry.categoryId); setCategoryForm({ name: entry.name || "", description: entry.description || "", parentCategoryId: entry.parentCategoryId ? String(entry.parentCategoryId) : "" }); }} className="btn-ghost rounded-full px-2 py-1 text-[11px]">Edit</button><button onClick={() => runAction(() => taxonomyService.deleteCategory(entry.categoryId, token), "Category deleted", { action: "DELETE_CATEGORY", targetType: "CATEGORY", targetId: entry.categoryId })} className="rounded-full border border-red-500/70 bg-red-500/20 px-2 py-1 text-[11px] font-semibold text-red-700 dark:text-red-200">Delete</button></div></div>)}
          </div>
          <div className="mt-3 max-h-28 space-y-2 overflow-auto pr-1 text-xs">
            {tags.map((entry) => (
              <div key={entry.tagId} className="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 px-3 py-2">
                <span>#{entry.name}</span>
                <div className="flex gap-2">
                  <button onClick={() => { setEditingTagId(entry.tagId); setTagName(entry.name || ""); }} className="btn-ghost rounded-full px-2 py-0.5 text-[11px]">Edit</button>
                  <button onClick={() => runAction(() => taxonomyService.deleteTag(entry.tagId, token), "Tag deleted", { action: "DELETE_TAG", targetType: "TAG", targetId: entry.tagId })} className="rounded-full border border-red-500/70 bg-red-500/20 px-2 py-0.5 text-[11px] font-semibold text-red-700 dark:text-red-200">Delete</button>
                </div>
              </div>
            ))}
          </div>
          <div className="mt-2 flex flex-wrap gap-2 text-xs">
            {trendingTags.map((entry) => <span key={entry.tagId} className="chip rounded-full px-3 py-1">#{entry.name} ({entry.postCount || 0})</span>)}
          </div>
        </article>

        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Comments</h2>
          <select value={commentStatus} onChange={(e) => setCommentStatus(e.target.value)} className="mt-4 w-full rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="ALL">All</option><option value="PENDING">PENDING</option><option value="APPROVED">APPROVED</option><option value="REJECTED">REJECTED</option></select>
          <div className="mt-3 max-h-[340px] space-y-2 overflow-auto pr-1 text-xs">
            {comments.map((entry) => {
              const normalizedStatus = String(entry.status || "").toUpperCase();
              const postName = postTitleMap[entry.postId] || `Post #${entry.postId}`;
              return (
                <div key={entry.commentId} className="rounded-xl border border-white/10 bg-white/5 p-2">
                  <p className="text-[var(--muted)]">
                    {postName} (#{entry.postId}) | {entry.status}
                  </p>
                  <p className="text-white">{entry.content}</p>
                  <div className="mt-1 flex gap-2">
                    {normalizedStatus !== "APPROVED" && (
                      <button
                        onClick={() =>
                          runAction(
                            () => commentService.approve(entry.commentId, token),
                            "Comment approved",
                            { action: "APPROVE_COMMENT", targetType: "COMMENT", targetId: entry.commentId }
                          )
                        }
                        className="btn-ghost rounded-full px-2 py-1 text-[11px]"
                      >
                        Approve
                      </button>
                    )}
                    {normalizedStatus !== "REJECTED" && (
                      <button
                        onClick={() =>
                          runAction(
                            () => commentService.reject(entry.commentId, token),
                            "Comment rejected",
                            { action: "REJECT_COMMENT", targetType: "COMMENT", targetId: entry.commentId }
                          )
                        }
                        className="btn-ghost rounded-full px-2 py-1 text-[11px]"
                      >
                        Reject
                      </button>
                    )}
                    <button
                      onClick={() =>
                        runAction(
                          () => commentService.delete(entry.commentId, user.userId, token),
                          "Comment deleted",
                          { action: "DELETE_COMMENT", targetType: "COMMENT", targetId: entry.commentId }
                        )
                      }
                      className="rounded-full border border-red-500/70 bg-red-500/20 px-2 py-1 text-[11px] font-semibold text-red-700 dark:text-red-200"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </article>
      </section>

      <section className="mt-6 grid gap-6 lg:grid-cols-2">
        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Newsletter + Broadcast</h2>
          <div className="mt-4 grid gap-2">
            <input value={newsletterSubject} onChange={(e) => setNewsletterSubject(e.target.value)} placeholder="Newsletter subject" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <textarea rows={3} value={newsletterContent} onChange={(e) => setNewsletterContent(e.target.value)} placeholder="Newsletter content" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <select value={newsletterStatus} onChange={(e) => setNewsletterStatus(e.target.value)} className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="ACTIVE">ACTIVE</option><option value="PENDING">PENDING</option><option value="UNSUBSCRIBED">UNSUBSCRIBED</option></select>
            <input value={newsletterPreferences} onChange={(e) => setNewsletterPreferences(e.target.value)} placeholder="Preference CSV (optional)" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <button disabled={busyAction} onClick={() => runAction(() => newsletterService.sendNewsletter({ subject: newsletterSubject, content: newsletterContent, statusFilter: newsletterStatus || undefined, preferenceFilter: newsletterPreferences.split(",").map((v) => v.trim().toLowerCase()).filter(Boolean) || undefined }, token), "Newsletter sent", { action: "SEND_NEWSLETTER", targetType: "NEWSLETTER", details: newsletterSubject })} className="btn-primary rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60">{busyAction ? "Sending..." : "Send Newsletter"}</button>
            <select value={postForNotification} onChange={(e) => setPostForNotification(e.target.value)} className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="">Select post for subscriber notification</option>{posts.map((entry) => <option key={entry.postId} value={entry.postId}>{entry.title}</option>)}</select>
            <button disabled={busyAction} onClick={() => { const selected = posts.find((entry) => entry.postId === Number(postForNotification)); if (!selected) { setError("Select a post first"); notify.info("Select a post first"); return; } runAction(() => newsletterService.sendPostNotification({ postId: selected.postId, title: selected.title, slug: selected.slug, excerpt: selected.excerpt }, token), "Post notification sent", { action: "SEND_POST_NOTIFICATION", targetType: "POST", targetId: selected.postId }); }} className="btn-ghost rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60">{busyAction ? "Sending..." : "Send Post Notification"}</button>
            <p className="text-xs text-[var(--muted)]">Subscribers: {subscribers.length} | Active: {subscriberStats.ACTIVE} | Pending: {subscriberStats.PENDING} | Unsubscribed: {subscriberStats.UNSUBSCRIBED}</p>
            <select value={broadcastRole} onChange={(e) => setBroadcastRole(e.target.value)} className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none"><option value="ALL">All Active Users</option><option value="READER">READER</option><option value="AUTHOR">AUTHOR</option><option value="ADMIN">ADMIN</option></select>
            <input value={broadcastTitle} onChange={(e) => setBroadcastTitle(e.target.value)} placeholder="Broadcast title" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <textarea rows={3} value={broadcastMessage} onChange={(e) => setBroadcastMessage(e.target.value)} placeholder="Broadcast message" className="rounded-2xl border border-white/15 bg-white/5 px-4 py-3 text-sm outline-none" />
            <button disabled={busyAction} onClick={() => { const recipients = (broadcastRole === "ALL" ? allUsers.filter((u) => u.active) : allUsers.filter((u) => u.active && u.role === broadcastRole)).map((u) => u.userId); if (recipients.length === 0) { setError("No recipients found"); notify.info("No recipients found"); return; } runAction(() => notificationService.sendBulk({ recipientIds: recipients, actorId: user.userId, type: "ADMIN_BROADCAST", title: broadcastTitle, message: broadcastMessage || "Platform broadcast", relatedType: "SYSTEM" }, token), "Broadcast sent", { action: "SEND_BROADCAST", targetType: "NOTIFICATION", details: `Recipients: ${recipients.length}` }); }} className="btn-ghost rounded-full px-4 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-60">{busyAction ? "Sending..." : "Send Broadcast"}</button>
          </div>
        </article>

        <article className="glass rounded-3xl p-5 md:p-6">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-xl font-semibold">Analytics + Media</h2>
            <BarChart3 size={18} className="text-[var(--primary)]" />
          </div>

          <div className="mt-4 grid gap-3 sm:grid-cols-3">
            <div className="rounded-2xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-3">
              <p className="text-xs uppercase tracking-[0.08em] text-[var(--muted)]">Active Media</p>
              <p className="mt-1 text-xl font-semibold">{activeMediaCount}</p>
            </div>
            <div className="rounded-2xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-3">
              <p className="text-xs uppercase tracking-[0.08em] text-[var(--muted)]">Deleted Media</p>
              <p className="mt-1 text-xl font-semibold">{deletedMediaCount}</p>
            </div>
            <div className="rounded-2xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-3">
              <p className="text-xs uppercase tracking-[0.08em] text-[var(--muted)]">Authors Tracked</p>
              <p className="mt-1 text-xl font-semibold">{mostActiveAuthors.length}</p>
            </div>
          </div>

          <div className="mt-5">
            <p className="mb-2 inline-flex items-center text-sm font-semibold">
              <FileChartColumn size={14} className="mr-1.5 text-[var(--primary)]" />
              Most viewed posts
            </p>
            <div className="space-y-2">
              {mostViewed.map((entry) => {
                const width = Math.max(8, Math.round(((entry.viewCount || 0) / maxPostViews) * 100));
                return (
                  <div key={entry.postId} className="rounded-xl border border-[var(--line)] bg-[var(--surface-2)]/70 p-2">
                    <div className="flex items-center justify-between gap-2 text-xs">
                      <p className="line-clamp-1">{entry.title}</p>
                      <p className="font-semibold">{entry.viewCount || 0}</p>
                    </div>
                    <div className="mt-1 h-1.5 w-full rounded-full bg-[var(--line)]">
                      <div className="h-1.5 rounded-full bg-gradient-to-r from-blue-500 to-violet-500" style={{ width: `${width}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="mt-5">
            <p className="mb-2 text-sm font-semibold">Most active authors</p>
            <div className="space-y-2 text-xs">
              {mostActiveAuthors.map((entry) => (
                <div key={entry.authorId} className="rounded-xl border border-[var(--line)] bg-[var(--surface-2)]/70 px-3 py-2">
                  <p className="font-medium">{entry.name}</p>
                  <p className="text-[var(--muted)]">{entry.postsCount} posts - {entry.totalViews} views</p>
                </div>
              ))}
            </div>
          </div>

          <div className="mt-5">
            <p className="mb-2 inline-flex items-center text-sm font-semibold">
              <ImageIcon size={14} className="mr-1.5 text-[var(--primary)]" />
              Media library
            </p>
            <div className="max-h-48 space-y-2 overflow-auto pr-1 text-xs">
              {mediaRows.map((entry) => (
                <div key={entry.mediaId} className="rounded-xl border border-[var(--line)] bg-[var(--surface-2)]/70 px-3 py-2">
                  <div className="flex items-center justify-between gap-2">
                    <p className="line-clamp-1">{entry.originalName}</p>
                    <span className={`rounded-full px-2 py-0.5 text-[10px] ${entry.deleted ? "bg-red-500/15 text-red-600 dark:text-red-300" : "bg-emerald-500/15 text-emerald-700 dark:text-emerald-300"}`}>
                      {entry.deleted ? "DELETED" : "ACTIVE"}
                    </span>
                  </div>
                  <button onClick={() => runAction(() => mediaService.delete(entry.mediaId, token), "Media deleted", { action: "DELETE_MEDIA", targetType: "MEDIA", targetId: entry.mediaId })} className="mt-2 rounded-full border border-red-500/70 bg-red-500/20 px-2 py-0.5 text-[11px] font-semibold text-red-700 dark:text-red-200">Delete</button>
                </div>
              ))}
            </div>
          </div>
        </article>
      </section>

      <section className="mt-6">
        <article className="glass mb-6 rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Subscriptions</h2>
          <div className="mt-3 max-h-[220px] space-y-2 overflow-auto pr-1 text-xs">
            {subscriptions.map((entry) => (
              <div key={entry.subscriptionId} className="rounded-xl border border-white/10 bg-white/5 p-2">
                <p className="text-white">
                  User #{entry.userId} | {entry.planType} | {entry.status}
                </p>
                <p className="text-[var(--muted)]">
                  {entry.amountPaise ? `Amount: ${(Number(entry.amountPaise) / 100).toFixed(2)} ${entry.currency || "INR"}` : "Included"}
                </p>
                <p className="text-[var(--muted)]">
                  {entry.startsAt ? `Start: ${formatDate(entry.startsAt)} | ` : ""}
                  {entry.endsAt ? `End: ${formatDate(entry.endsAt)}` : "No expiry"}
                </p>
              </div>
            ))}
            {subscriptions.length === 0 && (
              <p className="text-sm text-[var(--muted)]">No subscription records found.</p>
            )}
          </div>
        </article>

        <article className="glass rounded-3xl p-5 md:p-6">
          <h2 className="text-xl font-semibold">Audit Logs</h2>
          <div className="mt-3 max-h-[260px] space-y-2 overflow-auto pr-1 text-xs">
            {auditLogs.map((entry) => <div key={entry.auditLogId} className="rounded-xl border border-white/10 bg-white/5 p-2"><p className="text-white">[{entry.action}] {entry.targetType || "SYSTEM"} {entry.targetId ? `#${entry.targetId}` : ""}</p><p className="text-[var(--muted)]">by {entry.actorEmail || "unknown"} at {formatDate(entry.createdAt)}</p>{entry.details && <p className="text-[var(--muted)]">{entry.details}</p>}</div>)}
          </div>
        </article>
      </section>

      {authorRequestsOpen && (
        <div className="fixed inset-0 z-[80] flex items-start justify-center bg-slate-950/55 p-4 backdrop-blur-sm">
          <div className="glass edge-glow mt-10 w-full max-w-6xl rounded-3xl p-5 md:p-6">
            <div className="mb-4 flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-semibold">Author Requests</h2>
                <p className="text-xs text-[var(--muted)]">
                  Review reader requests and approve/reject with details.
                </p>
              </div>
              <button
                type="button"
                onClick={() => setAuthorRequestsOpen(false)}
                className="icon-action inline-flex items-center rounded-full p-2"
                aria-label="Close"
              >
                <X size={16} />
              </button>
            </div>

            <div className="mb-4 flex flex-wrap items-center gap-2">
              <select
                value={authorRequestStatusFilter}
                onChange={(event) => setAuthorRequestStatusFilter(event.target.value)}
                className="rounded-2xl border border-white/15 bg-white/5 px-3 py-2 text-sm outline-none"
              >
                <option value="ALL">All</option>
                <option value="PENDING">PENDING</option>
                <option value="APPROVED">APPROVED</option>
                <option value="REJECTED">REJECTED</option>
              </select>
              <button
                type="button"
                onClick={loadAuthorRequests}
                className="btn-ghost rounded-full px-3 py-2 text-xs"
              >
                Refresh
              </button>
            </div>

            {authorRequestsError && (
              <p className="mb-3 rounded-2xl border border-red-300/30 bg-red-500/10 px-4 py-2 text-xs text-red-500">
                {authorRequestsError}
              </p>
            )}

            <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
              <div className="max-h-[62vh] space-y-2 overflow-auto pr-1">
                {authorRequestsLoading && (
                  <p className="text-xs text-[var(--muted)]">Loading requests...</p>
                )}
                {!authorRequestsLoading && authorRequests.length === 0 && (
                  <p className="text-xs text-[var(--muted)]">No requests found.</p>
                )}
                {authorRequests.map((entry) => (
                  <button
                    key={entry.requestId}
                    type="button"
                    onClick={() => loadAuthorRequestDetails(entry.requestId)}
                    className={`w-full rounded-2xl border px-3 py-3 text-left text-xs ${
                      selectedAuthorRequestId === entry.requestId
                        ? "border-cyan-300/60 bg-cyan-500/10"
                        : "border-white/10 bg-white/5"
                    }`}
                  >
                    <p className="font-medium text-[var(--text)]">
                      {entry.fullName || entry.username || `User #${entry.userId}`}
                    </p>
                    <p className="mt-1 text-[var(--muted)]">@{entry.username} | {entry.status}</p>
                    <p className="mt-1 text-[var(--muted)]">{formatDate(entry.createdAt)}</p>
                  </button>
                ))}
              </div>

              <div className="max-h-[62vh] overflow-auto rounded-2xl border border-white/10 bg-white/5 p-4 text-sm">
                {!selectedAuthorRequest && (
                  <p className="text-xs text-[var(--muted)]">
                    Select a request to view full details.
                  </p>
                )}
                {selectedAuthorRequest && (
                  <>
                    <div className="grid gap-2 text-xs md:grid-cols-2">
                      <p><span className="text-[var(--muted)]">Name:</span> {selectedAuthorRequest.fullName || "-"}</p>
                      <p><span className="text-[var(--muted)]">Username:</span> {selectedAuthorRequest.username}</p>
                      <p><span className="text-[var(--muted)]">Email:</span> {selectedAuthorRequest.email}</p>
                      <p><span className="text-[var(--muted)]">User ID:</span> {selectedAuthorRequest.userId}</p>
                      <p><span className="text-[var(--muted)]">Status:</span> {selectedAuthorRequest.status}</p>
                      <p><span className="text-[var(--muted)]">Submitted:</span> {formatDate(selectedAuthorRequest.createdAt)}</p>
                      <p><span className="text-[var(--muted)]">Reviewed By:</span> {selectedAuthorRequest.reviewedByName || "-"}</p>
                      <p><span className="text-[var(--muted)]">Reviewed At:</span> {formatDate(selectedAuthorRequest.reviewedAt)}</p>
                    </div>

                    <div className="mt-4 space-y-3">
                      <div>
                        <p className="text-xs font-semibold">Bio</p>
                        <p className="mt-1 whitespace-pre-wrap rounded-xl border border-white/10 bg-black/20 p-3 text-xs text-white">
                          {selectedAuthorRequest.bio || "-"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs font-semibold">Motivation</p>
                        <p className="mt-1 whitespace-pre-wrap rounded-xl border border-white/10 bg-black/20 p-3 text-xs text-white">
                          {selectedAuthorRequest.motivation || "-"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs font-semibold">Expertise Categories</p>
                        <p className="mt-1 rounded-xl border border-white/10 bg-black/20 p-3 text-xs text-white">
                          {Array.isArray(selectedAuthorRequest.expertiseCategories) &&
                          selectedAuthorRequest.expertiseCategories.length > 0
                            ? selectedAuthorRequest.expertiseCategories.join(", ")
                            : "-"}
                        </p>
                      </div>
                      <div>
                        <p className="text-xs font-semibold">Writing Samples</p>
                        <div className="mt-1 space-y-1 rounded-xl border border-white/10 bg-black/20 p-3 text-xs ">
                          {Array.isArray(selectedAuthorRequest.writingSampleUrls) &&
                          selectedAuthorRequest.writingSampleUrls.length > 0 ? (
                            selectedAuthorRequest.writingSampleUrls.map((url) => (
                              <a
                                key={url}
                                href={url}
                                target="_blank"
                                rel="noreferrer"
                                className="block break-all text-white underline"
                              >
                                {url}
                              </a>
                            ))
                          ) : (
                            <p className="text-[var(--muted)]">-</p>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="mt-4">
                      <label className="mb-1 block text-xs font-semibold">
                        Decision Note (required for reject)
                      </label>
                      <textarea
                        rows={3}
                        value={authorDecisionReason}
                        onChange={(event) => setAuthorDecisionReason(event.target.value)}
                        className="w-full rounded-2xl border border-white/15 bg-white/5 px-3 py-2 text-xs outline-none focus:border-cyan-300/70"
                        placeholder="Add reason"
                      />
                    </div>

                    {String(selectedAuthorRequest.status || "").toUpperCase() === "PENDING" && (
                      <div className="mt-4 flex flex-wrap gap-2">
                        <button
                          type="button"
                          disabled={authorDecisionBusy}
                          onClick={() => decideAuthorRequest(true)}
                          className="btn-primary rounded-full px-4 py-2 text-xs disabled:cursor-not-allowed disabled:opacity-60"
                        >
                          {authorDecisionBusy ? "Processing..." : "Approve"}
                        </button>
                        <button
                          type="button"
                          disabled={authorDecisionBusy}
                          onClick={() => decideAuthorRequest(false)}
                          className="rounded-full border border-orange-500/70 bg-orange-500/20 px-4 py-2 text-xs font-semibold text-orange-700 disabled:cursor-not-allowed disabled:opacity-60 dark:text-orange-200"
                        >
                          {authorDecisionBusy ? "Processing..." : "Reject"}
                        </button>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminPanelPage;
