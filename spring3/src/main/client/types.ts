export interface SseConnection {
    url: string;
    config: RequestInit;
    lastEventId?: string;
    attempt: number;
    status: number;
    cancelled: boolean;
}

export interface SseConnectionDetail {
    connection: SseConnection;
}

export interface SseMessageDetail {
    connection: SseConnection;
    message: {
        data: string;
        event: string;
        id?: string;
    };
    waitUntil?: (promise: Promise<any>) => void;
    cancelled?: boolean;
}