# Ollama Chatbot (Full Stack)

A simple AI-powered chatbot built with **React** (Vite) and **Spring Boot**, utilizing **Ollama** for running large language models locally.

## Project Structure

- `chatbot/`: Frontend built with React, Vite, and CSS.
- `springweb/`: Backend built with Spring Boot and Spring AI (Ollama integration).

## Prerequisites

- **Java 17** or higher
- **Node.js** (v18+)
- **Ollama** installed and running locally
- A Llama model pulled in Ollama (e.g., `ollama pull llama3` or `ollama pull smollm2:135m`)

## Getting Started

### 1. Ollama Configuration
Ensure Ollama is running and you have a model downloaded.
```bash
ollama serve
# In another terminal
ollama pull llama3
```
*Note: The backend default configuration will look for a model. You might need to specify the model in `springweb/src/main/resources/application.properties`.*

### 2. Backend Setup (Spring Boot)
Navigate to the `springweb` directory:
```bash
cd springweb
./mvnw spring-boot:run
```
The backend will start on `http://localhost:8080`.

### 3. Frontend Setup (React)
Navigate to the `chatbot` directory:
```bash
cd chatbot
npm install
npm run dev
```
The frontend will start on `http://localhost:5173`. It is configured to proxy `/api` requests to `http://localhost:8080`.

## Features
- Local AI processing via Ollama.
- Real-time chat interface.
- Proxy configuration for seamless local development.

## License
MIT
