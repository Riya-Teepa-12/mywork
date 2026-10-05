# Inkwell

Inkwell is a full-stack, microservices-based blogging platform with authentication, role-based access control, content publishing, comments, likes, notifications, newsletter delivery, and an admin panel.

The repository combines a **Spring Boot backend ecosystem**, a **React/Vite frontend**, and supporting design documentation. It is structured as a production-style platform rather than a single monolith, with separate services for identity, posts, comments, taxonomy, media, newsletters, notifications, and service discovery.

---

## 🎭 User Roles

| Role   | Access                                                                 |
|--------|------------------------------------------------------------------------|
| Guest  | Browse published posts and read full articles.                         |
| Reader | Log in, like posts, comment, manage profile, subscribe to newsletters, receive notifications, request author access. |
| Author | Create, edit, draft, publish, unpublish, manage posts/media/comments.  |
| Admin  | Full access: user management, moderation, taxonomy, newsletters, analytics, audit logs. |

---

## ✨ Key Features

- Guest browsing of published posts only.
- Reader authentication with profile management and author-upgrade flow.
- Author post creation with rich-text editing, media upload, categories, tags, featured images, SEO-friendly slugs.
- Post interactions: likes, threaded comments, replies, view counts, moderation.
- Notification delivery through Kafka-backed event flow and email.
- Newsletter functionality with subscription, confirmation, unsubscribe, campaigns, and Razorpay-backed payment flow.
- OAuth login with Google and GitHub.
- Email verification and login-related email flows through SMTP.
- Admin dashboard access for all users, content, taxonomy, media, notifications, and audits.
- Microservices architecture with service discovery and gateway routing.

---

## 🏗️ Architecture

The system follows a **gateway-centric microservices model**.

### Request Flow
- Frontend sends all requests to the API Gateway.
- Gateway validates JWTs and forwards user context headers.
- Services discovered through Eureka.
- Persistent data stored in MySQL.
- Redis used for hot counters and cached state.
- Kafka used for async notification events.
- SMTP used for mail delivery and verification flows.
- Media stored locally or in S3-compatible object storage.

---

## 📦 Services in this Repo

| Module             | Responsibility                                                                 |
|--------------------|---------------------------------------------------------------------------------|
| service-registry   | Eureka service discovery                                                       |
| api-gateway        | Entry point, routing, JWT validation, header propagation                       |
| Auth-service       | Authentication, profile management, OAuth, author-upgrade, roles, subscriptions |
| post-service       | Post lifecycle, publishing, view counts, likes, follows, notifications         |
| comment-service    | Comments, replies, likes, moderation                                           |
| category-service   | Categories, tags, taxonomy mapping                                             |
| media-service      | Media upload, metadata updates, post linking                                   |
| newsletter-service | Newsletter subscription, confirmation, campaigns, post-publish email behavior  |
| notification-service | In-app/email notifications, unread tracking, Kafka consumption               |
| inkwell-frontend   | User-facing web app built with React + Vite                                    |

---

## 📖 Role Capabilities

### Guest
- Browse published posts
- Open posts by slug
- Filter by category/tag
- Search posts by keyword
- View author profiles

### Reader
- Register/login (email/password, Google OAuth, GitHub OAuth)
- Like/unlike posts
- Comment/reply on posts
- Like comments
- Receive notifications
- Subscribe/unsubscribe newsletters
- Update profile
- Request author access

### Author
- Create/edit/draft/publish/unpublish posts
- Upload/manage media
- Assign categories/tags/featured images
- Moderate comments on own posts
- View engagement metrics
- Manage media library

### Admin
- Manage users/roles
- Suspend/reactivate/delete accounts
- Moderate posts/comments
- Manage taxonomy
- Manage newsletter subscribers
- Send broadcast notifications
- Review analytics/audit logs
- Manage site-wide media
- Oversee subscription/payment flows

---

## 🗂️ Core Domain Model

- Users and roles  
- Posts and post lifecycle  
- Comments and comment likes  
- Categories and tags  
- Media entities  
- Newsletter subscribers  
- Notifications  
- Author upgrade requests  
- Payment orders and subscription records  

---

## 📑 Design Documents

- `SDD.md`
- `docs/SDD_Evaluation.md`
- `docs/diagrams/architecture.svg`
- `docs/diagrams/er.svg`
- `docs/diagrams/class.svg`
- `docs/diagrams/sequence-login.svg`
- `docs/diagrams/sequence-post-comment.svg`

---

## ⚙️ Tech Stack

### Frontend
- React  
- Vite  
- React Router  
- Tailwind CSS  
- TipTap editor  

### Backend
- Java 17  
- Spring Boot 3.2.x  
- Spring Web  
- Spring Security  
- Spring Data JPA  
- Spring Cloud Gateway  
- Eureka  
- Spring Kafka  
- Spring Mail  
- Springdoc OpenAPI  

### Data & Infrastructure
- MySQL  
- H2 (local/dev)  
- Redis  
- Kafka  
- SMTP  
- S3-compatible storage  
- Razorpay  

### Build & Quality
- Maven multi-module build  
- JaCoCo coverage  
- SonarQube integration  
- Docker Compose for local infra  

---

## 🔐 Authentication & Access Control

- JWT-based access control  
- Email/password login  
- Google OAuth  
- GitHub OAuth  
- SMTP-driven email verification  

---

## 🔔 Notifications

- Event-driven (Kafka)  
- In-app + email delivery  
- Notification service persists alerts  
- Readers/authors receive activity alerts  

---

## 📬 Newsletter & Payments

- Subscribe / Confirm / Unsubscribe  
- Campaign delivery  
- Post-publish email dispatch  
- Razorpay-backed subscription/payment flows  

---

## 📝 Post Lifecycle

- Draft → Published → Unpublished → Archived  
- SEO-friendly slugs  
- Rich-text formatting  
- Featured images  
- Category/tag mapping  
- View count tracking  
- Likes & comments  

---

## 🚀 Getting Started

### Requirements
- JDK 17+  
- Maven 3.9+  
- Node.js (frontend)  
- MySQL, Redis, Kafka  
- SMTP credentials  
- Google/GitHub OAuth credentials  
- Razorpay credentials  
- Optional S3 storage credentials  

### Build Backend
```bash
mvn clean install
