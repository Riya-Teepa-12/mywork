# Inkwell Frontend File-by-File Document

This document explains the **Inkwell React frontend** in a simple teacher-style way so you can understand how frontend works internally, file by file, concept by concept, and flow by flow.

> Note: This guide is based on the frontend files currently visible in the workspace. For very large files like `AdminPanelPage.jsx`, the explanation is organized by logical line ranges and blocks so it stays readable while still being line-by-line in spirit.

---

## 1) What the frontend does in Inkwell

The frontend is the part the user sees in the browser.

In Inkwell it does all of this:
- shows pages
- collects user input
- calls backend APIs
- stores login session in browser
- shows loading and error messages
- protects private pages
- manages author/admin dashboards
- displays notifications
- handles OAuth redirects
- updates UI automatically when data changes

The frontend is built with **React**, so the UI is component-based and state-driven.

---

## 2) Main frontend architecture

### Important layers

1. **`App.jsx`**
   - root route controller
   - decides which page opens for which URL
   - controls public/private/admin routes
   - shows navbar and footer conditionally

2. **`AuthContext.jsx`**
   - stores token, user, role, login status
   - handles login/logout/profile refresh
   - syncs auth data with `localStorage`

3. **`NotificationContext.jsx`**
   - shows success/info/error toasts
   - helps every page display messages consistently

4. **`api.js`**
   - common API request wrapper
   - adds JWT token
   - parses JSON
   - handles success/error/loader events

5. **`services.js`**
   - organized domain-wise API functions
   - `authService`, `postService`, `commentService`, `mediaService`, `taxonomyService`, `newsletterService`, `notificationService`

6. **Pages**
   - `LoginPage.jsx`, `SignupPage.jsx`, `HomePage.jsx`, `FeedPage.jsx`, `PostPage.jsx`, `AuthorDashboardPage.jsx`, `AdminPanelPage.jsx`, `ProfilePage.jsx`, `NotificationsPage.jsx`, `NewsletterPage.jsx`, `SubscriptionsPage.jsx`, `ForgotPasswordPage.jsx`, `OAuthCallbackPage.jsx`, `AuthorProfilePage.jsx`, `SuspendedAccountPage.jsx`, `NotFoundPage.jsx`

7. **Shared components**
   - `Navbar.jsx`
   - `Footer.jsx`
   - `ProtectedRoute.jsx`
   - `RichTextEditor.jsx`

---

## 3) React concepts used in Inkwell frontend

### `useState`
Used to store local data inside a component.
Example: form input, loading flag, error message, selected post, selected category.

### `useEffect`
Used for side effects.
Example: fetch data after page loads, show notifications, sync `localStorage`.

### `useContext`
Used to access shared state from `AuthContext` and `NotificationContext`.

### `useNavigate`
Used for programmatic route changes after login/logout or when access is restricted.

### `useLocation`
Used to read current route information and route state messages.

### `useSearchParams`
Used to read query parameters from the URL, such as OAuth errors.

### `useMemo`
Used to optimize derived values like counts, maps, and statistics.

### `useRef`
Used for direct DOM or component access, such as scrolling to a form or controlling the editor.

### `motion` and `AnimatePresence`
Used for page transition animations.

### Controlled components
Inputs are controlled by React state.
That means the input value comes from state and updates state on change.

### Conditional rendering
UI is shown only when a condition is true.
Example: show error message only if `error` exists.

### Route protection
`ProtectedRoute` and `PublicOnlyRoute` control access to pages.

---

## 4) File-by-file overview

### 4.1 `src/App.jsx`
Purpose:
- defines all routes
- decides public/protected/admin routes
- shows navbar/footer conditionally
- blocks suspended users

### 4.2 `src/context/AuthContext.jsx`
Purpose:
- global auth state manager
- login/logout/session persistence
- role and suspension detection

### 4.3 `src/context/NotificationContext.jsx`
Purpose:
- global message/toast system
- success/info/error notifications

### 4.4 `src/lib/api.js`
Purpose:
- common request wrapper around `fetch`
- adds token automatically
- centralizes loader and notification events

### 4.5 `src/lib/services.js`
Purpose:
- grouped API service methods
- keeps pages clean and readable

