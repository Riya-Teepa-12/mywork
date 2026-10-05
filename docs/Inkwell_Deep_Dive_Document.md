# Inkwell Project Deep-Dive Guide

> PDF-ready study document for the **Inkwell / Inkwell blogging platform**.
>
> This guide is based on the code and workspace context currently visible in the repository. Where exact files were not fully opened, the explanation follows the project structure and implementation style already present in the codebase.

---

## 1. Complete High-Level Architecture

Inkwell is a **microservices-based blogging platform** built with:

- **React frontend** for user interface
- **Spring Boot microservices** for domain logic
- **API Gateway** as the single entry point
- **Eureka Server** for service discovery
- **JWT + Spring Security** for authentication and authorization
- **OAuth2** for social login
- **MySQL + JPA** for persistence
- **Kafka** for async event flow
- **Razorpay** for payment/subscription flow
- **Docker + AWS EC2** for deployment

### Overall runtime flow

```text
React Frontend
  ↓
API Gateway
  ↓
JWT / Security Filter
  ↓
Target Microservice
  ↓
Controller
  ↓
ServiceImpl
  ↓
Repository
  ↓
MySQL / Redis / Kafka / Mail
  ↓
Response
  ↓
React state update
  ↓
UI update
```

### Why this architecture is used

- Each domain is isolated and independently deployable.
- The gateway hides internal service URLs from the frontend.
- Eureka allows dynamic service discovery.
- JWT makes authentication stateless.
- Kafka reduces tight coupling for notifications and other async tasks.
- MySQL stores durable business data.
- Redis improves read performance for counters/cache use cases.

---

## 2. Service-by-Service Deep Dive

---

# 2.1 Eureka Server

## Purpose of this service

### Why it exists
Eureka Server is the **service registry**. All backend services register themselves here so other services can find them by name instead of hardcoding IP/port.

### Business problem it solves
- avoids hardcoded service URLs
- supports scaling and container redeployments
- enables load-balanced discovery-based routing
- makes microservices manageable

### Which services talk to it
- API Gateway
- Auth Service
- Post Service
- Comment Service
- Category Service
- Notification Service
- Newsletter Service
- Media Service

### Which frontend pages use it
Frontend does **not** call Eureka directly. It is used indirectly through the gateway and backend service lookup.

## Important files
- `application.yml` / `application.properties`
- Main application class with `@EnableEurekaServer`
- Dependency setup in `pom.xml`

## Code explanation
When you paste Eureka code, explain:
- package and imports
- `@SpringBootApplication`
- `@EnableEurekaServer`
- main method
- server port and registry config

## Annotation explanation
- `@EnableEurekaServer` → marks the app as Eureka registry server.

## API explanation
Typical Eureka endpoints are internal and managed by Spring Cloud Netflix.

## Frontend-backend connection
Frontend does not connect to Eureka. Gateway and services use it internally.

## Database flow
Eureka does not use business tables like MySQL; it stores registry metadata in memory.

## Business logic
- service registration
- service discovery
- health/instance tracking

## Flow diagram
```text
Service Startup
  ↓
Registers itself in Eureka
  ↓
Gateway / services query Eureka
  ↓
Resolved service instance used for routing
```

## Interview prep
### 5-line summary
Eureka Server is the service registry of the platform. It stores the network location of each microservice. Gateway and services use it to find one another dynamically. It removes hardcoded dependency on port numbers. It is foundational for microservice discovery.

### Common interview questions
1. Why do we need Eureka?
2. What happens if Eureka is down?
3. How do services register themselves?
4. Why use service discovery instead of hardcoded URLs?
5. Is Eureka mandatory in microservices?

---

# 2.2 API Gateway

## Purpose of this service

### Why it exists
The API Gateway is the single public entry point for the frontend.

### Business problem it solves
- hides backend service topology
- centralizes security and routing
- simplifies frontend API calls
- supports CORS and auth token forwarding

