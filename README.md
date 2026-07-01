# AI Chat Assistant — Spring Boot + Spring AI + NVIDIA NIM + Supabase

A full-stack AI chat assistant with user accounts, JWT authentication, persistent
conversation history, and login tracking — backed by **NVIDIA NIM** (via
[build.nvidia.com](https://build.nvidia.com)) for inference and **Supabase Postgres**
for storage.

## Stack

| Layer      | Tech |
|------------|------|
| Frontend   | React 19 + Vite, React Router |
| Backend    | Spring Boot 3.5, Spring AI 1.1 (OpenAI-compatible client) |
| AI model   | NVIDIA NIM (`nvidia/nemotron-3-ultra-550b-a55b` by default), OpenAI-compatible API |
| Database   | Supabase Postgres (JPA/Hibernate) |
| Auth       | Custom JWT (Spring Security, BCrypt password hashing) |

No Ollama, no local models — inference goes straight to NVIDIA's hosted NIM endpoint.

## Features

- Email/password registration and login (JWT-based, stateless)
- Every login is recorded in a `login_history` table — queryable directly from the Supabase table editor or SQL editor
- Chat sessions with full message history persisted per user (`chat_sessions`, `chat_messages`)
- Conversational context: prior turns in a session are replayed to the model so it remembers the conversation
- Clean, responsive dark-mode chat UI

## Project structure

```
springweb/   Spring Boot backend
chatbot/     React + Vite frontend
```

## 1. Supabase setup

1. In your Supabase project, go to **Project Settings → Database** and copy the
   connection string (host, port, database).
2. **Reset your database password** if you've ever shared it anywhere outside
   Supabase's dashboard — treat it as compromised the moment it leaves that page.
3. Tables (`app_users`, `login_history`, `chat_sessions`, `chat_messages`) are
   created automatically on first run via `spring.jpa.hibernate.ddl-auto=update` —
   no manual SQL needed. You can view/query them anytime in the Supabase
   **Table Editor** or **SQL Editor**.

## 2. Backend setup

```bash
cd springweb
cp .env.example .env   # then fill in real values — .env is git-ignored
```

Fill in `.env` (or export as real environment variables):

```
DB_URL=jdbc:postgresql://db.<your-project-ref>.supabase.co:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=<your Supabase DB password>

NVIDIA_API_KEY=<your key from build.nvidia.com>
NVIDIA_MODEL=nvidia/nemotron-3-ultra-550b-a55b

JWT_SECRET=<a long random string>
JWT_EXPIRATION_MS=86400000

CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Run it (env vars need to be in the shell's environment, or use a tool like
[direnv](https://direnv.net/) / your IDE's run config to load `.env`):

```bash
export $(cat .env | xargs)   # simple way to load .env into your shell (macOS/Linux)
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`.

## 3. Frontend setup

```bash
cd chatbot
cp .env.example .env   # VITE_API_URL=http://localhost:8080
npm install
npm run dev
```

Frontend runs on `http://localhost:5173`.

## API overview

| Method | Endpoint | Auth | Description |
|--------|----------|------|--------------|
| POST | `/api/auth/register` | none | Create account, returns JWT |
| POST | `/api/auth/login` | none | Login, returns JWT, logs to `login_history` |
| GET | `/api/chat/sessions` | Bearer | List your chat sessions |
| POST | `/api/chat/sessions` | Bearer | Create a new session |
| GET | `/api/chat/sessions/{id}/messages` | Bearer | Full message history of a session |
| POST | `/api/chat/send` | Bearer | Send a message, get an AI reply, both are saved |

## Security notes

- Passwords are hashed with BCrypt — never stored or logged in plain text.
- JWTs are stateless (`HS256`), sent as `Authorization: Bearer <token>`.
- All secrets live in `.env` files that are git-ignored — only `.env.example`
  (no real values) is committed.
- If a secret (DB password, API key) is ever pasted into a chat, doc, or commit,
  treat it as compromised and rotate it immediately.

## Resume-friendly summary

*Built a full-stack AI chat assistant (Spring Boot, Spring AI, React) with JWT
authentication, persistent conversation history, and login auditing, integrating
NVIDIA NIM for LLM inference and Supabase Postgres for storage.*

## License
MIT