### 4.6 `src/pages/LoginPage.jsx`
Purpose:
- login form
- OAuth login buttons
- redirects after success
- displays messages and errors

### 4.7 `src/pages/AdminPanelPage.jsx`
Purpose:
- full admin control center
- user, post, taxonomy, comment, newsletter, subscription, audit and author-request management

### 4.8 `src/pages/AuthorDashboardPage.jsx`
Purpose:
- author content creation and moderation dashboard

### 4.9 `src/components/ProtectedRoute.jsx`
Purpose:
- route guard for authenticated and role-based access

### 4.10 `src/components/Navbar.jsx`
Purpose:
- top navigation
- role-based menu display

### 4.11 `src/components/Footer.jsx`
Purpose:
- bottom footer area

### 4.12 `src/components/RichTextEditor.jsx`
Purpose:
- editor for writing formatted posts

---

# 5) Line-by-line explanation: `src/App.jsx`

File purpose:
`App.jsx` is the root page router of the frontend.

---

## Imports

```javascriptreact
import { Route, Routes, useLocation } from "react-router-dom";
```
- `Route` and `Routes` create routing.
- `useLocation` reads current route path.

```javascriptreact
import { AnimatePresence, motion } from "framer-motion";
```
- used for page transition animations.

```javascriptreact
import Navbar from "./components/Navbar.jsx";
import Footer from "./components/Footer.jsx";
```
- layout components.

```javascriptreact
import HomePage from "./pages/HomePage.jsx";
import FeedPage from "./pages/FeedPage.jsx";
import PostPage from "./pages/PostPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import SignupPage from "./pages/SignupPage.jsx";
import ForgotPasswordPage from "./pages/ForgotPasswordPage.jsx";
import OAuthCallbackPage from "./pages/OAuthCallbackPage.jsx";
import AuthorDashboardPage from "./pages/AuthorDashboardPage.jsx";
import AuthorProfilePage from "./pages/AuthorProfilePage.jsx";
import AdminPanelPage from "./pages/AdminPanelPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";
import NotificationsPage from "./pages/NotificationsPage.jsx";
import NewsletterPage from "./pages/NewsletterPage.jsx";
import SubscriptionsPage from "./pages/SubscriptionsPage.jsx";
import NotFoundPage from "./pages/NotFoundPage.jsx";
import SuspendedAccountPage from "./pages/SuspendedAccountPage.jsx";
```
- all page components used in routing.

```javascriptreact
import ProtectedRoute, { PublicOnlyRoute } from "./components/ProtectedRoute.jsx";
```
- route guard components.

```javascriptreact
import { useAuth } from "./context/AuthContext.jsx";
```
- reads auth status globally.

---

## Component start

```javascriptreact
function App() {
```
- root React component.

---

## Read current URL

```javascriptreact
const location = useLocation();
```
- gives current browser path.
- needed for routing animation and route logic.

---

## Read auth status

```javascriptreact
const { isAuthenticated, isSuspended } = useAuth();
```
- gets login state and suspended state.

---

## Detect auth-related pages

```javascriptreact
const authRoute =
  location.pathname === "/login" ||
  location.pathname === "/signup" ||
  location.pathname === "/oauth/callback" ||
  location.pathname === "/forgot-password" ||
  location.pathname === "/suspended";
```
- checks whether current page is authentication-related.
- used to hide navbar/footer on those pages.

---

## Suspended user protection

```javascriptreact
if (isAuthenticated && isSuspended) {
  return <SuspendedAccountPage />;
}
```
- if user is logged in but suspended, show suspended page directly.
- this blocks access to the rest of the app.

---

## Main layout wrapper

```javascriptreact
return (
  <div className="relative min-h-screen bg-[var(--surface)] text-[var(--text)] transition-colors duration-500">
```
- outer app shell.
- sets full-screen layout and theme colors.

```javascriptreact
<div className="ambient-bg" />
```
- decorative background layer.

```javascriptreact
{!authRoute && <Navbar />}
```
- show navbar only on non-auth pages.

```javascriptreact
<main className={authRoute ? "pt-0" : "pt-24"}>
```
- add top padding when navbar is present.

---