### Which services it talks to
- Eureka Server
- Auth Service
- Post Service
- Comment Service
- Category Service
- Notification Service
- Newsletter Service
- Media Service

### Which frontend pages use it
All pages use it indirectly via `lib/api.js`.

## Important files
- Gateway `application.yml`
- Route configuration
- JWT filter / auth filter
- CORS config
- security config

## Code explanation
When you paste gateway code, explain:
- route predicates and URI mapping
- filter chain
- token validation logic
- header propagation

## Annotation explanation
- `@Configuration` → defines Java config class
- `@Bean` → registers route/filter beans
- `@Value` → injects external config

## API explanation
Gateway maps public paths like `/auth/**`, `/posts/**`, `/comments/**` to respective services.

## Frontend-backend connection
```text
React component
  ↓
lib/api.js
  ↓
fetch() / axios
  ↓
API Gateway
  ↓
JWT filter validates Authorization header
  ↓
Request forwarded to target service
```

## Security explanation
Gateway often reads:
- `Authorization: Bearer <token>`
- extracts user id / role
- forwards headers like `X-User-Id`, `X-User-Role`, `X-User-Email`

## Error handling
If token invalid or route unauthorized, gateway returns `401`/`403`.

## Logging
Gateway logs request paths, routing decisions, and auth failures.

## Interview prep
### 5-line summary
API Gateway is the entry point for the frontend. It routes traffic to backend microservices. It handles security and header forwarding. It reduces client complexity. It centralizes cross-cutting concerns.

### Common interview questions
1. Why use API Gateway?
2. What is the difference between gateway and Eureka?
3. How does it help frontend development?
4. What happens on invalid JWT?
5. Why centralize security at gateway?

---

# 2.3 Auth Service

## Purpose of this service

### Why it exists
Auth Service handles identity and access management.

### Business problem it solves
- user login/register
- OTP verification
- forgot/reset password
- JWT generation and validation
- OAuth2 login
- role and entitlement checks

### Which services it talks to
- API Gateway
- Newsletter Service (internal entitlement check)
- Post Service and others through gateway headers
- Frontend login/signup/forgot password/OAuth pages

### Which frontend pages use it
- `LoginPage`
- `SignupPage`
- `ForgotPasswordPage`
- `OAuthCallbackPage`
- profile/account pages

## Important files
- `AuthServiceApplication.java`
- `SecurityConfig.java`
- controllers such as `AuthController`
- service classes
- repositories for users, OTP, subscriptions, logs
- DTOs for login/register/reset/refresh
- security filters and handlers

### Security config example from workspace
`SecurityConfig.java` shows:
- `@Configuration`
- `@EnableMethodSecurity`
- `SecurityFilterChain`
- `JwtAuthenticationFilter`
- OAuth2 success/failure handlers
- public endpoints for register/login/refresh/OTP/reset/internal entitlement lookup

## Code explanation
When you paste code, explain:
- auth flow
- password encoding
- JWT generation
- filter order
- endpoint protection

## Annotation explanation
- `@EnableMethodSecurity` → enables method-level security annotations like `@PreAuthorize`
- `@Configuration` → Spring config class
- `@Bean` → registers security filter chain
- `@RequestMatcher` usage → path-based access rules

## API explanation
Common auth endpoints:
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `GET /auth/profile`
- `POST /auth/logout`
- OTP and password reset endpoints

Each endpoint returns JWT/user profile or action status.

## Frontend-backend connection
```text
LoginPage / SignupPage
  ↓
useAuth() / apiRequest()
  ↓
Gateway
  ↓
Auth controller
  ↓
Auth service
  ↓
MySQL tables for user/OTP/reset
  ↓
JWT / response
  ↓
AuthContext stores token/user
```

## Database flow
Likely entities:
- `User`
- `EmailOtp`
- `PasswordResetToken`
- `AuditLog`
- subscription-related entities

## Business logic
- login/register/JWT
- OTP verification
- password reset
- OAuth2 login success/failure
- role checks for admin/author/reader

