import type {SseMessageDetail} from './types.ts';
import htmx from "htmx.org";
import "htmx.org/dist/htmax.min.js"
import Alpine from "alpinejs";

const sessionId: string = crypto.randomUUID();

declare global {
    interface Window {
        Alpine: typeof Alpine;
        handleHeartbeat: (event: any) => void;
    }
}

window.Alpine = Alpine;
window.handleHeartbeat = handleHeartbeat;

Alpine.start();

document.addEventListener('htmx:sse:before:connection', handleBeforeConnection as EventListener);
document.addEventListener('htmx:sse:after:connection', handleAfterConnection as EventListener);
document.addEventListener('htmx:sse:before:message', handleBeforeMessage as EventListener);
document.addEventListener('htmx:sse:after:message', handleAfterMessage as EventListener);
document.addEventListener('htmx:sse:close', handleClose as EventListener);
document.addEventListener('htmx:sse:error', handleError as EventListener);

function handleBeforeConnection(event: CustomEvent<SseMessageDetail>) {
    console.log(event.detail);
}

function handleAfterConnection(event: CustomEvent<SseMessageDetail>) {
    console.log(event.detail)
}

function handleBeforeMessage(event: CustomEvent<SseMessageDetail>) {
    console.log(event.detail)
}

function handleAfterMessage(event: CustomEvent<SseMessageDetail>) {
    console.log(event.detail)
}

function handleClose(event: CustomEvent<SseMessageDetail>) {
    console.log(event.detail);
}

function handleError(event: CustomEvent<SseMessageDetail>) {
    console.log(event.detail)
}

function handleConfigRequest(event: any) {
    event.detail.ctx.request.headers['X-Session-Id'] = sessionId
}

document.addEventListener('htmx:config:request', handleConfigRequest as EventListener);

function handleHeartbeat(event : any) {
    console.log(event)
}

