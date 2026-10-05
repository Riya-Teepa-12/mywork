<div align="center">
  <img src="https://raw.githubusercontent.com/tandpfun/skill-icons/main/icons/TailwindCSS-Dark.svg" alt="Tailwind CSS" width="50" height="50" />
  
  <br />
  <br />

  <h1 align="center">🖋️ Inkwell Platform Frontend</h1>

  <p align="center">
    <strong>A next-generation publishing platform crafted for modern writers and readers.</strong>
    <br />
    Responsive, lightning-fast, and thoughtfully designed.
  </p>
  
  <p align="center">
    <a href="https://github.com/Riya-Teepa-12/Blogging-platform-frontend/issues">Report Bug</a>
    ·
    <a href="https://github.com/Riya-Teepa-12/Blogging-platform-frontend/issues">Request Feature</a>
  </p>

  <p align="center">
    <img src="https://img.shields.io/github/license/Riya-Teepa-12/Blogging-platform-frontend?style=flat-square&color=5D5DFF" alt="License" />
    <img src="https://img.shields.io/github/stars/Riya-Teepa-12/Blogging-platform-frontend?style=flat-square&color=5D5DFF" alt="Stars" />
    <img src="https://img.shields.io/github/forks/Riya-Teepa-12/Blogging-platform-frontend?style=flat-square&color=5D5DFF" alt="Forks" />
    <img src="https://img.shields.io/github/issues/Riya-Teepa-12/Blogging-platform-frontend?style=flat-square&color=5D5DFF" alt="Issues" />
  </p>
</div>

## 🌟 Overview

Inkwell is a comprehensive, microservices-backed blogging frontend that delivers a premium publishing experience. Designed with a mobile-first philosophy, the application seamlessly handles complex workflows—from sophisticated rich-text editing to robust administration dashboards—all while maintaining a fluid, native-feeling user interface.

## ✨ Platform Features

### 👥 Multi-Role Ecosystem
- **Readers**: Personalized feeds, reading history, bookmarks, and subscriptions.
- **Authors**: Advanced rich-text editor (Tiptap), draft management, analytics dashboard, and profile customization.
- **Administrators**: Comprehensive admin panel for content moderation, user suspension, and platform analytics.

### 🔐 Security & Identity
- Secure JWT-based authentication flow.
- Seamless OAuth integration (Google/GitHub).
- Protected routing and session management.
- Forgot password and secure reset workflows.

### 📝 The Publishing Experience
- **WYSIWYG Editor**: Distraction-free writing powered by Tiptap.
- **Media Management**: Effortless image uploads and cover photo integration.
- **Engagement**: Comments, claps/likes, and sharing mechanisms.

### 📨 Newsletters & Notifications
- Built-in newsletter subscription system.
- Real-time application notifications for engagement and updates.

## 🛠️ Technology Stack

Our stack is carefully chosen to provide optimal developer experience and unparalleled user performance.

| Category | Technology | Description |
| :--- | :--- | :--- |
| **Core** | [React 19](https://react.dev/) | UI Library |
| **Build Tool** | [Vite](https://vitejs.dev/) | Next-generation frontend tooling |
| **Routing** | [React Router 7](https://reactrouter.com/) | Declarative routing |
| **Styling** | [Tailwind CSS](https://tailwindcss.com/) | Utility-first CSS framework |
| **Animation** | [Framer Motion](https://www.framer.com/motion/) | Production-ready animation library |
| **Editor** | [Tiptap](https://tiptap.dev/) | Headless wrapper around ProseMirror |
| **Icons** | [Lucide](https://lucide.dev/) | Beautiful & consistent icon toolkit |

## 🚀 Getting Started

### Prerequisites

- **Node.js** (v18.x or higher)
- **npm** (v9.x or higher) or **yarn**

### Local Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/Riya-Teepa-12/Blogging-platform-frontend.git
   cd Blogging-platform-frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure Environment Variables**
   Duplicate the example environment file and update the variables to point to your backend services.
   ```bash
   cp .env.example .env
   ```

4. **Launch the development server**
   ```bash
   npm run dev
   ```
   *The application will boot up at `http://localhost:5173`.*

## 📂 Architecture overview

```text
src/
├── components/       # Modular, reusable UI components (Buttons, Modals, Navbar)
├── context/          # Global state management (AuthContext, ThemeContext)
├── pages/            # View-level components mapped to routes
│   ├── AdminPanelPage.jsx
│   ├── AuthorDashboardPage.jsx
│   ├── FeedPage.jsx
│   ├── PostPage.jsx
│   └── ...
├── lib/              # API interceptors and utility functions
├── data/             # Mock services and static configuration
└── styles.css        # Global CSS and Tailwind directives
```

## 📜 Available Commands

- `npm run dev` - Starts the Vite dev server with HMR.
- `npm run build` - Transpiles and minifies the application for production.
- `npm run preview` - Serves the production build locally for testing.


## 📄 License

This project is licensed under the MIT License.

<div align="center">
  <br />
  <sub>Built with precision by the Inkwell Team.</sub>
</div>