## Security explanation
- `Authorization` header carries JWT
- `JwtAuthenticationFilter` reads token
- role/claims are attached to request context
- internal endpoints may use special header like internal API key

## Logging
- login success/failure
- suspicious or invalid token attempts
- account state changes

## Interview prep
### 5-line summary
Auth Service is the identity center of Inkwell. It manages users, JWT, OTP, reset password, and OAuth2 login. It enforces roles and permissions. Other services depend on it for identity context. It is the security backbone of the platform.

### Common interview questions
1. Why is JWT used?
2. How does OAuth2 fit into auth service?
3. How does the JWT filter work?
4. Why use `@EnableMethodSecurity`?
5. How does logout work in a JWT app?

---

# 2.4 Post Service

## Purpose of this service

### Why it exists
Post Service manages the core content lifecycle of the blogging platform.

### Business problem it solves
- create/edit/publish/unpublish/delete posts
- track views and likes
- author follow graph
- search and featured posts
- post ownership enforcement

### Which services it talks to
- Auth Service for identity headers
- Category Service for taxonomy mapping
- Comment Service for comments/moderation views
- Media Service for featured media and linked assets
- Notification Service for publish/comment side effects

### Which frontend pages use it
- Home feed
- Post detail page
- Author dashboard
- Admin panel

## Important files
- `PostResource.java`
- `PostService`
- `PostServiceImpl`
- repositories for post likes, follows, etc.
- DTOs: `PostCreateRequest`, `PostUpdateRequest`, `PostResponse`
- `GlobalExceptionHandler.java`

### Exception handling example
`GlobalExceptionHandler` catches:
- `IllegalArgumentException`
- `MethodArgumentNotValidException`

and returns consistent JSON errors.

## Code explanation
From `PostResource.java`:
- create/update/delete methods check `X-User-Id` and `X-User-Role`
- role-based checks ensure only author/admin can manage posts
- only owners can modify their own posts unless admin
- published posts can be visible to others

## Annotation explanation
- `@RestController` → REST JSON controller
- `@RequestMapping("/posts")` → base URL
- `@RequestHeader` → reads forwarded identity from gateway
- `@PathVariable` → reads URL path data
- `@Valid` → triggers validation on request DTO
- `@RestControllerAdvice` → global error handling
- `@ExceptionHandler` → maps exceptions to JSON response

## API explanation
Core endpoints visible in code:
- `POST /posts`
- `GET /posts/{postId}`
- `GET /posts/slug/{slug}`
- `GET /posts/author/{authorId}`
- `GET /posts/published`
- `PUT /posts/{postId}`
- `PUT /posts/{postId}/publish`
- `PUT /posts/{postId}/unpublish`
- `DELETE /posts/{postId}`
- `POST /posts/{postId}/views`
- `POST /posts/{postId}/like`
- `POST /posts/{postId}/unlike`

### Frontend flow example
`AuthorDashboardPage.jsx` uses:
- `postService.getByAuthor(user.userId, token)`
- `postService.create(...)`
- `postService.update(...)`
- `postService.publish(...)`
- `postService.delete(...)`

## Frontend-backend connection
```text
AuthorDashboardPage
  ↓
postService in lib/services.js
  ↓
apiRequest()
  ↓
Gateway
  ↓
JWT filter adds X-User-Id / X-User-Role
  ↓
PostResource
  ↓
PostServiceImpl
  ↓
PostRepository and related JPA repos
  ↓
MySQL
  ↓
Response
  ↓
React state update and dashboard refresh
```

## Database flow
Typical entities:
- `Post`
- `PostLike`
- `AuthorFollow`

Fields usually include:
- post id
- author id
- title
- slug
- content
- excerpt
- status
- featured flag
- view/like counts

## Business logic
- author or admin only can create or publish
- owner check before update/delete
- view count increments with session id
- like/unlike only by authenticated self
- search/published/featured filtering

## Logging
- content state changes
- publish/unpublish/delete events
- like/view counters