## Animation wrapper

```javascriptreact
<AnimatePresence mode="wait">
```
- animate page exit/entry transitions.

```javascriptreact
<motion.div
  key={location.pathname}
  initial={{ opacity: 0, y: 24 }}
  animate={{ opacity: 1, y: 0 }}
  exit={{ opacity: 0, y: -16 }}
  transition={{ duration: 0.4, ease: "easeOut" }}
>
```
- animates page changes when URL changes.
- `key={location.pathname}` ensures page animation runs per route.

---

## Routes

```javascriptreact
<Routes location={location}>
```
- all route definitions.

### Public routes
```javascriptreact
<Route path="/" element={<HomePage />} />
<Route path="/feed" element={<FeedPage />} />
<Route path="/post/:slug" element={<PostPage />} />
<Route path="/authors/:authorId" element={<AuthorProfilePage />} />
<Route path="/suspended" element={<SuspendedAccountPage />} />
```
- accessible without login.

### Public-only route group
```javascriptreact
<Route element={<PublicOnlyRoute />}>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/signup" element={<SignupPage />} />
  <Route path="/forgot-password" element={<ForgotPasswordPage />} />
  <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
</Route>
```
- only for users who are not already logged in.

### Protected route group
```javascriptreact
<Route element={<ProtectedRoute />}>
  <Route path="/profile" element={<ProfilePage />} />
  <Route path="/notifications" element={<NotificationsPage />} />
  <Route path="/newsletter" element={<NewsletterPage />} />
  <Route path="/subscriptions" element={<SubscriptionsPage />} />
</Route>
```
- only for authenticated users.

### Author/Admin route
```javascriptreact
<Route element={<ProtectedRoute allowedRoles={["AUTHOR", "ADMIN"]} />}>
  <Route path="/author" element={<AuthorDashboardPage />} />
</Route>
```
- only author or admin can access.

### Admin-only route
```javascriptreact
<Route element={<ProtectedRoute allowedRoles={["ADMIN"]} />}>
  <Route path="/admin" element={<AdminPanelPage />} />
</Route>
```
- only admin can access.

### Unknown route
```javascriptreact
<Route path="*" element={<NotFoundPage />} />
```
- fallback route.

---

## Footer

```javascriptreact
{!authRoute && <Footer />}
```
- show footer only on non-auth pages.

---

# 6) Line-by-line explanation: `src/context/AuthContext.jsx`

File purpose:
This file manages the **global login session**.

It stores:
- token
- user
- role
- authentication status
- suspended status

---

## Imports

```javascriptreact
import {
  createContext,
  useContext,
  useEffect,
  useState,
  startTransition,
} from "react";
```
- `createContext` creates a shared state container.
- `useContext` reads shared state.
- `useEffect` handles side effects.
- `useState` stores local state.
- `startTransition` updates state without blocking UI.

```javascriptreact
import { apiRequest } from "../lib/api.js";
```
- common backend request helper.

---

## Create context

```javascriptreact
const AuthContext = createContext(null);
```
- creates auth context.
- default value is `null`.

```javascriptreact
const AUTH_STORAGE_KEY = "inkwell_auth";
```
- localStorage key for saving auth data.

---

## Suspended user helper

```javascriptreact
function isSuspendedUser(user) {
```
- helper function to check account status.

```javascriptreact
if (!user) {
  return false;
}
```
- if user does not exist, not suspended.

```javascriptreact
if (typeof user.active === "boolean" && user.active === false) {
  return true;
}
```
- if backend explicitly says active=false, user is suspended.

```javascriptreact
const status = String(user.status || user.accountStatus || "").toUpperCase();
return status === "SUSPENDED";
```
- also checks string status fields.
- returns true if status is suspended.

---

## Read auth from localStorage

```javascriptreact
function readStoredAuth() {
```
- helper to load saved auth data.

```javascriptreact
try {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY);
```
- tries to read saved token/user JSON.

```javascriptreact
if (!raw) {
  return { token: null, user: null };
}
```
- if nothing stored, return empty auth.

```javascriptreact
const data = JSON.parse(raw);
return { token: data.token ?? null, user: data.user ?? null };
```
- parse JSON and return values.

