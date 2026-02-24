// src/main/resources/static/js/main.js

const sessionId = crypto.randomUUID();

const body = document.getElementById("html-body");
if (body) {
    body.setAttribute("sse-connect", `/api/v1/sse/subscribe?sessionId=${sessionId}`);
}

// Automatically inject the session ID header into EVERY HTMX request (POST, GET, etc.)
document.addEventListener('htmx:configRequest', (event) => {
    event.detail.headers['X-Session-Id'] = sessionId;
});

// Update SSE status badge helper
function updateSseStatus(status, text, badgeClass) {
    const badge = document.getElementById('sse-status-badge');
    const textEl = document.getElementById('sse-status-text');
    const dotEl = document.getElementById('sse-status-dot');
    if (!badge || !textEl || !dotEl) return;

    badge.className = `badge gap-2 py-3 px-3 shadow-sm font-medium text-xs ${badgeClass}`;
    textEl.textContent = text;

    if (status === 'connected') {
        dotEl.className = 'h-2 w-2 rounded-full bg-success animate-pulse';
    } else if (status === 'connecting') {
        dotEl.className = 'h-2 w-2 rounded-full bg-warning animate-ping';
    } else {
        dotEl.className = 'h-2 w-2 rounded-full bg-error';
    }
}

// 1. Connection successfully established
document.body.addEventListener('htmx:sseOpen', function (e) {
    console.log('[HTMX SSE] Connection opened successfully:', {
        element: e.detail.elt,
        source: e.detail.source
    });
    updateSseStatus('connected', 'Live SSE Connected', 'badge-soft badge-success border border-success/30');
});

// 2. Connection error or failed to establish
document.body.addEventListener('htmx:sseError', function (e) {
    console.error('[HTMX SSE] Connection error encountered:', {
        error: e.detail.error,
        source: e.detail.source
    });
    updateSseStatus('error', 'SSE Disconnected', 'badge-soft badge-error border border-error/30');
});

// 3. Just before an SSE event payload is swapped into the DOM
document.body.addEventListener('htmx:sseBeforeMessage', function (e) {
    const emptyState = document.getElementById('fibonacci-empty-state');
    if (emptyState) {
        emptyState.style.display = 'none';
    }
});

// 4. After the SSE event data has been successfully swapped in
document.body.addEventListener('htmx:sseMessage', function (e) {
    const streamContainer = document.getElementById('fibonacci-stream-container');
    if (streamContainer) {
        const count = streamContainer.querySelectorAll('.stream-card-appear').length;
        const countBadge = document.getElementById('stream-count-badge');
        if (countBadge) {
            countBadge.textContent = `${count} received`;
            countBadge.classList.remove('hidden');
        }
    }
});

// 5. Connection closed
document.body.addEventListener('htmx:sseClose', function (e) {
    console.warn(`[HTMX SSE] Connection closed. Reason: [${e.detail.type}]`, {
        element: e.detail.elt
    });
    updateSseStatus('closed', 'SSE Closed', 'badge-soft badge-warning border border-warning/30');
});

// Preset helpers
window.setLimit = function(val) {
    const input = document.getElementById('input-limit');
    if (input) input.value = val;
};

window.setDelay = function(val) {
    const input = document.getElementById('input-delay');
    if (input) input.value = val;
};

window.clearStream = function() {
    const streamContainer = document.getElementById('fibonacci-stream-container');
    const emptyState = document.getElementById('fibonacci-empty-state');
    const countBadge = document.getElementById('stream-count-badge');
    if (streamContainer) {
        streamContainer.innerHTML = '';
    }
    if (emptyState) {
        emptyState.style.display = 'flex';
    }
    if (countBadge) {
        countBadge.classList.add('hidden');
        countBadge.textContent = '0 received';
    }
};