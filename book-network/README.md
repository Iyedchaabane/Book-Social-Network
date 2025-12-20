# Book Network API

The backend API for the Book Social Network, built with Java 17 and Spring Boot 3.

## Features

- **Authentication**: JWT-based security with registration and email validation.
- **Book Management**: Manage books, borrowing, and returns.
- **Feedback**: Rate and review books.
- **API Documentation**: Integrated OpenAPI (Swagger) documentation.

## Tech Stack

- **Java 17**
- **Spring Boot 3.5.3**
- **PostgreSQL** (Database)
- **Docker & Docker Compose** (Containerization)
- **MailDev** (Email testing)

## Prerequisites

- **Java JDK 17**
- **Maven**
- **Docker**

## Configuration

The application uses `application-dev.yml` active query by default.
- **Database URL**: `jdbc:postgresql://localhost:5432/book_social_network`
- **MailDev**: `localhost:1025`
- **Server Port**: `8088`
- **Context Path**: `/api/v1`

## Running the Application

1.  **Start Services**:
    Ensure the PostgreSQL and MailDev containers are running (from the root directory):
    ```bash
    docker-compose up -d
    ```

2.  **Run with Maven**:
    ```bash
    mvn clean compile spring-boot:run
    ```

## Pagination

The API endpoints that return lists of data support pagination to help manage large datasets efficiently.

### Parameters
- `page`: The page number to retrieve (0-indexed). **Default: 0**.
- `size`: The number of items per page. **Default: 10**.

### Example Usage
To get the first page of books with 5 items per page:
`GET /books?page=0&size=5`

### Response Structure
Paginated responses are wrapped in a `PageResponse` object containing:
- `content`: List of items.
- `number`: Current page number.
- `size`: Current page size.
- `totalElements`: Total number of items available.
- `totalPages`: Total number of pages.
- `first`: Boolean indicating if this is the first page.
- `last`: Boolean indicating if this is the last page.

## Best Practices

### Architecture
- **Layered Architecture**: The application follows a strict Controller-Service-Repository pattern.
- **DTOs**: Data Transfer Objects (Records) are used for all API requests and responses to decouple the internal domain model from the API.

### Error Handling
- **Global Exception Handling**: A `@ControllerAdvice` is used to catch and handle exceptions globally, providing consistent error responses.
- **Custom Exceptions**: Domain-specific exceptions are used (e.g., `OperationNotPermittedException`).

### Security
- **JWT Authentication**: all protected endpoints require a valid JSON Web Token.
- **Method Security**: Specific actions like borrowing or returning books have logical checks to ensure data integrity (e.g., users cannot borrow their own books).

## API Documentation

Once the application is running, you can access the Swagger UI documentation at:
`http://localhost:8088/api/v1/swagger-ui/index.html`

## Directory Structure

- `src/main/java`: Source code.
- `src/main/resources`: Configuration files.