```javascriptreact
} catch {
  return { token: null, user: null };
}
```
- safe fallback if parsing fails.

---

## Provider component

```javascriptreact
export function AuthProvider({ children }) {
```
- wraps the app and shares auth data to all children.

```javascriptreact
const [{ token, user }, setAuth] = useState(readStoredAuth);
```
- state contains token and user.
- initial value is loaded from localStorage.

---

## Sync auth to localStorage

```javascriptreact
useEffect(() => {
```
- runs whenever token/user changes.

```javascriptreact
if (!token || !user) {
  localStorage.removeItem(AUTH_STORAGE_KEY);
  return;
}
```
- if no token or no user, clear storage.

```javascriptreact
localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ token, user }));
```
- save auth data to browser storage.

```javascriptreact
}, [token, user]);
```
- re-run only when token/user change.

---

## Refresh profile effect

```javascriptreact
useEffect(() => {
```
- runs when token changes.

```javascriptreact
let active = true;
```
- flag to avoid updating state after unmount.

```javascriptreact
if (!token) {
  return () => {
    active = false;
  };
}
```
- if no token, do nothing.

```javascriptreact
apiRequest("/auth/profile", { token })
```
- fetch current profile from backend.

```javascriptreact
.then((profile) => {
  if (!active) {
    return;
  }
```
- if component still mounted, continue.

```javascriptreact
startTransition(() => {
  setAuth((prev) => ({ ...prev, user: profile }));
});
```
- update user profile without blocking UI.

```javascriptreact
.catch(() => {
  if (!active) {
    return;
  }
  startTransition(() => {
    setAuth({ token: null, user: null });
  });
});
```
- if profile fetch fails, log out automatically.

```javascriptreact
return () => {
  active = false;
};
```
- cleanup on unmount.

```javascriptreact
}, [token]);
```
- run again when token changes.

---

## Login function

```javascriptreact
const login = async (credentials) => {
```
- function called by login page.

```javascriptreact
const result = await apiRequest("/auth/login", {
  method: "POST",
  body: JSON.stringify(credentials),
  loaderLabel: "Logging in...",
});
```
- sends login request.

```javascriptreact
startTransition(() => {
  setAuth({ token: result.accessToken, user: result.user });
});
```
- store returned JWT and user profile.

```javascriptreact
return result;
```
- return backend response to caller.

---

## Signup function

```javascriptreact
const signup = async (payload) => {
  return apiRequest("/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
    loaderLabel: "Creating account...",
  });
};
```
- calls registration endpoint.

---

## Login with token

```javascriptreact
const loginWithToken = async (accessToken) => {
```
- used after OAuth login or token-based redirect.

```javascriptreact
if (!accessToken) {
  throw new Error("Token is required");
}
```
- token must exist.

```javascriptreact
const profile = await apiRequest("/auth/profile", { token: accessToken });
```
- fetch profile using token.

```javascriptreact
startTransition(() => {
  setAuth({ token: accessToken, user: profile });
});
```
- store token and user.

---

## Logout function

```javascriptreact
const logout = () => {
```
- clears auth session.

```javascriptreact
if (token) {
  apiRequest("/auth/logout", { method: "POST", token, trackLoader: false }).catch(() => null);
}
```
- inform backend about logout.

```javascriptreact
startTransition(() => {
  setAuth({ token: null, user: null });
});
```
- clear frontend auth state.

---

## Refresh profile function

```javascriptreact
const refreshProfile = async () => {
```
- manually reload profile.

```javascriptreact
if (!token) {
  return null;
}
```
- cannot refresh without token.

```javascriptreact
const profile = await apiRequest("/auth/profile", { token });
```
- fetch latest profile.

```javascriptreact
startTransition(() => {
  setAuth((prev) => ({ ...prev, user: profile }));
});
```
- update only the user part.

---

## Context value

```javascriptreact
const value = {
  token,
  user,
  role: user?.role || null,
  isAuthenticated: Boolean(token && user),
  isSuspended: isSuspendedUser(user),
  login,
  loginWithToken,
  signup,
  logout,
  refreshProfile,
};
```
- exposes everything other components need.

---

## Provider return

