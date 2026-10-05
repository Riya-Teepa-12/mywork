import {
  authService,
  postService,
  commentService,
  taxonomyService,
  mediaService,
  newsletterService,
  notificationService,
} from "../lib/services.js";
import { apiRequest } from "../lib/api.js";

jest.mock("../lib/api.js", () => ({
  apiRequest: jest.fn(),
}));

describe("services wrappers", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    apiRequest.mockResolvedValue({ ok: true });
  });

  it("builds authService.getAuthorRequests query params correctly", async () => {
    await authService.getAuthorRequests({ status: "PENDING", page: 2 }, "token-1");

    expect(apiRequest).toHaveBeenCalledWith(
      "/auth/admin/author-requests?status=PENDING&page=2",
      { token: "token-1" }
    );
  });

  it("builds postService.search with keyword", async () => {
    await postService.search("react");

    expect(apiRequest).toHaveBeenCalledWith("/posts/search?q=react", { token: false });
  });

  it("covers query-string fallback branches", async () => {
    await postService.search(undefined);
    await authService.getUsers({}, "t");
    await authService.countUsers({ role: null, status: "" }, "t");
    await authService.getAuditLogs(undefined, "t");
    await authService.searchUsers(undefined, "t");
    await postService.incrementViews(8, undefined, "t");
    await authService.getAuthorRequestById(10, "t");

    expect(apiRequest).toHaveBeenCalledWith("/posts/search", { token: false });
  });

  it("passes newsletter subscribe payload and loader label", async () => {
    const payload = { email: "a@b.com", userId: 9 };
    await newsletterService.subscribe(payload, "token-2");

    expect(apiRequest).toHaveBeenCalledWith("/newsletter/subscribe", {
      method: "POST",
      body: JSON.stringify(payload),
      token: "token-2",
      loaderLabel: "Creating newsletter subscription...",
    });
  });

  it("covers common auth and post wrappers", async () => {
    await authService.getProfile("t");
    await authService.requestSignupOtp({ email: "a@b.com" });
    await authService.verifySignupOtp({ code: "123456" });
    await authService.requestForgotPasswordOtp("a@b.com");
    await authService.resetForgotPassword({ email: "a@b.com", password: "x", otp: "1" });
    await authService.updateProfile({ fullName: "A" }, "t");
    await authService.becomeAuthor({ penName: "A" }, "t");
    await authService.getMyAuthorRequest("t");
    await authService.decideAuthorRequest(9, { approved: true }, "t");
    await authService.changePassword({ oldPassword: "a", newPassword: "b" }, "t");
    await authService.deactivateAccount("t");
    await authService.searchUsers("john", "t");
    await authService.getUsers({ page: 1 }, "t");
    await authService.countUsers({ role: "READER" }, "t");
    await authService.getUserById(4, "t");
    await authService.getPublicUser(4);
    await authService.changeUserRole(4, "AUTHOR", "t");
    await authService.suspendUser(4, "t");
    await authService.reactivateUser(4, "t");
    await authService.deleteUser(4, "t");
    await authService.recordAudit({ action: "x" }, "t");
    await authService.getAuditLogs(20, "t");
    await authService.getSubscriptionPlans();
    await authService.getMySubscriptionEntitlements("t");
    await authService.createSubscriptionOrder({ planId: "pro" }, "t");
    await authService.verifySubscriptionPayment({ paymentId: "1" }, "t");
    await authService.getAllSubscriptions("t");

    await postService.getPublished();
    await postService.getBySlug("slug");
    await postService.getById(8, "t");
    await postService.getByAuthor(3, "t");
    await postService.getPublishedByAuthor(3);
    await postService.getAll("t");
    await postService.create({ title: "T" }, "t");
    await postService.update(8, { title: "T2" }, "t");
    await postService.publish(8, "t");
    await postService.unpublish(8, "t");
    await postService.feature(8, true, "t");
    await postService.incrementViews(8, "sess1", "t");
    await postService.like(8, 2, "t");
    await postService.unlike(8, 2, "t");
    await postService.delete(8, "t");
    await postService.count(3, "t");
    await postService.getMostViewed(5, "t");
    await postService.followAuthor(3, 2, "t");
    await postService.unfollowAuthor(3, 2, "t");
    await postService.followerCount(3);
    await postService.isFollowing(3, 2, "t");
    await postService.getFollowedAuthors(2, "t");

    expect(apiRequest).toHaveBeenCalled();
  });

  it("covers comments, taxonomy, media, newsletter and notification wrappers", async () => {
    await commentService.getByPost(1, "t");
    await commentService.getAll("PENDING", "t");
    await commentService.getById(1, "t");
    await commentService.getReplies(1, "t");
    await commentService.add({ postId: 1, text: "x" }, "t");
    await commentService.update(1, { text: "y" }, "t");
    await commentService.delete(1, 2, "t");
    await commentService.approve(1, "t");
    await commentService.reject(1, "t");
    await commentService.like(1, 2, "t");
    await commentService.unlike(1, 2, "t");
    await commentService.count(1, "t");
    await commentService.getModerationMode("t");
    await commentService.setModerationMode(true, "t");

    await taxonomyService.getCategories();
    await taxonomyService.getCategoryBySlug("tech");
    await taxonomyService.createCategory({ name: "Tech" }, "t");
    await taxonomyService.updateCategory(2, { name: "Tech2" }, "t");
    await taxonomyService.deleteCategory(2, "t");
    await taxonomyService.getTags();
    await taxonomyService.getTagBySlug("react");
    await taxonomyService.createTag({ name: "React" }, "t");
    await taxonomyService.updateTag(2, { name: "React2" }, "t");
    await taxonomyService.deleteTag(2, "t");
    await taxonomyService.getTagsByPost(9);
    await taxonomyService.getCategoriesByPost(9);
    await taxonomyService.addTagToPost(9, 2, "t");
    await taxonomyService.removeTagFromPost(9, 2, "t");
    await taxonomyService.addCategoryToPost(9, 2, "t");
    await taxonomyService.removeCategoryFromPost(9, 2, "t");
    await taxonomyService.getTrendingTags();

    await mediaService.getById(3, "t");
    await mediaService.getByUploader(2, "t");
    await mediaService.getByPost(9, "t");
    await mediaService.getAll(true, "t");
    await mediaService.updateAltText(3, "alt", "t");
    await mediaService.linkToPost(3, 9, "t");
    await mediaService.unlinkFromPost(3, "t");
    await mediaService.delete(3, "t");
    await mediaService.cleanupDeleted("t");

    const file = new File(["x"], "x.txt", { type: "text/plain" });
    await mediaService.upload(file, 2, "alt", "t");
    await mediaService.upload(file, 2, "", "t");

    await newsletterService.getMe("t");
    await newsletterService.confirm("abc");
    await newsletterService.unsubscribe("abc");
    await newsletterService.getAll("t");
    await newsletterService.sendNewsletter({ subject: "s" }, "t");
    await newsletterService.sendPostNotification({ postId: 1 }, "t");
    await newsletterService.updatePreferences({ weeklyDigest: true }, "t");
    await newsletterService.sendWelcome("a@b.com", "t");
    await newsletterService.count("ACTIVE", "t");

    await notificationService.send({ recipientId: 2, message: "x" }, "t");
    await notificationService.sendBulk({ recipientIds: [1, 2], message: "x" }, "t");
    await notificationService.sendEmail({ to: "a@b.com", subject: "s" }, "t");
    await notificationService.getByRecipient(2, "t");
    await notificationService.markRead(4, "t");
    await notificationService.markAllRead(2, "t");
    await notificationService.deleteRead(2, "t");
    await notificationService.unreadCount(2, "t");
    await notificationService.delete(4, "t");
    await notificationService.getAll("t");

    expect(apiRequest).toHaveBeenCalled();
  });
});
