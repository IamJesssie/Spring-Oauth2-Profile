# 🏗️ System Architecture Documentation

## 📋 Architecture Overview

This document describes the comprehensive architecture of the Spring Boot OAuth2 Profile Management System using **traditional server-side rendering** with Thymeleaf, including system design, component relationships, and implementation patterns.

## 🏛️ System Architecture Diagram

```mermaid
graph TB
    %% Main Application Layer
    A[Spring Boot Application] --> B[Security Layer]
    A --> C[Web Layer]
    A --> D[Service Layer]
    A --> E[Database Layer]

    %% Security Layer Components
    B --> F[Spring Security OAuth2]
    F --> G[OAuth2 Client]
    F --> H[Session Management]
    F --> I[CSRF Protection]

    %% OAuth2 Providers
    G --> J[GitHub OAuth2]
    G --> K[Google OAuth2]

    %% Web Layer Components
    C --> L[UserController]
    C --> M[Thymeleaf Engine]
    C --> N[Static Resources]
    C --> O[Bootstrap 5.3.0]

    %% Controller Endpoints
    L --> P[GET /profile - View Form]
    L --> Q[POST /profile - Submit Form]

    %% Service Layer
    D --> R[CustomOAuth2UserService]
    D --> S[User Provisioning]
    D --> T[Provider Management]

    %% Database Layer
    E --> U[H2 Database]
    U --> V[User Table]
    U --> W[AuthProvider Table]

    %% External Dependencies
    J -.-> X[GitHub API]
    K -.-> Y[Google Identity API]
    O -.-> Z[CDN Bootstrap]

    style P fill:#90EE90
    style Q fill:#87CEEB
```

## 🏢 Component Architecture

### **1. Presentation Layer (Server-Side Rendering)**
- **Thymeleaf Templates** - Server-side HTML generation
- **Bootstrap 5.3.0** - Responsive CSS framework
- **HTML5 Forms** - Traditional form submission (no JavaScript required)
- **Flash Attributes** - Cross-redirect message passing

**Key Components:**
- `profile.html` - Profile management page with form
- `error.html` - Error handling pages
- `index.html` - Home page with OAuth2 login buttons

**Design Pattern:** **Post-Redirect-Get (PRG)** - Prevents duplicate form submissions

### **2. Web Layer (Controllers)**
- **UserController** - Single controller for profile management
  - `GET /profile` → Returns HTML with user data pre-filled
  - `POST /profile` → Processes form data and redirects
- **Model Attributes** - Passes data from controller to templates
- **Flash Attributes** - Success/error messages after redirects

**Key Features:**
- Form binding with `@ModelAttribute`
- Redirect with `RedirectAttributes` for flash messages
- Server-side rendering with `Model` object

### **3. Service Layer (Business Logic)**
- **CustomOAuth2UserService** - OAuth2 user processing and persistence
- **User Provisioning** - Creates user on first login
- **Provider Mapping** - Links OAuth2 providers to users

**Key Components:**
- User creation/update logic
- Email extraction from OAuth2 attributes
- GitHub fallback email generation

### **4. Data Layer (Persistence)**
- **H2 Database** - In-memory database for development
- **JPA Entities** - User and AuthProvider domain models
- **Repository Pattern** - UserRepository, AuthProviderRepository
- **Automatic Timestamps** - `@PreUpdate` hook for `updatedAt`

### **5. Security Layer (Protection)**
- **OAuth2 Authorization Code Flow** - Secure token exchange
- **Session Management** - Server-side session storage
- **CSRF Protection** - Hidden tokens in forms
- **Endpoint Protection** - Authentication-required resources

## 🔄 Request Flow Architecture

### **OAuth2 Authentication Flow**
```mermaid
sequenceDiagram
    participant U as User Browser
    participant A as Spring Boot App
    participant S as Spring Security
    participant P as OAuth2 Provider
    participant CS as CustomOAuth2UserService
    participant D as H2 Database

    U->>A: 1. Click Login Button (GitHub/Google)
    A->>S: 2. Initiate OAuth2 Flow
    S->>P: 3. Redirect to OAuth2 Authorization
    P->>U: 4. Show Consent Screen
    U->>P: 5. Grant Permission
    P->>S: 6. Return Authorization Code
    S->>P: 7. Exchange Code for Access Token
    P->>S: 8. Return Token + User Info
    S->>CS: 9. Process OAuth2 User
    CS->>D: 10. Check if User Exists
    
    alt First Time Login
        CS->>D: 11a. Create User Record
        CS->>D: 11b. Create AuthProvider Record
    else Existing User
        CS->>D: 11c. Find Existing User
    end
    
    CS->>S: 12. Return Authenticated User
    S->>A: 13. Set Session Cookie
    A->>U: 14. Redirect to /profile
```