```javascriptreact
return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
```
- gives auth data to all children.

---

## Custom hook

```javascriptreact
export function useAuth() {
```
- helper hook to use auth context.

```javascriptreact
const value = useContext(AuthContext);
```
- read auth context data.

```javascriptreact
if (!value) {
  throw new Error("useAuth must be used within AuthProvider");
}
```
- prevents misuse outside provider.

```javascriptreact
return value;
```
- return auth state and actions.

---

# 7) Line-by-line explanation: `src/pages/LoginPage.jsx`

File purpose:
Login screen for users.

## Main concepts used
- form state
- notifications
- route messages
- OAuth redirects
- navigation after success

### Important idea
This page does not call backend directly. It calls `login()` from `AuthContext`.

### Main flow
1. user types email/password
2. page stores values in state
3. submit calls auth login
4. success → redirect to home
5. error → show message, maybe redirect to suspended page
6. OAuth buttons redirect to auth-service

---

# 8) Line-by-line explanation: `src/pages/AdminPanelPage.jsx`

File purpose:
This is the **most powerful admin page**. It manages users, posts, categories, tags, comments, newsletters, broadcasts, subscriptions, media, audit logs, and author requests.

Because the file is large, the explanation is grouped by functional blocks, which is the most readable line-by-line style for a real project of this size.

---

## Block 1: Imports and helpers

### Imports
- `useEffect` → load data after render
- `useMemo` → derived calculations
- `useState` → page states
- icon components → UI icons
- service modules → backend API methods
- `useAuth()` → token and current user
- `useNotification()` → toasts/messages

### Helper
```javascriptreact
function formatDate(value) {
  return value ? new Date(value).toLocaleString() : "";
}
```
- converts date to readable string.

---

## Block 2: Component start and auth access

```javascriptreact
function AdminPanelPage() {
```
- starts admin page component.

```javascriptreact
const { token, user } = useAuth();
```
- read logged-in user and token.

```javascriptreact
const notify = useNotification();
```
- show messages.

---

## Block 3: State declarations

This page stores many datasets because admin controls many features.

### Data state
- `users`
- `allUsers`
- `posts`
- `mostViewed`
- `comments`
- `categories`
- `tags`
- `trendingTags`
- `subscribers`
- `subscriptions`
- `mediaRows`
- `auditLogs`
- `counts`

### Filter/search state
- `searchText`
- `roleFilter`
- `activeFilter`
- `commentStatus`

### Category/tag form state
- `categoryForm`
- `editingCategoryId`
- `tagName`
- `editingTagId`

### Newsletter/broadcast state
- `newsletterSubject`
- `newsletterContent`
- `newsletterStatus`
- `newsletterPreferences`
- `postForNotification`
- `broadcastRole`
- `broadcastTitle`
- `broadcastMessage`

### UI state
- `error`
- `loading`
- `busyAction`

### Author request modal state
- `authorRequestsOpen`
- `authorRequestStatusFilter`
- `authorRequests`
- `authorRequestsLoading`
- `authorRequestsError`
- `selectedAuthorRequestId`
- `selectedAuthorRequest`
- `authorDecisionReason`
- `authorDecisionBusy`

### Why so many states?
Because the admin panel combines multiple management modules into one dashboard.

---

## Block 4: Derived maps with `useMemo`

### `userMap`
Maps userId to user object.
Used to quickly find names.

### `postTitleMap`
Maps postId to post title.
Used in comments section.

### `mostActiveAuthors`
Calculates authors by posts and views.
This is derived from the `posts` list.

### `subscriberStats`
Counts ACTIVE / PENDING / UNSUBSCRIBED subscribers.

### `activeMediaCount` and `deletedMediaCount`
Count media entries by deleted status.

### `maxPostViews`
Used to calculate bar widths in analytics.

### Why `useMemo`?
To avoid recalculating these values on every render unless input data changes.

---

## Block 5: Main data loader

```javascriptreact
const load = async () => {
```
- loads all admin dashboard data.

