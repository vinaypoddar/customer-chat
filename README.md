# Chat Application

## Overview
The JobRad Chat Application is a real-time chat system built with a **React** frontend and a **Spring Boot** backend. It enables **guests** (customers) to initiate chats and be assigned to available **agents** for one-on-one communication. The system uses **STOMP over WebSocket** (via SockJS) for real-time messaging, persists messages in an **H2 database**, and manages agent-guest assignments dynamically. The application ensures that agents can reply to assigned guests without manual recipient selection, and agents are freed for reassignment after a chat ends.

> note: this is a quick win solution, 
> State of the Art Design [here](README-STATE-OF-THE-ART.md)

## Functional Features
- **Guest Initiation**: Guests join anonymously via `/api/auth/anonymous`, receiving a unique `Guest_XXX` ID and an assigned agent (if available).
- **Agent Assignment**: Agents (e.g., `agent1`) are assigned to guests automatically. If no agent is available, guests are notified and updated when an agent becomes available.
- **Real-Time Messaging**: Guests and agents exchange messages in real-time, with messages displayed in a chat UI and persisted in the database.
- **Dynamic Agent Availability**: When a chat ends (guest or agent logs out), the agent is freed and can be assigned to a new guest.
- **Late Agent Connection**: Guests who join before an agent logs in are notified and connected when an agent becomes available, with the UI updating to reflect the connection.
- **Error Handling**: The UI displays errors for WebSocket disconnections, empty messages, or unavailable agents, with automatic retries for WebSocket connections.

## Technical Architecture
### Frontend
- **Framework**: React with TypeScript, Vite for development.
- **Dependencies**:
    - `axios` for HTTP requests.
    - `sockjs-client@1.5.2` and `@stomp/stompjs` for STOMP over WebSocket.
- **Key Components**:
    - `Chat.tsx`: Handles guest and agent chat logic, including STOMP WebSocket connections, message sending, and UI rendering.
    - `Login.tsx`: Manages agent login with Basic Auth.
    - `App.tsx`: Routes between login and chat views.
- **WebSocket**:
    - Connects to `http://localhost:8080/chat` using SockJS.
    - Subscribes to `/user/{userId}/queue/messages` for chat messages and `/user/{guestId}/queue/assignment` for guest agent assignment updates.
    - Retries failed connections up to 5 times with exponential backoff.
- **HTTP Requests**:
    - `POST /api/auth/anonymous`: Creates guest users.
    - `GET /api/assignment/{userId}`: Fetches assigned guest or agent.
    - `POST /api/messages`: Saves messages.
    - `POST /api/assignment/free/{agentId}`: Frees agents on logout.

### Backend
- **Framework**: Spring Boot with Spring Web, Spring WebSocket, Spring Security, and Spring Data JPA.
- **Database**: H2 in-memory database for storing users and messages.
- **Dependencies**:
    - `spring-boot-starter-web`, `spring-boot-starter-websocket`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`.
    - `h2` for the database.
- **Key Components**:
    - **Controllers**:
        - `AuthController.java`: Handles anonymous guest creation and agent login.
        - `MessageController.java`: Manages message persistence and STOMP message routing.
        - `AssignmentController.java`: Provides endpoints for agent-guest assignments.
    - **Services**:
        - `AgentAssignmentService.java`: Manages agent-guest assignments in memory, notifies guests of assignments via STOMP.
    - **WebSocket**:
        - `WebSocketConfig.java`: Configures STOMP endpoint `/chat` with SockJS fallback and CORS for `http://localhost:3000`.
        - `ChatWebSocketHandler.java`: Handles raw WebSocket connections (fallback).
    - **Security**:
        - `SecurityConfig.java`: Enables Basic Auth, permits `/api/auth/anonymous` and `/chat/**`, requires authentication for other endpoints.
