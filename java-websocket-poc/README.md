# Java WebSocket PoC

A proof-of-concept chat application demonstrating Spring Boot STOMP-over-WebSocket communication between a dedicated server and a standalone CLI client.

## Architecture

Single chat room, broadcast-all model. The client sends `username: message` strings to the server, which relays them to all subscribers via a simple in-memory broker. No persistence, no REST, no HTML frontend.

```
┌─────────────────────┐         STOMP/WebSocket         ┌─────────────────────┐
│  CLI Client (stdin)  │ ──────── ws://localhost:8080/ws ──────── │   Spring Boot Server │
│                     │  SEND /app/chat                 │                     │
│  subscribes to      │  ◄── MESSAGE /topic/chat ────── │  simple broker      │
│  /topic/chat        │                                 │  @MessageMapping    │
└─────────────────────┘                                 └─────────────────────┘
```

## Subprojects

### java-websocket-poc-server

Spring Boot application that hosts the WebSocket STOMP endpoint.

- **Package:** `com.example.chat.server`
- **Main class:** `ChatServerApplication`
- **Key classes:**
  - `WebSocketConfig` — registers the STOMP endpoint at `/ws`, enables a simple broker on `/topic`, sets application destination prefix `/app`
  - `ChatController` — `@MessageMapping("chat")` receives messages and `@SendTo("/topic/chat")` broadcasts them to all subscribers
- **Port:** 8080

### java-websocket-poc-client

Spring Boot CLI application that connects to the server and provides an interactive chat session via stdin/stdout.

- **Package:** `com.example.chat.client`
- **Main class:** `ChatClientApplication`
- **Behavior:**
  1. Prompts for a username
  2. Connects to the server using `WebSocketStompClient`
  3. Subscribes to `/topic/chat` and prints incoming messages
  4. Reads lines from stdin and sends them as `username: message` to `/app/chat`
  5. Type `/quit` to disconnect

## Prerequisites

- Java 21+
- No global Gradle install needed (both projects include the Gradle wrapper)

## Running

**1. Start the server:**

```sh
cd java-websocket-poc-server
./gradlew bootRun
```

**2. Start one or more clients (in separate terminals):**

```sh
cd java-websocket-poc-client
./gradlew bootRun
```

Each client will prompt for a username, then you can type messages that appear in all connected clients.

## Tech Stack

- Spring Boot 4.1.0
- Spring WebSocket (STOMP messaging)
- Tyrus standalone client (Jakarta WebSocket transport for the CLI client)
- Gradle 9.5.1
- Java 21