## Interview prep
### 5-line summary
Post Service is the blogging core of Inkwell. It manages content creation, publishing, editing, and deletion. It also handles views, likes, and author relationships. It uses role checks to protect owner actions. It is one of the most important business services.

### Common interview questions
1. How do you prevent non-owners from editing posts?
2. How do you count views safely?
3. Why separate published and draft posts?
4. What is the role of `slug`?
5. How do likes and follows work?

---

# 2.5 Comment Service

## Purpose of this service

### Why it exists
Comment Service manages user discussions under posts.

### Business problem it solves
- add comments and replies
- moderate pending comments
- approve/reject/delete comments
- keep comments separate from post content

### Which services it talks to
- Auth Service for identity/role
- Post Service for post reference
- Notification Service for comment events

### Which frontend pages use it
- Post detail page
- Author dashboard moderation panel

## Important files
- controller for comments
- service and repository
- comment entity
- DTOs for create/update/respond
- exception handling

## Business logic
- pending moderation flow
- parent comment support for replies
- owner/admin delete logic

## Interview prep
### 5-line summary
Comment Service powers discussion and moderation on posts. It stores replies and approvals separately from posts. It supports moderation workflow. It helps maintain platform quality. It integrates with identity and notifications.

### Common interview questions
1. Why keep comment moderation separate?
2. How do you model replies?
3. Who can approve comments?
4. How do you delete comments safely?
5. How do notifications trigger from comments?

---

# 2.6 Category Service

## Purpose of this service

### Why it exists
Category Service manages taxonomy: categories and tags.

### Business problem it solves
- organize posts
- support filtering/searching
- attach categories/tags to posts

### Which services it talks to
- Post Service
- Frontend admin/author pages

### Which frontend pages use it
- Author dashboard
- Admin/category management pages

## Important files
- controller
- service and repository
- category/tag entity and DTOs
- global exception handler

## Business logic
- category CRUD
- tag CRUD
- post-category/post-tag mapping

## Interview prep
### 5-line summary
Category Service manages taxonomy for content discovery. It keeps categories and tags structured. Posts can be mapped to multiple categories and tags. This improves content organization and filtering. It is a supporting but important domain service.

### Common interview questions
1. Why use categories and tags?
2. What is taxonomy mapping?
3. How do you attach a category to a post?
4. Why separate tag and category?
5. How does frontend load categories/tags?

---

# 2.7 Notification Service

## Purpose of this service

### Why it exists
Notification Service handles in-app notifications and email notifications.

### Business problem it solves
- notify users about actions like posts, comments, newsletters
- store notification history
- show unread counts fast

### Which services it talks to
- Kafka
- MySQL
- Redis
- JavaMailSender
- Gateway/frontend

### Which frontend pages use it
- Notification bell
- Notifications page

## Important files
Visible in workspace:
- `NotificationResource.java`
- `NotificationServiceImpl.java`
- `NotificationKafkaConsumer.java`
- repository/entity/DTO files

### Important implementation detail from code
`NotificationServiceImpl` uses:
- `NotificationRepository`
- `AppUserRepository`
- `JavaMailSender`
- `ObjectProvider<StringRedisTemplate>`
- Redis unread count cache with prefix like `notification:unread`
- `@Value` for email sender and cache TTL

## Core service methods (from code)
- `send(NotificationRequest request)`
- `sendBulk(BulkNotificationRequest request)`
- `markAsRead(Long notificationId)`
- `markAllRead(Long recipientId)`
- `deleteRead(Long recipientId)`
- `getByRecipient(Long recipientId)`
- `getUnreadCount(Long recipientId)`
- `deleteNotification(Long notificationId)`
- `sendEmail(NotificationRequest request)`
- `getAll()`

## Kafka flow
From `NotificationKafkaConsumer`:
- listens on topic like `notification.dispatch.v1`
- decides whether to dispatch email or in-app notification
- bulk dispatch uses `sendBulk`
- single-recipient dispatch uses `send`

## Business logic
- unread count cache is read from Redis first
- cache is evicted whenever notifications change
- email is sent only if recipient user exists and is active
- read status updates affect unread count