- **Endpoints**:
    - `POST /api/auth/anonymous`: Creates a guest and assigns an agent.
    - `POST /api/auth/login`: Authenticates agents.
    - `GET /api/messages?userId={userId}`: Retrieves message history.
    - `POST /api/messages`: Saves messages.
    - `GET /api/assignment/{agentId}`: Gets assigned guest for an agent.
    - `GET /api/assignment/guest/{guestId}`: Gets assigned agent for a guest.
    - `POST /api/assignment/free/{agentId}`: Frees an agent.
    - STOMP: `/app/chat` for sending messages, `/user/{userId}/queue/messages` for receiving messages, `/user/{guestId}/queue/assignment` for assignment updates.

### Database Schema
- **USER**: Stores agent credentials (`username`, `password`, `role`).
- **MESSAGE**: Stores chat messages (`id`, `sender`, `recipient`, `content`, `sender_type`, `timestamp`).

## Setup Instructions
### Prerequisites
- Node.js 16+ for frontend.
- Java 17 and Maven for backend.
- `wscat` for WebSocket testing (`npm install -g wscat`).

### Backend Setup
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install dependencies and run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
3. Verify the server is running on `http://localhost:8080`.
4. Access H2 console at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:testdb`, username: `sa`, password: empty).

### Frontend Setup
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install sockjs-client@1.5.2 @stomp/stompjs
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
4. Access the app at `http://localhost:3000`.

## Testing
1. **Guest-Agent Chat**:
    - Open `http://localhost:3000` in two tabs.
    - **Tab 1 (Guest)**: Click “Join as Guest.” If no agent, see “Waiting for agent assignment...”.
    - **Tab 2 (Agent)**: Click “Login as Agent,” enter `agent1`/`password`. See “Assigned to: Guest_XXX”.
    - **Tab 1**: UI updates to “Connected to: agent1”. Send a message.
    - **Tab 2**: Receive and reply to the message.
    - **End Chat**: Guest or agent logs out; agent sees “Waiting for guest assignment...”.
2. **Late Agent Login**:
    - Open guest tab first, then agent tab. Guest UI updates when agent logs in.
3. **WebSocket**:
    - Test STOMP connection:
      ```bash
      wscat -c ws://localhost:8080/chat
      ```
      Send: `{"type":"subscribe","userId":"agent1"}`.
    - Check Network tab for STOMP frames (`CONNECT`, `SUBSCRIBE`).
4. **Database**:
    - Query `SELECT * FROM MESSAGE;` in H2 console to verify messages.

## Troubleshooting
- **WebSocket Error**:
    - Check Console for `Chat.tsx: STOMP error:` or `Chat.tsx: WebSocket error:`.
    - Verify `http://localhost:8080/chat/info` in Network tab.
    - Ensure `sockjs-client@1.5.2` and `@stomp/stompjs` are installed.
- **POST 405 Error**:
    - Check Network tab for failing `POST` URL (e.g., `/api/assignment/free/{agentId}`).
    - Verify endpoint in `AssignmentController.java`.
- **Guest Not Connecting**:
    - Confirm STOMP subscription to `/user/{guestId}/queue/assignment`.
    - Check backend logs for `AgentAssignmentService: Assigned agent`.
- **Logs**:
    - Frontend: Console logs (`Chat.tsx`).
    - Backend: `mvn spring-boot:run` output, especially `MessageController`, `AgentAssignmentService`.

## Known Issues Resolved
- **WebSocket Failure**: Fixed by switching to STOMP over SockJS, adding `globalThis` polyfill in `index.html`, and configuring CORS in `WebSocketConfig.java`.
- **POST Method Not Supported**: Resolved by verifying `POST` mappings in `AssignmentController.java` and adding logging to identify failing endpoints.
- **Guest Page Not Refreshing**: Fixed by adding STOMP notifications (`/user/{guestId}/queue/assignment`) in `AgentAssignmentService.java` and subscribing in `Chat.tsx`.

## Future Improvements
- Persist agent assignments in the database for scalability.
- Add message read receipts and typing indicators.
- Support multiple agents with load balancing.
- Implement message encryption for security.
