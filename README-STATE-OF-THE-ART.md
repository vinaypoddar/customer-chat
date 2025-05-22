# JobRad Chat Application - State-of-the-Art Solution with Spring Boot

## Overview
The JobRad Chat Application is a scalable, real-time chat system enabling seamless one-on-one communication between **customers** (Guests & User) and **agents**.
This state-of-the-art solution uses 
- **Next.js** for a server-rendered frontend, 
- **Spring Boot** with **Spring Data JPA** for a robust backend, 
- **Apache Kafka** for event-driven architecture, 
- **WebSocket** with **STOMP** for real-time communication, and 
- **PostgreSQL** for persistent storage. 

Deployed on **Kubernetes**, it ensures high availability, fault tolerance, and a modern user experience with typing indicators and message read receipts.

## Functional Features
- **Customer/Anonymous Guest Access**: Customer/Guests join with a unique ID and are assigned an available agent.
- **Dynamic Agent Assignment**: Agents are assigned to guests via a load-balanced queue, with a waiting state if no agents are available.
- **Real-Time Messaging**: Guests and agents communicate in real-time, with UI updates for typing indicators and read receipts.
- **Agent Reassignment**: Agents are freed upon chat completion (end of chat or session end due to inactivity) and reassigned to new guests.
- **Late Agent Connection**: Guests are notified instantly when an agent becomes available, with seamless UI updates.
- **Rich UI/UX**: Optimistic updates, offline message queuing, and responsive design for mobile and desktop.
- **Scalability**: Supports thousands of concurrent users with horizontal scaling via Kubernetes.

## Technical Architecture
### Architectural Diagram
```
Client ----> LB --> API Gateway --> Auth Service --> User Service --> PostgreSQL [User Database]
(Mobile & Web)         ^  |                           ^
                       |  |                           | Agent
                   ws  |  |                           | Details
           ws          V  V                           |
Agent <----------> Chat Service --> Agent Assingment Service ---> Look For Available Agent [Redis] 
                   |          |                ^
                   |   Kafka  |                |
                   |          V                |
                   |        Customer Waiting Queue
                   |
                   V
                Casandara [Message Database] ------------------------> Archive Service ----------------> S3 [Cold Storage]
                ( 
                  if search needed; then
                    replicate to elasticsearch
                  end
                )

```

### Frontend
- **Framework**: Next.js 14 with TypeScript for server-side rendering (SSR) and static site generation (SSG).
- **State Management**: Redux Toolkit for managing chat messages, assignments, and UI state.
- **Styling**: Tailwind CSS for responsive, utility-first design.
- **Real-Time**: `@stomp/stompjs` and `sockjs-client` for STOMP over WebSocket communication.
- **Dependencies**:
    - `next`, `react`, `redux-toolkit`, `sockjs-client`, `@stomp/stompjs`, `axios`, `tailwindcss`.
- **Key Components**:
    - `pages/chat.tsx`: Manages guest and agent chat UI, WebSocket events, and message sending.
    - `pages/login.tsx`: Agent login with JWT authentication.
    - `store/index.ts`: Redux store for messages, assignments, and UI state.
- **Features**:
    - Optimistic message sending with rollback on failure.
    - Offline message queuing using IndexedDB.
    - Typing indicators via STOMP messages.

### Backend
- **Framework**: Spring Boot 3.3 with Java 17.
- **ORM**: Spring Data JPA with PostgreSQL for data persistence.
- **Event Streaming**: Spring Kafka for asynchronous message and assignment events.
- **Real-Time**: Spring WebSocket with STOMP for WebSocket communication.
- **Authentication**: Spring Security with JWT and refresh tokens for secure agent access.
- **Dependencies**:
    - `spring-boot-starter-web`, `spring-boot-starter-websocket`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-kafka`, `postgresql`, `flyway-core`.
- **Key Components**:
    - **Services**:
        - `Agent Assignment Service`: Manages available agents, customer waiting queues, agent <-> customer assignment mapping, publishes Kafka events, and sends STOMP notifications.
        - `Chat Service`: Handles message persistence and retrieval.
        - `Auth Service`: Manages user authentication and JWT generation.
        - `User Service`: Manages user details and roles.
        - `Notfication Service`: Sends notifications to guests when offline (mobile device) or email when applicable.
        - `Archive Service`: Archive old messages to S3 or to Datalake for analysis etc.
- **Basic Endpoints**:
    - `POST /api/v1/auth/login`: Authenticates agents, returns JWT.
    - `POST /api/v1/auth/register`: Creates users.
    - `GET /api/v1/messages?userId={userId}&lastReadTimestamp={lastReadTimestamp}`: Retrieves message history and the delta.
    - `GET /api/vi/assignments/{userId}`: Gets assigned guest or agent.
    - `POST /api/v1/assignments/agents/{agentId}/free`: Frees an agent and queue the corresponding agent.
    - `POST /api/v1/assignments/customer/{customerId}/free`: Frees a customer and corresponding agent.
- **Kafka Topics**:
    - `messages`: Publishes new messages for persistence and broadcasting (Analytics, Notifications)
    - `assignments`: Publishes agent-customer assignment updates.
- **WebSocket Events**:
    - STOMP destination `/app/chat`: Sends messages.
    - `/user/{userId}/queue/messages`: Delivers messages to users.
    - `/user/{guestId}/queue/assignment`: Notifies guests of assignments.
    - `/user/{userId}/queue/typing`: Emits typing indicators.
    - `/user/{userId}/queue/read`: Confirms read receipts.

### Database
- **Type**: PostgreSQL 16 for scalability and reliability.
- **Schema**:
    - `users`: Stores agent credentials (`id`, `username`, `password`, `role`).
    - `messages`: Stores chat messages (`id`, `sender_id`, `recipient_id`, `content`, `sender_type`, `timestamp`, `read`).


- **Type**: Cassandra for optimised write performance and scalability.
- **Schema**:
    - `messages`: Stores archived messages (`id`, `sender_id`, `recipient_id`, `content`, `sender_type`, `timestamp`).
    - **Indexes**: On `messages(sender_id, recipient_id)`.

- **Type**: Redis for fast retrieval.
- **Schema**:
    - `chat:queue:waiting-users`: Stores customer IDs and their assigned agents.
    - `chat:available-agents`: Stores agent IDs.
    - `chat:session:user -> agent`: Stores user to agent mapping.`
    - `chat:session:agent -> user`: Stores agent to user mapping.

### Infrastructure
- **Containerization**: Docker for consistent deployment.
- **Orchestration**: Kubernetes for scaling and high availability.
- **Monitoring**: Spring Boot Actuator with Prometheus and Grafana for metrics (e.g., WebSocket latency, Kafka throughput).
- **CI/CD**: GitHub Actions for automated testing and deployment.

## Why Spring Boot?
- **Maturity**: Enterprise-grade framework with extensive adoption since 2014.
- **Ecosystem**: Integrates seamlessly with Kafka, PostgreSQL, and Kubernetes.
- **WebSocket**: STOMP provides standardized, scalable real-time communication.
- **Security**: Spring Security offers robust JWT and refresh token support.
- **Performance**: Reactive WebFlux and GraalVM options for high concurrency.

## Future Enhancements
- End-to-end encryption for messages using Spring Security.
- AI-powered chat analytics with Spring AI.
- Multi-language support with realtime translation service.