### **Profile View Flow (GET Request)**
```mermaid
sequenceDiagram
    participant U as User Browser
    participant C as UserController
    participant R as UserRepository
    participant D as Database
    participant T as Thymeleaf Engine

    U->>C: 1. GET /profile
    C->>C: 2. Extract email from OAuth2User
    C->>R: 3. findByEmail(email)
    R->>D: 4. Query USERS table
    D->>R: 5. Return User record
    R->>C: 6. User object
    C->>C: 7. Add user to Model
    C->>T: 8. Render "profile" template
    T->>T: 9. Bind user data to th:value
    T->>C: 10. Generated HTML with form
    C->>U: 11. HTTP 200 + HTML Response
    Note over U: User sees form with current data
```

### **Profile Update Flow (POST Request)**
```mermaid
sequenceDiagram
    participant U as User Browser
    participant C as UserController
    participant R as UserRepository
    participant D as Database

    U->>C: 1. POST /profile (form data + CSRF token)
    C->>C: 2. Validate CSRF token
    C->>C: 3. Bind form to ProfileUpdateRequest
    C->>C: 4. Extract email from OAuth2User
    C->>R: 5. findByEmail(email)
    R->>D: 6. Query USERS table
    D->>R: 7. Return User record
    R->>C: 8. User object
    C->>C: 9. Update displayName & bio
    C->>C: 10. Set updatedAt timestamp
    C->>R: 11. save(user)
    R->>D: 12. UPDATE USERS table
    D->>R: 13. Confirm save
    R->>C: 14. Updated User object
    C->>C: 15. Add flash message "Profile updated!"
    C->>U: 16. HTTP 302 Redirect to /profile
    
    Note over U: Browser follows redirect
    
    U->>C: 17. GET /profile (see above flow)
    Note over U: User sees updated data + success message
```

**Key Pattern:** This implements the **Post-Redirect-Get (PRG)** pattern:
- POST processes the form submission
- Redirect prevents duplicate submissions (F5 refresh safe)
- GET displays the updated data with success message

## 🗄️ Database Architecture

### **Entity Relationship Diagram**
```mermaid
erDiagram
    USER ||--o{ AUTH_PROVIDER : has

    USER {
        bigint id PK
        varchar email UK
        varchar displayName
        varchar avatarUrl
        clob bio
        timestamp createdAt
        timestamp updatedAt
    }

    AUTH_PROVIDER {
        bigint id PK
        provider provider
        varchar providerUserId
        bigint userId FK
        varchar providerEmail
    }
```

### **Database Schema Details**

#### **User Table**
```sql
CREATE TABLE user (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    avatar_url VARCHAR(255),
    bio CLOB,
    created_at TIMESTAMP(6),
    updated_at TIMESTAMP(6),
    PRIMARY KEY (id)
);
```

#### **AuthProvider Table**
```sql
CREATE TABLE auth_providers (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    provider ENUM('GITHUB','GOOGLE','LOCAL') NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE (provider, provider_user_id),
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

## 🔒 Security Architecture

### **Authentication Architecture**
- **OAuth2 Authorization Code Flow** - Industry-standard secure authentication
- **Session-based Security** - Server-side session management (no JWT)
- **Provider Integration** - GitHub and Google OAuth2 applications
- **User Mapping** - AuthProvider table maintains cross-provider relationships

### **Security Configuration Details**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/error", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/profile", true)  // Redirect after login
                .userInfoEndpoint(userInfo -> 
                    userInfo.userService(customOAuth2UserService)
                )
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")  // H2 console only
            );
    }
}
```

### **CSRF Protection Flow**
```mermaid
sequenceDiagram
    participant B as Browser
    participant S as Spring Security
    participant C as Controller
    participant D as Database

    B->>C: GET /profile
    C->>S: Generate CSRF Token
    S->>C: Return Token
    C->>B: HTML with hidden CSRF field
    
    Note over B: User fills form
    
    B->>S: POST /profile (form data + CSRF token)
    S->>S: Validate CSRF Token
    
    alt Valid Token
        S->>C: Forward Request
        C->>D: Update User
        C->>B: Redirect to /profile
    else Invalid Token
        S->>B: 403 Forbidden
    end
```

