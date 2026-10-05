# Inkwell Project Deep-Dive Prompt

You are an expert **Spring Boot Microservices + React instructor**.

I want to understand **all microservices of my Inkwell / Inkwell blogging platform** in very deep detail.

## My stack
- React frontend
- Spring Boot microservices
- API Gateway
- Eureka Server
- JWT / Spring Security
- OAuth2
- MySQL + JPA
- Kafka
- Razorpay
- Docker / AWS EC2

## Explain these services one by one

1. Eureka Server
2. API Gateway
3. Auth Service
4. Post Service
5. Comment Service
6. Category Service
7. Notification Service
8. Newsletter Service
9. Media Service
10. Frontend React app

## For EACH service, explain in this format

### 1. Purpose of this service
- Why this service exists
- What business problem it solves
- Which other services it talks to
- Which frontend pages use it

### 2. Important files
Explain:
- `application.yml` / properties
- Main class
- Controller
- Service interface
- ServiceImpl
- Repository
- Entity / Model
- DTO / Request / Response
- Feign Client
- Kafka Producer / Consumer
- Config classes
- Exception classes

### 3. Code explanation
When I paste code, explain:
- package
- imports
- class
- annotations
- variables
- constructor
- methods
- return type
- parameters
- business logic
- runtime flow

### 4. Annotation explanation
For every annotation, explain:
- meaning
- purpose
- who reads it
- why it is used
- what happens if removed

Examples:
`@RestController`, `@Service`, `@Repository`, `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@RequestBody`, `@RequestHeader`, `@PathVariable`, `@Valid`, `@FeignClient`, `@KafkaListener`, `@Configuration`, `@Bean`, `@Value`, `@RestControllerAdvice`, `@ExceptionHandler`, `@SpringBootApplication`

### 5. API explanation
For every endpoint, explain:
- HTTP method
- full URL
- request body
- response body
- headers
- frontend function calling it
- backend flow
- database operation
- business purpose

### 6. Frontend-backend connection
Explain full flow:
React page/component
→ `lib/api.js` function
→ `fetch` / Axios call
→ API Gateway
→ JWT validation
→ Controller
→ Service
→ Repository
→ MySQL
→ Response
→ React state update
→ UI update

### 7. Database flow
Explain:
- entity to table mapping
- each field
- repository method
- `save` / `findById` / `findAll` / `delete`
- insert / update / select query meaning

### 8. Business logic
Explain important logic deeply:
- login/register/JWT
- gateway routing/CORS
- post creation
- comment moderation
- category/tag handling
- notification send
- newsletter subscription
- media upload/linking
- payment charge
- Razorpay order creation
- payment verification
- delivery assignment
- tracking updates
- review creation
- role validation

### 9. Feign explanation
Wherever Feign is used, explain:
- why Feign is used
- which service calls which service
- how Eureka helps
- synchronous communication flow
- drawbacks

### 10. Kafka explanation
Wherever Kafka is used, explain:
- producer
- consumer
- topic
- event DTO
- async communication flow
- why Kafka instead of Feign

### 11. Security explanation
Explain:
- JWT token flow
- Authorization header
- `X-Authenticated-User`
- `X-Authenticated-Role`
- role checking
- Spring Security filters
- password encoding
- OAuth2 if used

### 12. Error handling
Explain:
- `GlobalExceptionHandler`
- `@RestControllerAdvice`
- `@ExceptionHandler`
- `ResponseEntity`
- how errors return JSON response

### 13. Logging
Explain:
- logger
- info/debug/warn/error
- why logs are useful in production

### 14. Flow diagram
For every service, create text diagram like:

Frontend
 ↓
API Gateway
 ↓
JWT Filter
 ↓
Controller
 ↓
ServiceImpl
 ↓
Repository
 ↓
Database
 ↓
Response
 ↓
Frontend UI update

Also add Feign/Kafka flow wherever used.

### 15. Interview preparation
For every service, give:
- 5-line summary
- important technical points
- important business logic points
- common interview questions
- simple answers I can speak

## Important rules
- Explain in very simple Hinglish / easy English.
- Do not give only theory.
- Always connect with my code.
- Explain every important word.
- Explain why code is written.
- Explain runtime flow.
- Explain frontend and backend together.
- Explain for sprint review and interview preparation.

## Start by giving me the complete high-level architecture of all microservices, then ask me to paste code of the first service.

---

## Inkwell project mapping

This prompt is tailored for my **Inkwell blogging platform**. The expected services are:
- Eureka Server
- API Gateway
- Auth Service
- Post Service
- Comment Service
- Category Service
- Notification Service
- Newsletter Service
- Media Service
- Frontend React app

The explanation should be based on the actual code in my workspace, especially the current Inkwell frontend and backend source files.