## Frontend-backend connection
```text
NotificationBell / NotificationsPage
  ↓
notificationService in frontend
  ↓
apiRequest()
  ↓
Gateway
  ↓
NotificationResource
  ↓
NotificationServiceImpl
  ↓
NotificationRepository / Redis / Mail
  ↓
MySQL / Redis state
  ↓
JSON response
  ↓
React unread badge / list update
```

## Interview prep
### 5-line summary
Notification Service stores and manages notifications. It supports unread counts with Redis caching. It can send emails too. It consumes Kafka events for async dispatch. It is the user engagement delivery layer.

### Common interview questions
1. Why use Redis for unread counts?
2. Why is Kafka used for notification events?
3. How do you mark notifications as read?
4. What is the difference between single and bulk notifications?
5. Why use email and in-app notifications together?

---

# 2.8 Newsletter Service

## Purpose of this service

### Why it exists
Newsletter Service manages subscription, confirmation, unsubscribe, campaign mails, and post alert mails.

### Business problem it solves
- lets users subscribe/unsubscribe
- verifies email ownership
- sends broadcast mail to entitled users
- sends post alert emails

### Which services it talks to
- Auth Service for entitlement validation
- Notification Service via Kafka
- SMTP / JavaMailSender
- Frontend newsletter pages

### Which frontend pages use it
- Newsletter page
- Subscribe/confirm/unsubscribe flows
- Admin newsletter broadcast UI

## Important files
Visible in workspace:
- `NewsletterServiceImpl.java`
- `NewsletterResource.java`
- `SubscribeRequest.java`
- `SendNewsletterRequest.java`
- `SendPostNotificationRequest.java`
- `SubscriberResponse.java`
- `NotificationDispatchEvent.java`

### Important implementation detail from code
`NewsletterServiceImpl` uses:
- `SubscriberRepository`
- `JavaMailSender`
- `RestTemplate` for auth-service entitlement lookup
- `ObjectProvider<KafkaTemplate<String, NotificationDispatchEvent>>`
- properties for:
  - `spring.mail.username`
  - `inkwell.frontend-url`
  - `inkwell.newsletter.public-base-url`
  - `inkwell.auth-service-url`
  - `inkwell.internal-api-key`

## Core public methods (from code)
- `subscribe(SubscribeRequest request)`
- `unsubscribe(String token)`
- `confirmSubscription(String token)`
- `getSubscriberByEmail(String email)`
- `getAllSubscribers()`
- `sendNewsletter(SendNewsletterRequest request)`
- `sendPostNotification(SendPostNotificationRequest request)`
- `updatePreferences(UpdatePreferencesRequest request)`
- `getSubscriberCount(SubscriberStatus status)`
- `sendWelcomeEmail(String email)`

## Important internal helper flow
- `fetchEntitlements(userId)` calls auth-service with `X-Internal-Api-Key`
- `filterEntitledRecipients()` keeps only admin/newsletter-entitled users
- `sendPlainEmail()` sends simple mail via SMTP
- `sendInAppNotification()` builds Kafka event
- `publishNotificationEvent()` sends event to Kafka topic `notification.dispatch.v1`

## Business logic
- subscription starts in `PENDING`
- confirmation token expires after 24 hours
- confirmation moves status to `ACTIVE`
- unsubscribe moves status to `UNSUBSCRIBED`
- newsletter recipients are filtered by preferences and entitlement
- post notifications build frontend post URL using `frontendUrl + /post/{slug}`

## Frontend-backend connection
```text
NewsletterPage / Subscribe form
  ↓
apiRequest("/newsletter/subscribe")
  ↓
Gateway
  ↓
NewsletterResource
  ↓
NewsletterServiceImpl.subscribe()
  ↓
Auth-service entitlement check
  ↓
SubscriberRepository save
  ↓
SMTP confirmation mail + Kafka in-app event
  ↓
Frontend shows pending confirmation
```