### **Session Management**
- **Cookie-based Sessions** - JSESSIONID cookie
- **Server-side Storage** - Spring session storage
- **Logout Cleanup** - Invalidates session and clears cookies
- **HTTPS Recommended** - Secure cookie transmission in production
- **CSRF Protection** - Token-based form submission protection
- **Session Security** - Secure cookie configuration with proper flags
- **Logout Handling** - Complete session cleanup and invalidation

### **OAuth2 Provider Integration**

#### **GitHub OAuth2 Integration**
- **Client Configuration** - GitHub OAuth2 app with proper redirect URIs
- **User Profile Access** - GitHub API integration for user data
- **Fallback Email Handling** - GitHub login as email when email scope not available
- **Provider User ID** - GitHub user ID for account linking

#### **Google OAuth2 Integration**
- **OIDC Integration** - Google OpenID Connect for authentication
- **Profile Information** - Google account data and email access
- **Email Verification** - Google account email validation
- **Provider User ID** - Google subject (sub) claim for account linking

## 🚀 Deployment Architecture

### **Development Environment**
- **Local Development Server** - Spring Boot embedded Tomcat
- **H2 Database** - In-memory database with web console
- **Hot Reload** - Spring Boot DevTools for rapid development
- **Debug Logging** - Comprehensive logging for troubleshooting

### **Production Environment**
- **Application Server** - Tomcat/Undertow deployment ready
- **PostgreSQL/MySQL** - Production database support (schema ready)
- **Environment Variables** - OAuth2 credentials configuration
- **Session Clustering** - Multi-server session management support

### **Configuration Management**
```properties
# Development (H2)
spring.datasource.url=jdbc:h2:mem:testdb
spring.h2.console.enabled=true

# Production (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/oauth2_profile
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

## 🔧 System Components

### **1. Spring Boot Application Core**
- **Main Application Class** - Spring Boot application entry point
- **Component Scanning** - Automatic bean discovery and wiring
- **Configuration Properties** - Externalized configuration management
- **Health Checks** - Application monitoring and status

### **2. Security Configuration**
- **OAuth2 Client Configuration** - GitHub and Google client registration
- **Security Filter Chain** - Request filtering and protection
- **Session Management** - Concurrent session control
- **Logout Configuration** - Secure session termination

### **3. OAuth2 User Service**
- **Custom User Processing** - OAuth2 user information handling
- **Database Persistence** - User and AuthProvider record management
- **Provider-specific Logic** - GitHub vs Google data processing
- **Error Handling** - Comprehensive exception management

### **4. Web Controllers**
- **UserController** - Profile management endpoints
- **ErrorController** - Error handling and user feedback
- **Request Mapping** - RESTful endpoint design
- **Response Handling** - JSON and HTML response management

### **5. Data Access Layer**
- **Repository Interfaces** - JPA repository pattern implementation
- **Entity Management** - JPA entity lifecycle management
- **Query Optimization** - Efficient database queries
- **Transaction Management** - Database transaction handling

## 🌐 Endpoint Architecture

### **Web Endpoints (Traditional Form-Based)**

| Endpoint | Method | Purpose | Response Type | Auth Required |
|----------|--------|---------|---------------|---------------|
| `GET /` | GET | Home page with OAuth2 login buttons | HTML | No |
| `GET /profile` | GET | View profile with editable form (server-rendered) | HTML | Yes |
| `POST /profile` | POST | Update profile via form submission | Redirect (302) | Yes |
| `GET /logout` | GET | Logout user and invalidate session | Redirect (302) | Yes |
| `GET /h2-console` | GET | Database console (dev only) | HTML | No* |
| `GET /debug/users` | GET | Debug: View all users | Plain Text | No* |
| `GET /test-oauth` | GET | Debug: OAuth2 principal info | Plain Text | Yes |

*Debug endpoints should be disabled in production

### **Request/Response Pattern: Post-Redirect-Get (PRG)**

```mermaid
graph LR
    A[Browser] -->|1. POST /profile| B[Controller]
    B -->|2. Process & Save| C[Database]
    C -->|3. Success| B
    B -->|4. HTTP 302 Redirect| A
    A -->|5. GET /profile| B
    B -->|6. Load Updated Data| C
    C -->|7. User Object| B
    B -->|8. Render HTML| A
    
    style B fill:#FFD700
    style C fill:#90EE90
