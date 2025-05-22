import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';
import type {Message} from '../types';

interface ChatProps {
    userType: 'GUEST' | 'SUPPORT_AGENT';
    setUserType: (type: 'GUEST' | 'SUPPORT_AGENT' | null) => void;
}

const Chat: React.FC<ChatProps> = ({ userType, setUserType }) => {
    const [userId, setUserId] = useState<string>('');
    const [recipientId, setRecipientId] = useState<string>('');
    const [message, setMessage] = useState<string>('');
    const [messages, setMessages] = useState<Message[]>([]);
    const [stompClient, setStompClient] = useState<Client | null>(null);
    const [error, setError] = useState<string>('');
    const [retryCount, setRetryCount] = useState(0);
    const maxRetries = 5;
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Initialize user and set recipient
    useEffect(() => {
        if (userType === 'GUEST') {
            axios.post('http://localhost:8080/api/auth/anonymous', {}, { withCredentials: true })
                .then(response => {
                    const guestId = response.data.guestId;
                    const agentId = response.data.agentId;
                    if (!guestId) {
                        setError('Failed to initialize chat: Invalid guest ID');
                        return;
                    }
                    setUserId(guestId);
                    setRecipientId(agentId || '');
                    fetchMessages(guestId);
                })
                .catch(err => {
                    console.error('Chat.tsx: Error creating anonymous user:', err);
                    if (err.response?.status === 503) {
                        setError('No agents available. Waiting for an agent....');
                    } else if (err.response?.status === 401) {
                        setError('Unauthorized access to anonymous endpoint.');
                    } else {
                        setError('Failed to connect to server. Possible CORS issue or server is down.');
                    }
                });
        } else {
            const username = axios.defaults.auth?.username || 'agent1';
            setUserId(username);
            axios.get(`http://localhost:8080/api/assignment/${username}`, { withCredentials: true })
                .then(response => {
                    setRecipientId(response.data.guestId || '');
                    fetchMessages(username);
                })
                .catch(error => {
                    console.error('Chat.tsx: Error fetching assigned guest:', error);
                    setError('No guest assigned yet.');
                });
        }
    }, [userType]);

    // Fetch message history
    const fetchMessages = (userId: string) => {
        axios.get(`http://localhost:8080/api/messages?userId=${userId}`, { withCredentials: true })
            .then(response => {
                setMessages(response.data);
            })
            .catch(error => {
                console.error('Chat.tsx: Error fetching messages:', error);
                if (error.response?.status === 500) {
                    setError('Server error while fetching messages.');
                } else {
                    setError('Failed to fetch messages. Possible CORS issue or server is down.');
                }
            });
    };

    // Connect to WebSocket with STOMP
    useEffect(() => {
        if (!userId || retryCount >= maxRetries) return;

        const socket = new SockJS('http://localhost:8080/chat');
        const client = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 3000 * retryCount,
            debug: (str) => console.debug('Chat.tsx: STOMP:', str),
        });

        client.onConnect = () => {
            setError('');
            client.subscribe(`/user/${userId}/queue/messages`, (msg) => {
                try {
                    const newMessage: Message = JSON.parse(msg.body);
                    setMessages((prev) => [...prev, newMessage]);
                    if (userType === 'SUPPORT_AGENT' && newMessage.sender !== userId && !recipientId) {
                        setRecipientId(newMessage.sender);
                    }
                } catch (err) {
                    console.error('Chat.tsx: Error parsing message:', err);
                }
            });
            if (userType === 'GUEST') {
                client.subscribe(`/user/${userId}/queue/assignment`, (msg) => {
                    try {
                        const assignment = JSON.parse(msg.body);
                        setRecipientId(assignment.agentId || '');
                        if (assignment.agentId) {
                            setError('');
                            fetchMessages(userId);
                        } else {
                            setError('No agents available. Waiting for an agent...');

                        }
                    } catch (err) {
                        console.error('Chat.tsx: Error parsing assignment:', err);
                    }
                });
            } else {
                client.subscribe(`/user/${userId}/queue/assignment`, (msg) => {
                    try {
                        const assignment = JSON.parse(msg.body);
                        setRecipientId(assignment.agentId || '');
                        if (assignment.agentId) {
                            setError('');
                            fetchMessages(userId);
                        } else {
                            setError('No guests available. Waiting for a guest...');
                        }
                    } catch (err) {
                        console.error('Chat.tsx: Error parsing assignment:', err);
                    }
                });
            }
            setRetryCount(0);
        };

        client.onStompError = (frame) => {
            console.error('Chat.tsx: STOMP error:', frame);
            setError(`WebSocket connection failed (Retry ${retryCount + 1}/${maxRetries}). Check server at http://localhost:8080/chat.`);
            setRetryCount((prev) => prev + 1);
        };

        client.onWebSocketError = (error) => {
            console.error('Chat.tsx: WebSocket error:', error);
            setError(`WebSocket connection failed (Retry ${retryCount + 1}/${maxRetries}).`);
            setRetryCount((prev) => prev + 1);
        };

        client.onWebSocketClose = (event) => {
            console.debug('Chat.tsx: WebSocket closed, code:', event.code, 'reason:', event.reason);
            setError('WebSocket connection closed. Retrying...');
            setRetryCount((prev) => prev + 1);
        };

        client.activate();
        setStompClient(client);

        return () => {
            if (client) {
                client.deactivate();
            }
        };
    }, [userId, retryCount]);

    // Retry WebSocket connection
    useEffect(() => {
        if (retryCount > 0 && retryCount < maxRetries) {
            const timeout = setTimeout(() => {
            }, 3000 * retryCount);
            return () => clearTimeout(timeout);
        }
    }, [retryCount]);

    // Scroll to bottom
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const sendMessage = async (e: React.MouseEvent<HTMLButtonElement>) => {
        e.preventDefault();
        if (!message.trim()) {
            setError('Message cannot be empty.');
            return;
        }
        if (!stompClient || !stompClient.connected) {
            setError('Cannot send message: WebSocket is not connected.');
            return;
        }
        if (!recipientId) {
            setError('Cannot send message: No recipient assigned.');
            return;
        }
        const newMessage: Message = {
            sender: userId,
            recipient: recipientId,
            content: message,
            senderType: userType,
            timestamp: new Date().toISOString(),
        };
        try {
            // Send via STOMP
            stompClient.publish({
                destination: '/app/chat',
                body: JSON.stringify(newMessage),
            });
            setMessages((prev) => [...prev]);
            setMessage('');
            setError('');
        } catch (err: any) {
            console.error('Chat.tsx: Error sending message:', err);
            setError('Failed to send message: ' + (err.response?.data || err.message));
        }
    };

    const handleLogout = () => {
        if (userType === 'SUPPORT_AGENT') {
            axios.post(`http://localhost:8080/api/assignment/free/${userId}`, {}, { withCredentials: true })
                .then(() => {
                    console.debug('Chat.tsx: Agent freed, user queued');
                })
                .catch(err => {
                    console.error('Chat.tsx: Error freeing agent:', err);
                    setError('Failed to free agent: ' + (err.response?.data || err.message));
                });
        } else {
            axios.post(`http://localhost:8080/api/assignment/guest/${userId}`, {}, { withCredentials: true })
                .then(() => {
                    console.debug('Chat.tsx: User Closed, Agent freed');
                })
                .catch(err => {
                    console.error('Chat.tsx: Error freeing agent:', err);
                    setError('Failed to free agent: ' + (err.response?.data || err.message));
                });
        }
        setUserType(null);
    };

    return (
        <div className="w-full max-w-md bg-white rounded-lg shadow-lg p-6">
            <h1 className="text-2xl font-bold text-center mb-4">
                {userType === 'GUEST' ? 'Guest Chat' : 'Agent Dashboard'}
            </h1>
            {error && <p className="text-red-500 mb-4">{error}</p>}
            {userType === 'SUPPORT_AGENT' && !recipientId && (
                <p className="text-gray-500 mb-4">Waiting for guest assignment...</p>
            )}
            {userType === 'SUPPORT_AGENT' && recipientId && (
                <p className="text-gray-500 mb-4">Assigned to: {recipientId}</p>
            )}
            {userType === 'GUEST' && !recipientId && (
                <p className="text-gray-500 mb-4">Waiting for agent assignment...</p>
            )}
            {userType === 'GUEST' && recipientId && (
                <p className="text-gray-500 mb-4">Connected to: {recipientId}</p>
            )}
            <div className="h-96 overflow-y-auto mb-4 p-4 border rounded bg-gray-50">
                {messages.map((msg, index) => (
                    <div
                        key={index}
                        className={`mb-2 ${msg.sender === userId ? 'text-right' : 'text-left'}`}
                    >
                        <span className="font-bold">{msg.sender}: </span>
                        <span>{msg.content}</span>
                        <div className="text-xs text-gray-500">
                            {new Date(msg.timestamp).toLocaleTimeString()}
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>
            <div className="flex">
                <input
                    type="text"
                    placeholder="Type a message"
                    className="flex-1 p-2 border rounded-l"
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                />
                <button
                    className="bg-blue-500 text-white p-2 rounded-r hover:bg-blue-600"
                    onClick={sendMessage}
                >
                    Send
                </button>
            </div>
            <button
                className="w-full mt-2 bg-gray-500 text-white p-2 rounded hover:bg-gray-600"
                onClick={handleLogout}
            >
                Logout
            </button>
        </div>
    );
};

export default Chat;