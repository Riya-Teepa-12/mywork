import { apiRequest } from "./api.js";

function queryString(params = {}) {
  const entries = Object.entries(params).filter(
    ([, value]) => value !== undefined && value !== null && value !== ""
  );
  if (entries.length === 0) {
    return "";
  }
  const search = new URLSearchParams();
  entries.forEach(([key, value]) => search.set(key, String(value)));
  return `?${search.toString()}`;
}

export const authService = {
  getProfile(token) {
    return apiRequest("/auth/profile", { token });
  },
  requestSignupOtp(payload) {
    return apiRequest("/auth/register/request-otp", {
      method: "POST",
      body: JSON.stringify(payload),
      token: false,
      loaderLabel: "Sending OTP email...",
    });
  },
  verifySignupOtp(payload) {
    return apiRequest("/auth/register/verify-otp", {
      method: "POST",
      body: JSON.stringify(payload),
      token: false,
    });
  },
  requestForgotPasswordOtp(email) {
    return apiRequest("/auth/password/forgot/request-otp", {
      method: "POST",
      body: JSON.stringify({ email }),
      token: false,
      loaderLabel: "Sending password reset OTP...",
    });
  },
  resetForgotPassword(payload) {
    return apiRequest("/auth/password/forgot/reset", {
      method: "POST",
      body: JSON.stringify(payload),
      token: false,
      loaderLabel: "Updating password...",
    });
  },
  updateProfile(payload, token) {
    return apiRequest("/auth/profile", {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  becomeAuthor(payload, token) {
    return apiRequest("/auth/profile/become-author", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Upgrading account to author...",
    });
  },
  getMyAuthorRequest(token) {
    return apiRequest("/auth/profile/author-request", {
      token,
      silentErrors: true,
      trackLoader: false,
      showSuccessToast: false,
    });
  },
  getAuthorRequests(filters = {}, token) {
    return apiRequest(`/auth/admin/author-requests${queryString(filters)}`, { token });
  },
  getAuthorRequestById(requestId, token) {
    return apiRequest(`/auth/admin/author-requests/${requestId}`, { token });
  },
  decideAuthorRequest(requestId, payload, token) {
    return apiRequest(`/auth/admin/author-requests/${requestId}/decision`, {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  changePassword(payload, token) {
    return apiRequest("/auth/password", {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  deactivateAccount(token) {
    return apiRequest("/auth/deactivate", { method: "PATCH", token });
  },
  searchUsers(keyword, token) {
    return apiRequest(`/auth/search${queryString({ q: keyword ?? "" })}`, { token });
  },
  getUsers(filters = {}, token) {
    return apiRequest(`/auth/users${queryString(filters)}`, { token });
  },
  countUsers(filters = {}, token) {
    return apiRequest(`/auth/users/count${queryString(filters)}`, { token });
  },
  getUserById(userId, token) {
    return apiRequest(`/auth/users/${userId}`, { token });
  },
  getPublicUser(userId) {
    return apiRequest(`/auth/public/users/${userId}`, { token: false });
  },
  changeUserRole(userId, role, token) {
    return apiRequest(`/auth/users/${userId}/role`, {
      method: "PUT",
      body: JSON.stringify({ role }),
      token,
    });
  },
  suspendUser(userId, token) {
    return apiRequest(`/auth/users/${userId}/suspend`, { method: "PUT", token });
  },
  reactivateUser(userId, token) {
    return apiRequest(`/auth/users/${userId}/reactivate`, { method: "PUT", token });
  },
  deleteUser(userId, token) {
    return apiRequest(`/auth/users/${userId}`, { method: "DELETE", token });
  },
  recordAudit(payload, token) {
    return apiRequest("/auth/admin/audit", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
    });
  },
  getAuditLogs(limit, token) {
    return apiRequest(`/auth/admin/audit${queryString({ limit })}`, { token });
  },
  getSubscriptionPlans() {
    return apiRequest("/auth/subscriptions/plans", { token: false });
  },
  getMySubscriptionEntitlements(token) {
    return apiRequest("/auth/subscriptions/me", { token });
  },
  createSubscriptionOrder(payload, token) {
    return apiRequest("/auth/subscriptions/order", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Creating payment order...",
    });
  },
  verifySubscriptionPayment(payload, token) {
    return apiRequest("/auth/subscriptions/verify", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Verifying payment...",
    });
  },
  getAllSubscriptions(token) {
    return apiRequest("/auth/subscriptions/admin/all", { token });
  },
};

export const postService = {
  getPublished() {
    return apiRequest("/posts/published", { token: false });
  },
  search(keyword) {
    return apiRequest(`/posts/search${queryString({ q: keyword ?? "" })}`, {
      token: false,
    });
  },
  getBySlug(slug) {
    return apiRequest(`/posts/slug/${slug}`, { token: false });
  },
  getById(postId, token) {
    return apiRequest(`/posts/${postId}`, { token });
  },
  getByAuthor(authorId, token) {
    return apiRequest(`/posts/author/${authorId}`, { token });
  },
  getPublishedByAuthor(authorId) {
    return apiRequest(`/posts/author/${authorId}/published`, { token: false });
  },
  getAll(token) {
    return apiRequest("/posts/all", { token });
  },
  create(payload, token) {
    return apiRequest("/posts", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Saving post...",
    });
  },
  update(postId, payload, token) {
    return apiRequest(`/posts/${postId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Updating post...",
    });
  },
  publish(postId, token) {
    return apiRequest(`/posts/${postId}/publish`, {
      method: "PUT",
      token,
      loaderLabel: "Publishing post...",
    });
  },
  unpublish(postId, token) {
    return apiRequest(`/posts/${postId}/unpublish`, { method: "PUT", token });
  },
  feature(postId, featured, token) {
    return apiRequest(`/posts/${postId}/feature${queryString({ featured })}`, {
      method: "PUT",
      token,
    });
  },
  incrementViews(postId, sessionId, token) {
    return apiRequest(`/posts/${postId}/views${queryString({ sessionId })}`, {
      method: "POST",
      token,
      silentErrors: true,
      trackLoader: false,
      showSuccessToast: false,
    });
  },
  like(postId, userId, token) {
    return apiRequest(`/posts/${postId}/like${queryString({ userId })}`, {
      method: "POST",
      token,
    });
  },
  unlike(postId, userId, token) {
    return apiRequest(`/posts/${postId}/unlike${queryString({ userId })}`, {
      method: "POST",
      token,
    });
  },
  delete(postId, token) {
    return apiRequest(`/posts/${postId}`, { method: "DELETE", token });
  },
  count(authorId, token) {
    return apiRequest(`/posts/count${queryString({ authorId })}`, { token });
  },
  getMostViewed(limit, token) {
    return apiRequest(`/posts/most-viewed${queryString({ limit })}`, { token });
  },
  followAuthor(authorId, followerId, token) {
    return apiRequest(`/posts/authors/${authorId}/follow${queryString({ followerId })}`, {
      method: "POST",
      token,
    });
  },
  unfollowAuthor(authorId, followerId, token) {
    return apiRequest(`/posts/authors/${authorId}/follow${queryString({ followerId })}`, {
      method: "DELETE",
      token,
    });
  },
  followerCount(authorId) {
    return apiRequest(`/posts/authors/${authorId}/followers/count`, { token: false });
  },
  isFollowing(authorId, followerId, token) {
    return apiRequest(`/posts/authors/${authorId}/followers/check${queryString({ followerId })}`, {
      token,
    });
  },
  getFollowedAuthors(followerId, token) {
    return apiRequest(`/posts/follows/${followerId}`, { token });
  },
};

export const commentService = {
  getByPost(postId, token) {
    return apiRequest(`/comments/post/${postId}`, { token });
  },
  getAll(status, token) {
    return apiRequest(`/comments/all${queryString({ status })}`, { token });
  },
  getById(commentId, token) {
    return apiRequest(`/comments/${commentId}`, { token });
  },
  getReplies(commentId, token) {
    return apiRequest(`/comments/${commentId}/replies`, { token });
  },
  add(payload, token) {
    return apiRequest("/comments", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
    });
  },
  update(commentId, payload, token) {
    return apiRequest(`/comments/${commentId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  delete(commentId, actorId, token) {
    return apiRequest(`/comments/${commentId}${queryString({ actorId })}`, {
      method: "DELETE",
      token,
    });
  },
  approve(commentId, token) {
    return apiRequest(`/comments/${commentId}/approve`, { method: "PUT", token });
  },
  reject(commentId, token) {
    return apiRequest(`/comments/${commentId}/reject`, { method: "PUT", token });
  },
  like(commentId, userId, token) {
    return apiRequest(`/comments/${commentId}/like${queryString({ userId })}`, {
      method: "PUT",
      token,
    });
  },
  unlike(commentId, userId, token) {
    return apiRequest(`/comments/${commentId}/unlike${queryString({ userId })}`, {
      method: "PUT",
      token,
    });
  },
  count(postId, token) {
    return apiRequest(`/comments/count${queryString({ postId })}`, {
      token: false,
      silentErrors: true,
      trackLoader: false,
    });
  },
  getModerationMode(token) {
    return apiRequest("/comments/moderation", { token });
  },
  setModerationMode(moderationRequired, token) {
    return apiRequest("/comments/moderation", {
      method: "PUT",
      body: JSON.stringify({ moderationRequired }),
      token,
    });
  },
};

export const taxonomyService = {
  getCategories() {
    return apiRequest("/categories", { token: false });
  },
  getCategoryBySlug(slug) {
    return apiRequest(`/categories/${slug}`, { token: false });
  },
  createCategory(payload, token) {
    return apiRequest("/categories", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
    });
  },
  updateCategory(categoryId, payload, token) {
    return apiRequest(`/categories/${categoryId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  deleteCategory(categoryId, token) {
    return apiRequest(`/categories/${categoryId}`, { method: "DELETE", token });
  },
  getTags() {
    return apiRequest("/tags", { token: false });
  },
  getTagBySlug(slug) {
    return apiRequest(`/tags/${slug}`, { token: false });
  },
  createTag(payload, token) {
    return apiRequest("/tags", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
    });
  },
  updateTag(tagId, payload, token) {
    return apiRequest(`/tags/${tagId}`, {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  deleteTag(tagId, token) {
    return apiRequest(`/tags/${tagId}`, { method: "DELETE", token });
  },
  getTagsByPost(postId) {
    return apiRequest(`/tags/post/${postId}`, { token: false });
  },
  getCategoriesByPost(postId) {
    return apiRequest(`/categories/post/${postId}`, { token: false });
  },
  addTagToPost(postId, tagId, token) {
    return apiRequest("/tags/post", {
      method: "POST",
      body: JSON.stringify({ postId, taxonomyId: tagId }),
      token,
    });
  },
  removeTagFromPost(postId, tagId, token) {
    return apiRequest("/tags/post", {
      method: "DELETE",
      body: JSON.stringify({ postId, taxonomyId: tagId }),
      token,
    });
  },
  addCategoryToPost(postId, categoryId, token) {
    return apiRequest("/categories/post", {
      method: "POST",
      body: JSON.stringify({ postId, taxonomyId: categoryId }),
      token,
    });
  },
  removeCategoryFromPost(postId, categoryId, token) {
    return apiRequest("/categories/post", {
      method: "DELETE",
      body: JSON.stringify({ postId, taxonomyId: categoryId }),
      token,
    });
  },
  getTrendingTags() {
    return apiRequest("/tags/trending", { token: false });
  },
};

export const mediaService = {
  upload(file, uploaderId, altText, token) {
    const body = new FormData();
    body.set("file", file);
    body.set("uploaderId", String(uploaderId));
    if (altText) {
      body.set("altText", altText);
    }
    return apiRequest("/media", {
      method: "POST",
      body,
      token,
      isFormData: true,
      loaderLabel: "Uploading media...",
    });
  },
  getById(mediaId, token) {
    return apiRequest(`/media/${mediaId}`, { token });
  },
  getByUploader(uploaderId, token) {
    return apiRequest(`/media/uploader/${uploaderId}`, { token });
  },
  getByPost(postId, token) {
    return apiRequest(`/media/post/${postId}`, { token });
  },
  getAll(includeDeleted, token) {
    return apiRequest(`/media/all${queryString({ includeDeleted })}`, { token });
  },
  updateAltText(mediaId, altText, token) {
    return apiRequest(`/media/${mediaId}/alt-text`, {
      method: "PUT",
      body: JSON.stringify({ altText }),
      token,
    });
  },
  linkToPost(mediaId, postId, token) {
    return apiRequest("/media/link", {
      method: "POST",
      body: JSON.stringify({ mediaId, postId }),
      token,
    });
  },
  unlinkFromPost(mediaId, token) {
    return apiRequest(`/media/${mediaId}/unlink`, { method: "POST", token });
  },
  delete(mediaId, token) {
    return apiRequest(`/media/${mediaId}`, { method: "DELETE", token });
  },
  cleanupDeleted(token) {
    return apiRequest("/media/cleanup", { method: "DELETE", token });
  },
};

export const newsletterService = {
    getMe(token) {
    return apiRequest("/newsletter/me", { token }).then((res) => {
      if (res?.subscribed === true) return res.data || null;
      if (res?.subscribed === false) return null;
      return res || null;
    });
  },
  subscribe(payload, token) {
    return apiRequest("/newsletter/subscribe", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Creating newsletter subscription...",
    });
  },
  confirm(tokenValue) {
    return apiRequest(`/newsletter/confirm${queryString({ token: tokenValue })}`, {
      token: false,
    });
  },
  unsubscribe(tokenValue) {
    return apiRequest(`/newsletter/unsubscribe${queryString({ token: tokenValue })}`, {
      token: false,
    });
  },
  getAll(token) {
    return apiRequest("/newsletter/all", { token });
  },
  sendNewsletter(payload, token) {
    return apiRequest("/newsletter/send-newsletter", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Sending newsletter campaign...",
    });
  },
  sendPostNotification(payload, token) {
    return apiRequest("/newsletter/send-post-notification", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Sending new post emails...",
    });
  },
  updatePreferences(payload, token) {
    return apiRequest("/newsletter/preferences", {
      method: "PUT",
      body: JSON.stringify(payload),
      token,
    });
  },
  sendWelcome(email, token) {
    return apiRequest(`/newsletter/send-welcome${queryString({ email })}`, {
      method: "POST",
      token,
      loaderLabel: "Sending welcome email...",
    });
  },
  count(status, token) {
    return apiRequest(`/newsletter/count${queryString({ status })}`, { token });
  },
};

export const notificationService = {
  send(payload, token) {
    return apiRequest("/notifications", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
    });
  },
  sendBulk(payload, token) {
    return apiRequest("/notifications/bulk", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
    });
  },
  sendEmail(payload, token) {
    return apiRequest("/notifications/email", {
      method: "POST",
      body: JSON.stringify(payload),
      token,
      loaderLabel: "Sending email notification...",
    });
  },
  getByRecipient(recipientId, token) {
    return apiRequest(`/notifications/recipient/${recipientId}`, { token });
  },
  markRead(notificationId, token) {
    return apiRequest(`/notifications/${notificationId}/read`, {
      method: "PUT",
      token,
    });
  },
  markAllRead(recipientId, token) {
    return apiRequest(`/notifications/read-all${queryString({ recipientId })}`, {
      method: "PUT",
      token,
    });
  },
  deleteRead(recipientId, token) {
    return apiRequest(`/notifications/read${queryString({ recipientId })}`, {
      method: "DELETE",
      token,
    });
  },
  unreadCount(recipientId, token) {
    return apiRequest(`/notifications/unread-count${queryString({ recipientId })}`, {
      token,
    });
  },
  delete(notificationId, token) {
    return apiRequest(`/notifications/${notificationId}`, {
      method: "DELETE",
      token,
    });
  },
  getAll(token) {
    return apiRequest("/notifications/all", { token });
  },
};