## Important code-based explanation from workspace
In `fetchEntitlements(Long userId)`:
- the method creates `HttpHeaders`
- sets `X-Internal-Api-Key`
- calls auth-service internal URL `/auth/internal/entitlements/{userId}`
- returns `EntitlementResponse`
- returns `null` on error

In `filterEntitledRecipients()`:
- it checks `userId`
- it uses a cache map to avoid repeated entitlement calls
- it keeps only admin or newsletter-entitled users

## Interview prep
### 5-line summary
Newsletter Service controls subscription lifecycle and newsletter campaigns. It validates users through auth-service. It sends confirmation, welcome, unsubscribe, and newsletter mails. It publishes in-app notification events through Kafka. It is both a communication and subscription domain service.

### Common interview questions
1. Why is entitlement checked from auth-service?
2. Why do you use an internal API key?
3. Why does subscription start as `PENDING`?
4. What happens when confirmation token expires?
5. Why use Kafka for in-app notification dispatch?

---

# 2.9 Media Service

## Purpose of this service

### Why it exists
Media Service handles upload, storage, metadata, and linking of media assets.

### Business problem it solves
- separate media from post body
- support images/files in post editor
- reuse and manage uploaded assets

### Which services it talks to
- Post Service
- Frontend author dashboard
- Storage layer (local/S3 depending on config)

### Which frontend pages use it
- Author dashboard media panel
- Post editor

## Important files
- controller
- service and repository
- media entity
- DTOs for upload/link response
- storage config if present

## Business logic
- upload file
- store metadata like original name, MIME type, alt text
- generate accessible URL
- link media to a post
- delete media

## Interview prep
### 5-line summary
Media Service manages files uploaded by authors. It stores metadata and links media to posts. It improves content richness in the editor. It may use local or cloud storage depending on deployment. It is a supporting service for post creation.

### Common interview questions
1. Why keep media separate from post table?
2. How do you handle image upload?
3. Why store metadata?
4. How do you link media to a post?
5. How do you delete unused media?

---

# 2.10 Frontend React App

## Purpose of this service

### Why it exists
The React frontend is the user-facing interface of Inkwell.

### Business problem it solves
- gives a unified experience for reader, author, and admin
- hides backend complexity
- manages routing, forms, dashboards, and state

### Which services it talks to
- All backend services through API Gateway
- Auth Service via gateway and OAuth redirects
- Notification/Newsletter/Post/Media services via shared `apiRequest`

### Which frontend pages use it
- LoginPage
- SignupPage
- ForgotPasswordPage
- HomePage
- FeedPage
- PostPage
- ProfilePage
- AuthorDashboardPage
- NewsletterPage
- NotificationsPage
- AdminPanelPage

## Important files
Visible in workspace:
- `src/context/AuthContext.jsx`
- `src/lib/api.js`
- `src/pages/LoginPage.jsx`
- `src/pages/AuthorDashboardPage.jsx`
- other pages in `src/pages`
- `src/context/NotificationContext.jsx`
- `src/context/LoaderContext.jsx`
- `src/context/ThemeContext.jsx`

### Important implementation detail from code
`AuthContext.jsx` manages:
- `token`
- `user`
- `role`
- `isAuthenticated`
- `logout()`
- `refreshProfile()`

It also syncs auth data with localStorage using `useEffect`.

`api.js` does:
- base URL handling
- token reading from localStorage
- Authorization header injection
- unified fetch wrapper
- start/end notification events
- error toasts / success toasts

`LoginPage.jsx` does:
- form state with `useState`
- submit login form
- redirect with `useNavigate`
- OAuth2 redirects to Google/GitHub

`AuthorDashboardPage.jsx` does:
- loads posts/media/categories/tags/comments
- creates/updates/publishes/deletes posts
- uploads media
- approves/rejects comments
- uses `useMemo`, `useRef`, `useEffect`, `useState`

