# 📚 Book Social Network API

The backend API for the Book Social Network, built with Java 17 and Spring Boot 3, supporting book management, borrowing, returns, and user notifications via WebSocket.

## ✨ Features

- **Authentication**: JWT-based security with registration and email validation.
- **Book Management**: Create, update, borrow, and return books.
- **Real-Time Notifications**: WebSocket notifications for borrowed or returned books.
- **Feedback**: Rate and review books.
- **API Documentation**: Integrated OpenAPI (Swagger) documentation.

## 🛠 Tech Stack

- Java 17
- Spring Boot 3.5.3
- PostgreSQL (Database)
- Docker & Docker Compose (Containerization)
- MailDev (Email testing)
- WebSocket (Real-time notifications)

## ⚡ Prerequisites

- Java JDK 17
- Maven
- Docker

## ⚙️ Configuration

The application uses `application-dev.yml` by default.

- **Database URL**: `jdbc:postgresql://localhost:5432/book_social_network`
- **MailDev**: `localhost:1025`
- **Server Port**: `8088`
- **Context Path**: `/api/v1`

## 🚀 Running the Application

### Start Services

Ensure PostgreSQL and MailDev containers are running:
```bash
docker-compose up -d
```

### Run with Maven
```bash
mvn clean compile spring-boot:run
```

### Access Swagger UI

Navigate to: [http://localhost:8088/api/v1/swagger-ui/index.html](http://localhost:8088/api/v1/swagger-ui/index.html)

## 📄 Pagination

Endpoints returning lists support pagination to manage large datasets efficiently.

### Parameters

- `page`: Page number (0-indexed). Default: `0`
- `size`: Items per page. Default: `10`

### Example
```http
GET /books?page=0&size=5
```

### Response Structure
```json
{
  "content": [...],
  "number": 0,
  "size": 5,
  "totalElements": 42,
  "totalPages": 9,
  "first": true,
  "last": false
}
```

## 🏛️ Architecture

This project follows **Clean Architecture** principles combined with **MVC pattern**, ensuring separation of concerns, maintainability, and testability.

### Layer Structure

```
src/main/java/com/ichaabane/book_network/
│
├── domain/                          # 🎯 Core Business Logic (Entities + Rules)
│   ├── model/                       # Domain entities (Book, User, Role, etc.)
│   ├── enums/                       # Business enums (TokenType, NotificationStatus)
│   ├── exception/                   # Business exceptions
│   └── repository/                  # Repository interfaces (ports)
│
├── application/                     # 💼 Use Cases & Business Logic
│   ├── service/                     # Business logic orchestration
│   ├── dto/                         # Data Transfer Objects
│   │   ├── request/                 # Request DTOs
│   │   └── response/                # Response DTOs
│   ├── mapper/                      # Entity ↔ DTO mappers
│   └── specification/               # Query specifications
│
├── infrastructure/                  # 🔧 Technical Implementation (Adapters)
│   ├── security/                    # JWT, filters, security config
│   ├── config/                      # Application configuration
│   ├── email/                       # Email infrastructure
│   └── file/                        # File storage utilities
│
└── presentation/                    # 🎮 Controllers (MVC Layer)
    ├── controller/                  # REST API endpoints
    └── handler/                     # Exception handlers
```

### Architecture Principles

#### 1. **Dependency Rule**
Dependencies point inward: `Presentation → Application → Domain`

- **Domain Layer**: Zero external dependencies, pure business logic
- **Application Layer**: Depends only on Domain
- **Infrastructure Layer**: Depends on Domain and Application
- **Presentation Layer**: Depends on Application

#### 2. **Layer Responsibilities**

**Domain Layer (Core)**
- Contains business entities (`Book`, `User`, `Feedback`, etc.)
- Defines business rules and constraints
- Repository interfaces (ports for infrastructure)
- Business exceptions
- **No framework dependencies**

**Application Layer (Use Cases)**
- Orchestrates business logic through services
- Implements use cases (`BookService`, `AuthenticationService`)
- DTOs for data transfer between layers
- Mappers to convert between entities and DTOs
- **Framework-agnostic business logic**

**Infrastructure Layer (Technical Concerns)**
- Security implementation (JWT, filters)
- Configuration classes
- Email and file system utilities
- External service integrations
- **Framework-specific implementations**

**Presentation Layer (API Interface)**
- REST controllers handle HTTP requests
- Exception handlers for API responses
- Request validation
- **MVC pattern for user interaction**

#### 3. **Benefits**

✅ **Maintainability**: Changes in one layer don't affect others  
✅ **Testability**: Business logic can be tested independently  
✅ **Flexibility**: Easy to swap implementations (e.g., change database)  
✅ **Scalability**: Clear structure for adding new features  
✅ **Domain-Centric**: Business logic is not tied to frameworks

### Data Flow Example

```
┌─────────────────────────────────────────────────────────────┐
│  HTTP Request → BookController (Presentation)               │
│       ↓                                                      │
│  BookService (Application) - orchestrates use case          │
│       ↓                                                      │
│  BookRepository (Domain interface)                          │
│       ↓                                                      │
│  JPA Implementation (Infrastructure - implicit)             │
│       ↓                                                      │
│  PostgreSQL Database                                        │
└─────────────────────────────────────────────────────────────┘
```

## 🔑 Best Practices

### Architecture

- **Clean Architecture**: Domain-centric design with clear layer boundaries
- **MVC Pattern**: Controllers handle HTTP, Services contain business logic
- **DTOs**: Data Transfer Objects (Records) separate API from internal domain models
- **Repository Pattern**: Abstract data access through interfaces

### Error Handling

- Global Exception Handling via `@ControllerAdvice` (Presentation Layer)
- Custom Business Exceptions in Domain Layer (e.g., `OperationNotPermittedException`)
- Standardized error responses across all endpoints

### Security

- **JWT Authentication** for all protected endpoints
- **Method Security**: Logical checks for operations (e.g., users cannot borrow their own books)
- Security logic isolated in Infrastructure Layer

### Code Organization

- **Single Responsibility**: Each class has one clear purpose
- **Dependency Injection**: All dependencies injected via constructors
- **Interface Segregation**: Small, focused repository interfaces
- **Package by Layer**: Clear separation between architectural layers

## 📡 WebSocket Notifications

Users are notified in real-time when a book is borrowed or returned.

- WebSocket configuration in Infrastructure Layer
- Notification service in Application Layer
- Real-time updates for borrowing/return operations

**Example**: If a user tries to borrow a book already borrowed, they receive a notification immediately.

## 📁 Project Structure

```
book-network-api/
├── src/
│   ├── main/
│   │   ├── java/com/ichaabane/book_network/
│   │   │   ├── domain/              # Core business logic
│   │   │   ├── application/         # Use cases & services
│   │   │   ├── infrastructure/      # Technical implementations
│   │   │   └── presentation/        # API controllers
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/                        # Unit and integration tests
├── pom.xml                          # Maven dependencies
└── README.md
```

## 🧪 Testing Strategy

The Clean Architecture enables comprehensive testing at each layer:

- **Domain Layer**: Pure unit tests (no mocks needed)
- **Application Layer**: Service tests with repository mocks
- **Infrastructure Layer**: Integration tests for security, config
- **Presentation Layer**: Controller tests with MockMvc

## 📚 Additional Resources

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot Best Practices](https://spring.io/guides)
---

**Note**: This project was recently refactored from a feature-based structure to Clean Architecture, improving maintainability and scalability while preserving all existing functionality.