This function calls many backend services in parallel using `Promise.all`:
- `authService.getUsers(...)`
- `authService.getUsers({})`
- `postService.getAll(...)`
- `postService.getMostViewed(...)`
- `commentService.getAll(...)`
- `taxonomyService.getCategories()`
- `taxonomyService.getTags()`
- `taxonomyService.getTrendingTags()`
- `newsletterService.getAll(...)`
- `authService.getAllSubscriptions(...)`
- `mediaService.getAll(...)`
- `authService.getAuditLogs(...)`
- `authService.countUsers(...)`
- `postService.count(...)`
- `commentService.count(...)`

### Why parallel loading?
Because admin panel needs a lot of data and `Promise.all` is faster than waiting one by one.

### After data loads
Each response is stored in the related state.
Then `counts` are updated.

---

## Block 6: Initial effect for loading dashboard

```javascriptreact
useEffect(() => {
```
- runs when page opens or filters/token change.

```javascriptreact
let active = true;
```
- prevents setting state after unmount.

```javascriptreact
setLoading(true);
setError("");
```
- show loading and clear old errors.

```javascriptreact
load()
  .catch(...)
  .finally(...)
```
- load data, handle error, stop loading.

```javascriptreact
return () => {
  active = false;
};
```
- cleanup on unmount.

### Dependencies
`[token, searchText, roleFilter, activeFilter, commentStatus]`
- page reloads when filters or auth changes.

---

## Block 7: Author request loaders

### `loadAuthorRequests`
- loads author requests filtered by status.
- updates selected request if list changes.

### `loadAuthorRequestDetails`
- loads details of one request.
- sets reason text and selected request.

### `useEffect` for modal
When author request modal opens, it loads requests.
This keeps data fresh.

---

## Block 8: Author request decision

### `decideAuthorRequest(approve)`
- approves or rejects author request.
- if rejecting, reason is required.
- sends payload to `authService.decideAuthorRequest(...)`
- refreshes data after success
- shows success/error toasts

### Why important?
This is a real admin moderation workflow.

---

## Block 9: Audit helper

### `audit(payload)`
- records admin action using `authService.recordAudit(...)`
- failure is ignored because audit should not block the main action

---

## Block 10: Shared action wrapper

### `runAction(action, message, auditPayload)`
This is a very important helper.

It:
- prevents duplicate clicks using `busyAction`
- clears old errors
- runs the action
- logs audit if needed
- shows success toast
- reloads page data
- shows error if something fails

### Why this is good design?
Because many admin buttons do the same pattern.
This avoids repeating the same try/catch logic everywhere.

---

## Block 11: Header and stats cards

The top section shows:
- title
- author requests button
- summary cards for users/posts/comments/media

These are dashboard KPI cards.

---

## Block 12: Users section

This section lets admin:
- search users
- filter by role
- filter by status
- change role
- suspend user
- reactivate user
- delete user

### Important pattern
Each button uses `runAction(...)` and a backend call from `authService`.

Examples:
- `changeUserRole`
- `suspendUser`
- `reactivateUser`
- `deleteUser`

### Why?
Admin needs direct control of account lifecycle.

---

## Block 13: Posts section

This section lets admin:
- feature/unfeature post
- publish post
- unpublish post
- delete post

### Why?
Admin moderates and controls content visibility.

### Business meaning
This is platform-level content control.

---

## Block 14: Categories & Tags section

This section lets admin:
- create category
- update category
- delete category
- create tag
- update tag
- delete tag
- choose parent category
- view trending tags

### Why?
This supports content organization and taxonomy.

### Important React pattern
Form values are stored using `useState` and updated with `onChange`.

---

## Block 15: Comments section

This section lets admin:
- filter comments by status
- approve comment
- reject comment
- delete comment

It also uses `postTitleMap` to show which post each comment belongs to.

### Why?
This is moderation flow for platform quality.

---

## Block 16: Newsletter + Broadcast section

This section lets admin:
- send newsletter
- select newsletter status filter
- optionally filter by preferences
- send post notification
- send platform broadcast notifications

### Why?
It supports communication with readers and authors.

### Important admin actions here
- `newsletterService.sendNewsletter(...)`
- `newsletterService.sendPostNotification(...)`
- `notificationService.sendBulk(...)`

### Subscriber stats
Shows ACTIVE, PENDING, UNSUBSCRIBED counts.

---

