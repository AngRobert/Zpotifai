# Zpotifai Digital Music Library

Zpotifai is a Java-based digital music library management system. It uses a PostgreSQL database managed via Docker Compose.

## Features
- Search for creators, albums, tracks, or podcasts.
- Full CRUD operations for music entities and tags.
- Tagging system for creators.
- Audit logging.

## Setup: The .env File

To run this project, you need to create a file named `.env` in the root directory. This file stores your database configuration.

### How to set up your .env:

1. Create a new file named `.env` in the project root.
2. Add the following lines, replacing the values if necessary:

```env
DB_NAME=zpotifai
DB_USER=postgres
DB_PASSWORD=your_password_here
DB_URL=jdbc:postgresql://localhost:5432/zpotifai
```

**Note:** Ensure the database name in `DB_URL` (the part after the last `/`) matches `DB_NAME`.

## How to Run

1. Ensure Docker is running.
2. Run the provided script:
   ```bash
   chmod +x run.sh
   ./run.sh
   ```

The script will automatically load your `.env` variables, start the database container, and launch the application.