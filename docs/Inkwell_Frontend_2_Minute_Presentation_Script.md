# Inkwell Frontend 2-Minute Presentation Script

Good morning everyone.

Today I will explain the **frontend of my Inkwell project**.

The frontend is built using **React**, and it is the part that the user sees in the browser. Its main job is to show pages, take user input, call backend APIs, and update the screen dynamically when data changes.

The root file of the frontend is **`App.jsx`**. This file controls all the routes of the application. It decides which page should open for each URL, and it also protects private routes like profile, notifications, newsletter, subscriptions, author dashboard, and admin panel. It also hides the navbar and footer on login and signup pages.

The most important part of frontend state management is **`AuthContext.jsx`**. This file stores the JWT token, current user, role, authentication status, and suspended status. It also handles login, logout, signup, profile refresh, and localStorage sync. So the frontend remembers the user session even after page reload.

For backend communication, the frontend uses a common API wrapper in **`api.js`**, and domain-wise API methods are grouped in **`services.js`**. This keeps the code clean and avoids repeating fetch logic in every page.

The **`LoginPage.jsx`** handles email-password login and OAuth login with Google and GitHub. It uses React hooks like `useState`, `useEffect`, `useNavigate`, and `useSearchParams` to manage form input, show messages, and redirect after login.

The **`AdminPanelPage.jsx`** is the most powerful page in the frontend. It allows the admin to manage users, posts, categories, tags, comments, newsletters, broadcasts, media, subscriptions, audit logs, and author requests. It acts like the control center of the whole platform.

The frontend also uses components like `Navbar`, `Footer`, `ProtectedRoute`, and `RichTextEditor` to make the UI reusable and organized.

Overall, the Inkwell frontend is designed with a modular React structure: routes in `App.jsx`, global auth in `AuthContext.jsx`, API handling in `api.js`, feature-wise services in `services.js`, and different pages for login, author actions, and admin operations.

Thank you.

