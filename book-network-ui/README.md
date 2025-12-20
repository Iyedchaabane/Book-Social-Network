# Book Network UI

The frontend user interface for the Book Social Network, built with Angular 17.

## Features

- **User Authentication**: Login, Registration, Account Activation.
- **Book Library**: Browse, search, and manage books.
- **Borrowing System**: Borrow and return books.
- **Responsive Design**: Built with Bootstrap 5.

## Tech Stack

- **Angular 17**
- **TypeScript**
- **Bootstrap 5**
- **FontAwesome**
- **Ngx-Toastr**

## Prerequisites

- **Node.js** (LTS version recommended)
- **NPM**

## Getting Started

1.  **Install Dependencies**:
    ```bash
    npm install
    # If you encounter issues, try: npm install --force
    ```

2.  **Start Development Server**:
    ```bash
    npm start
    ```
    Or use the Angular CLI:
    ```bash
    ng serve
    ```

3.  **Access the Application**:
    Open your browser and navigate to `http://localhost:4200`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Backend Connection

This frontend connects to the backend API at `http://localhost:8088/api/v1`. Ensure the backend is running before using the application.

## Best Practices

### Architecture
- **Component-Based**: The UI is built using reusable Angular components (e.g., `BookCard`, `Menu`).
- **Services**: All API communication is handled via dedicated services to separate business logic from UI logic.
- **Lazy Loading**: Modules are lazy-loaded where possible to improve initial load time.

### Type Safety
- **Interfaces**: TypeScript interfaces/models are used to define the shape of API responses (e.g., `BookResponse`, `PageResponse`) to ensure type safety across the application.
