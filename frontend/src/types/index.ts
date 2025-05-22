export interface Message {
    id?: number;
    sender: string;
    recipient: string;
    content: string;
    senderType: 'GUEST' | 'SUPPORT_AGENT';
    timestamp: string;
}