## Frontend hooks explanation
- `useState` → stores local form and UI data
- `useEffect` → side effects like syncing localStorage and API calls
- `useContext` → reads shared auth/notification state
- `useNavigate` → routes after login/logout
- `useRef` → scroll and editor access
- `useMemo` → optimization for derived stats

## Frontend-backend connection
```text
React component
  ↓
useAuth() / context state
  ↓
apiRequest(path, options)
  ↓
fetch(`${API_BASE}${path}`)
  ↓
Gateway
  ↓
JWT validation and header propagation
  ↓
Controller / Service / Repository
  ↓
MySQL / Kafka / Redis / Mail
  ↓
JSON response
  ↓
setState / context update
  ↓
UI refresh
```

## Security explanation
- token stored in localStorage under `inkwell_auth`
- `apiRequest()` automatically adds `Authorization: Bearer <token>`
- logout clears auth state and removes stored token
- auth profile is refreshed when token changes

## Error handling
- frontend shows API errors as notifications/toasts
- backend global handlers return JSON like `{ "message": "..." }`

## Logging
Frontend mainly logs via UI state/notifications rather than server logs.

## Interview prep
### 5-line summary
The React frontend is the presentation layer of Inkwell. It handles pages, forms, state, routing, and auth context. It calls backend APIs through a unified wrapper. It stores JWT and user info in localStorage. It makes the platform usable for reader, author, and admin roles.

### Common interview questions
1. Why use AuthContext?
2. How does logout work in frontend?
3. Why wrap fetch in `apiRequest()`?
4. How does `useNavigate` work after login?
5. How do you protect routes in React?

---

## 3. Cross-Cutting Concepts

---

# 3.1 Security

## JWT token flow
1. User logs in.
2. Auth service returns JWT.
3. Frontend stores token in localStorage.
4. `apiRequest()` sends it in `Authorization` header.
5. Gateway / JWT filter validates token.
6. Identity headers are forwarded to downstream services.

## Common headers
- `Authorization: Bearer <token>`
- `X-User-Id`
- `X-User-Role`
- `X-User-Email`
- internal headers like `X-Internal-Api-Key`

## Role checking
- gateway/security filters verify access
- controllers check ownership and admin/author roles
- method security may be used in auth-service

---

# 3.2 Kafka

## Why Kafka is used
- asynchronous communication
- decoupling services
- better scalability for notifications and event-driven flows

## Event flow
```text
Producer service
  ↓
Kafka topic
  ↓
Consumer service
  ↓
Business side-effect / DB update / mail
```

## In Inkwell
- newsletter → in-app notification event
- notification service consumes dispatch events
- other event-driven features can follow the same pattern

---

# 3.3 Feign

## Why Feign is used
Feign is used for clean service-to-service HTTP calls when a service needs synchronous data from another service.

## How Eureka helps
Feign can resolve logical service names through Eureka without hardcoded host/port.

## Drawbacks
- adds synchronous coupling
- one service failure can impact the caller
- needs fallback/resilience patterns

## Note for this workspace
If a specific service uses Feign in your code, explain that exact client and endpoint chain when you paste the file. If not visible, describe it as not used yet in the reviewed snippets.

---

# 3.4 Error Handling

## GlobalExceptionHandler
- `@RestControllerAdvice` gives centralized exception handling
- `@ExceptionHandler` catches specific exceptions
- `ResponseEntity` sets HTTP status and JSON body

Example error response:
```json
{ "message": "Invalid token" }
```

---

# 3.5 Logging

## Why logs matter
- help in debugging production issues
- show request/response flow
- capture errors, auth failures, Kafka failures, and mail failures

## Common log levels
- `info` → normal events
- `debug` → detailed internal state
- `warn` → recoverable issues
- `error` → failures

---

## 4. PDF Export Note

This file is **PDF-ready markdown**. To make it a PDF:
1. open it in a markdown editor, Word, or Google Docs
2. apply optional styling
3. export / print to PDF

If you want, I can also create a second version as:
- a **full HTML document with CSS**, or
- a **chaptered study guide split into separate files** for easier PDF conversion.