## Block 17: Analytics + Media section

This section shows:
- active media count
- deleted media count
- tracked authors count
- most viewed posts bars
- most active authors list
- media library with delete button

### Why?
Admins get a quick operational view of platform activity.

---

## Block 18: Subscriptions section

This section lists subscription records.
It shows:
- user id
- plan type
- status
- amount
- currency
- start time
- end time

### Why?
Admins can monitor paid access.

---

## Block 19: Audit logs section

This shows all recorded admin actions.
Each entry shows:
- action
- target type
- target id
- actor email
- created time
- optional details

### Why?
This is important for accountability and traceability.

---

## Block 20: Author Requests modal

This modal opens when admin clicks Author Requests.

It includes:
- request filters
- request list
- request details
- biography
- motivation
- expertise categories
- writing sample URLs
- decision note
- approve/reject buttons

### Why?
It allows manual review of users applying to become authors.

---

# 9) File-by-file frontend summary table

| File | Purpose | Main concept |
|---|---|---|
| `App.jsx` | route control | routing, protection, animations |
| `AuthContext.jsx` | global auth state | context, localStorage, JWT session |
| `NotificationContext.jsx` | toast system | global notifications |
| `api.js` | common API wrapper | fetch abstraction, token headers |
| `services.js` | organized API methods | service layer |
| `LoginPage.jsx` | login UI | controlled forms, OAuth redirect |
| `SignupPage.jsx` | registration UI | form submit, messaging |
| `AdminPanelPage.jsx` | admin dashboard | large dashboard orchestration |
| `AuthorDashboardPage.jsx` | author tools | content/editor/media/moderation |
| `ProtectedRoute.jsx` | access control | guarded routing |
| `Navbar.jsx` | top navigation | role-based UI |
| `Footer.jsx` | page footer | layout |
| `RichTextEditor.jsx` | post editor | editor integration |
| `HomePage.jsx` | landing page | public UI |
| `FeedPage.jsx` | published posts feed | browsing |
| `PostPage.jsx` | single post page | reading + interactions |
| `ProfilePage.jsx` | user profile | account details |
| `NotificationsPage.jsx` | notification center | read/unread state |
| `NewsletterPage.jsx` | newsletter flow | subscription preference |
| `SubscriptionsPage.jsx` | payment/subscription | Razorpay checkout |
| `OAuthCallbackPage.jsx` | OAuth return page | token login |
| `SuspendedAccountPage.jsx` | blocked access page | suspension handling |
| `NotFoundPage.jsx` | 404 page | fallback route |

---

# 10) 2-minute frontend presentation script (short version)

You can say this in a presentation:

> The Inkwell frontend is built in React and acts as the user-facing layer of the application. The root file `App.jsx` controls routing, page transitions, and route protection. Authentication is managed globally in `AuthContext.jsx`, where token and user data are stored and synced with localStorage. API calls are centralized in `api.js`, and domain-wise service methods are grouped in `services.js`.
>
> The login page uses controlled components, notifications, and OAuth redirects to authenticate the user. The author dashboard allows post creation, media upload, and comment moderation, while the admin panel provides full platform control over users, posts, categories, tags, comments, newsletters, subscriptions, media, audit logs, and author requests. Overall, the frontend is designed with reusable components, shared state, protected routes, and a clean service layer so the UI stays organized, scalable, and easy to maintain.

---

# 11) Final teaching summary

If you want to understand the frontend deeply, remember these 5 ideas:

1. **React components build the UI**
2. **State stores data**
3. **Effects run side tasks like API calls**
4. **Context shares global data**
5. **Routes decide which page opens**

That is the main skeleton of your frontend.

---

# 12) What to study first

If you are learning from zero, study in this order:
1. `App.jsx`
2. `AuthContext.jsx`
3. `api.js`
4. `services.js`
5. `LoginPage.jsx`
6. `AuthorDashboardPage.jsx`
7. `AdminPanelPage.jsx`
8. `ProtectedRoute.jsx`
9. `NotificationContext.jsx`
10. remaining pages

---

If you want, I can now create a **second version of this document as a cleaner PDF-style HTML file with styling**, or I can make a **version with only interview-presentation notes**.