```

**Benefits of PRG Pattern:**
- Prevents duplicate form submissions (F5 refresh safe)
- Clean URL after form submission
- Flash messages display only once
- Browser back button works correctly

### **Form Data Binding**
```java
// Controller Method
@PostMapping("/profile")
public String updateProfile(
    @ModelAttribute ProfileUpdateRequest request,  // Form binding
    RedirectAttributes redirectAttributes          // Flash messages
) {
    // Update database
    redirectAttributes.addFlashAttribute("successMessage", "Profile updated!");
    return "redirect:/profile";  // PRG pattern
}
```

### **Template Data Binding (Thymeleaf)**
```html
<!-- Display user data -->
<h4 th:text="${user.displayName}">Display Name</h4>
<p th:text="${user.email}">Email</p>

<!-- Form with pre-filled values -->
<form method="POST" action="/profile">
    <input type="hidden" th:name="${_csrf.parameterName}" 
           th:value="${_csrf.token}"/>
    <input type="text" name="displayName" 
           th:value="${user.displayName}"/>
    <textarea name="bio" th:text="${user.bio}"></textarea>
</form>

<!-- Flash message display -->
<div th:if="${successMessage}" class="alert alert-success">
    <span th:text="${successMessage}">Success!</span>
</div>
```

## 📊 Performance Architecture

### **Optimization Strategies**
- **Database Connection Pooling** - HikariCP for optimal database performance
- **Server-Side Rendering** - Thymeleaf template caching
- **Static Resource Optimization** - CDN-hosted Bootstrap and icons
- **Minimal Frontend Complexity** - No JavaScript frameworks or build steps
- **Efficient Queries** - Optimized JPA queries with proper indexing

### **Scalability Considerations**
- **Stateful Design** - Session-based architecture (requires sticky sessions or session replication)
- **Session Management** - Can be externalized to Redis or database
- **Database Optimization** - Efficient data access patterns with JPA
- **Horizontal Scaling** - Multiple instances with shared session storage
- **Template Caching** - Thymeleaf template cache in production mode

## 🔍 Monitoring & Debugging

### **Logging Architecture**
- **OAuth2 Flow Logging** - Provider interaction tracking
- **Database Operation Logging** - Persistence monitoring
- **Security Event Logging** - Authentication tracking
- **Error Logging** - Exception tracking and debugging

### **Debug Endpoints**
- **/debug/users** - View all users in database
- **/test-oauth** - Test OAuth2 principal information
- **H2 Console** - Direct database access and querying

## 🚀 Deployment Strategy

### **Development Deployment**
```bash
./mvnw spring-boot:run
# Access: http://localhost:8080/
# H2 Console: http://localhost:8080/h2-console
```

### **Production Deployment**
```bash
./mvnw clean package
java -jar target/spring-oauth2-profile-0.0.1-SNAPSHOT.jar
```

### **Environment Configuration**
- **OAuth2 Credentials** - Environment variables or properties file
- **Database Configuration** - Production database connection
- **Session Configuration** - Production session management
- **Security Headers** - Production security configuration

## 🛠️ Development Workflow

### **Package Structure Benefits**
- **Separation of Concerns** - Clear component boundaries
- **Maintainability** - Easy to locate and modify components
- **Testability** - Isolated testing of individual layers
- **Scalability** - Easy to add new features and components

### **Code Organization**
- **Config Package** - Security and application configuration
- **Controller Package** - Web layer and API endpoints
- **Service Package** - Business logic and OAuth2 processing
- **Repository Package** - Data access layer
- **Model Package** - Domain entities and enums
- **DTO Package** - Data transfer objects

## 📈 Future Enhancements

### **Potential Improvements**
- **Database Migration** - PostgreSQL/MySQL for production scale
- **API Endpoints** - Add REST API for mobile/SPA applications
- **Email Notifications** - Welcome and profile update emails
- **Profile Pictures** - Custom avatar upload functionality
- **Social Features** - User discovery and following capabilities
- **Admin Dashboard** - User management interface
- **Advanced Search** - Find users by name or interests
- **Two-Factor Authentication** - Enhanced security option

### **Performance Optimizations**
- **Session Store** - Redis for distributed session management
- **CDN Integration** - Static resource optimization
- **Database Indexing** - Optimized query performance
- **Template Pre-compilation** - Faster Thymeleaf rendering
- **HTTP/2 Server Push** - Faster page loads

### **Alternative Architecture Patterns**
- **REST API + SPA** - Modern single-page application approach
  - Keep server-side logic, add JSON endpoints
  - Add React/Vue.js frontend consuming REST API
  - Benefits: Better UX, no page reloads, modern feel
  - Trade-offs: More complexity, JavaScript required
  
- **Current Traditional Approach** (Implemented)
  - Server-side rendering with Thymeleaf
  - Traditional form POST with page reload
  - Benefits: Simple, no JavaScript, SEO-friendly, matches requirements
  - Trade-offs: Page reloads, less interactive UX

## 🏆 Architecture Quality Assessment

### **Strengths**
- ✅ **Clean Architecture** - Well-organized, maintainable structure
- ✅ **Security First** - Comprehensive security implementation (OAuth2, CSRF, sessions)
- ✅ **Simple & Effective** - Traditional patterns that are easy to understand
- ✅ **Scalable Design** - Ready for production deployment with session store
- ✅ **Developer Experience** - Easy to understand and extend
- ✅ **Requirements Match** - Exactly fulfills assignment specifications
- ✅ **No JavaScript Required** - Works without client-side dependencies
- ✅ **SEO Friendly** - Fully server-rendered content

### **Trade-offs Made**
- ⚖️ **Traditional vs Modern** - Chose simplicity over modern SPA architecture
- ⚖️ **Page Reloads** - Form submission requires full page reload (PRG pattern)
- ⚖️ **Stateful Sessions** - Requires session management for scaling
- ⚖️ **Limited Interactivity** - No real-time updates or AJAX features

### **Production Readiness Checklist**
- ✅ **Database Migration Path** - H2 to PostgreSQL/MySQL ready
- ✅ **Security Hardened** - CSRF, session management, OAuth2
- ✅ **Error Handling** - User-friendly error pages
- ✅ **Logging** - Comprehensive debugging and monitoring
- ✅ **Documentation** - Complete setup and deployment guides
- ⚠️ **Session Clustering** - Requires Redis/database for multi-server
- ⚠️ **HTTPS Required** - Secure cookie transmission essential
- ⚠️ **OAuth2 Credentials** - Must be environment-specific

## 🎯 Architecture Philosophy

### **Why Traditional Server-Side Rendering?**

This implementation deliberately uses **traditional form-based architecture** instead of modern REST API + AJAX patterns:

#### **✅ Advantages:**
1. **Simplicity** - No client-side state management
2. **Security** - CSRF tokens handled automatically by Spring
3. **No JavaScript Required** - Works on any browser
4. **SEO Friendly** - All content server-rendered
5. **Learning Clarity** - Clear request-response flow
6. **Matches Requirements** - Fulfills "GET /profile – View own profile" literally
7. **Fast Development** - Less code to write and maintain
8. **Backward Compatible** - Works on older browsers

#### **⚠️ Trade-offs:**
1. **User Experience** - Page reloads on form submission
2. **Network Traffic** - Full HTML page transfers
3. **Perceived Speed** - Not as "snappy" as SPAs
4. **Limited Interactivity** - No real-time updates

#### **When to Migrate to REST API + SPA:**
- Need mobile app support
- Want modern, interactive UX
- Building complex dashboard/admin interface
- Need real-time features (notifications, chat)
- Target tech-savvy user base

The current architecture is **ideal for academic projects, MVPs, and applications prioritizing simplicity and security over cutting-edge UX**.

## 🎓 Educational Value

### **Concepts Demonstrated**
- ✅ OAuth2 authorization code flow
- ✅ Spring Security configuration
- ✅ JPA entity relationships
- ✅ Server-side template rendering (Thymeleaf)
- ✅ Post-Redirect-Get pattern
- ✅ CSRF protection implementation
- ✅ Session management
- ✅ Form data binding
- ✅ Flash attributes for messaging
- ✅ Repository pattern
- ✅ Service layer separation

This architecture serves as an excellent **learning platform** for understanding traditional web application patterns before moving to more complex microservices or SPA architectures.

## 🎯 Conclusion

This architecture provides a **robust, secure, and maintainable** foundation for OAuth2-based user profile management using **traditional server-side rendering patterns**. It prioritizes **simplicity, security, and educational value** while remaining production-ready with proper environment configuration and session management.

The design choice to use traditional form submission over modern AJAX patterns demonstrates that **simple solutions can be powerful** when they match requirements precisely and provide clear learning value.

---

*Architecture designed for educational clarity, production readiness, and exact requirements fulfillment.*

