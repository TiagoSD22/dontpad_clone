const BACKEND_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080';
const DEBOUNCE_MS = 300;

class DontpadClient {
    constructor() {
        this.ws = null;
        this.currentName = null;
        this.debounceTimer = null;
        this.isUpdatingFromServer = false;
        
        this.initElements();
        this.attachEventListeners();
    }

    initElements() {
        this.nameInput = document.getElementById('dontpadName');
        this.openBtn = document.getElementById('openBtn');
        this.editorSection = document.getElementById('editorSection');
        this.editor = document.getElementById('editor');
        this.currentNameEl = document.getElementById('currentName');
        this.statusIndicator = document.getElementById('statusIndicator');
        this.statusText = document.getElementById('statusText');
    }

    attachEventListeners() {
        this.openBtn.addEventListener('click', () => this.openDontpad());
        this.nameInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.openDontpad();
        });
        
        this.editor.addEventListener('input', () => this.handleEditorChange());
    }

    async openDontpad() {
        const name = this.nameInput.value.trim();
        if (!name) {
            alert('Please enter a dontpad name');
            return;
        }

        try {
            // Fetch initial content
            const response = await fetch(`${BACKEND_URL}/dontpad/${name}`);
            const data = await response.json();

            this.currentName = name;
            this.currentNameEl.textContent = `/${name}`;
            this.isUpdatingFromServer = true;
            this.editor.value = data.content || '';
            this.isUpdatingFromServer = false;
            
            // Show editor
            this.editorSection.classList.add('active');
            this.editor.focus();

            // Connect WebSocket
            this.connectWebSocket(name);
        } catch (error) {
            console.error('Error opening dontpad:', error);
            alert('Failed to open dontpad. Is the backend running?');
        }
    }

    connectWebSocket(name) {
        // Close existing connection
        if (this.ws) {
            this.ws.close();
        }

        const wsUrl = `${WS_URL}/ws/${name}`;
        console.log('Connecting to:', wsUrl);
        
        this.ws = new WebSocket(wsUrl);

        this.ws.onopen = () => {
            console.log('WebSocket connected');
            this.updateStatus(true);
        };

        this.ws.onmessage = (event) => {
            try {
                const message = JSON.parse(event.data);
                console.log('Received message:', message.type);

                if (message.type === 'INIT' || message.type === 'CONTENT_UPDATE') {
                    // Update editor without triggering change event
                    this.isUpdatingFromServer = true;
                    
                    // Preserve cursor position
                    const cursorPos = this.editor.selectionStart;
                    const scrollPos = this.editor.scrollTop;
                    
                    this.editor.value = message.content || '';
                    
                    // Restore cursor and scroll position
                    this.editor.selectionStart = cursorPos;
                    this.editor.selectionEnd = cursorPos;
                    this.editor.scrollTop = scrollPos;
                    
                    this.isUpdatingFromServer = false;
                }
            } catch (error) {
                console.error('Error processing message:', error);
            }
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
            this.updateStatus(false);
        };

        this.ws.onclose = () => {
            console.log('WebSocket closed');
            this.updateStatus(false);
            
            // Attempt to reconnect after 3 seconds
            setTimeout(() => {
                if (this.currentName) {
                    console.log('Attempting to reconnect...');
                    this.connectWebSocket(this.currentName);
                }
            }, 3000);
        };
    }

    handleEditorChange() {
        // Don't send updates if we're updating from server
        if (this.isUpdatingFromServer) return;

        // Clear existing timer
        if (this.debounceTimer) {
            clearTimeout(this.debounceTimer);
        }

        // Debounce the update
        this.debounceTimer = setTimeout(() => {
            this.sendUpdate();
        }, DEBOUNCE_MS);
    }

    sendUpdate() {
        if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
            console.warn('WebSocket not ready');
            return;
        }

        const message = {
            type: 'CONTENT_UPDATE',
            content: this.editor.value,
            timestamp: Date.now()
        };

        try {
            this.ws.send(JSON.stringify(message));
            console.log('Sent update');
        } catch (error) {
            console.error('Error sending update:', error);
        }
    }

    updateStatus(connected) {
        if (connected) {
            this.statusIndicator.classList.add('connected');
            this.statusText.textContent = 'Connected';
        } else {
            this.statusIndicator.classList.remove('connected');
            this.statusText.textContent = 'Disconnected';
        }
    }
}

// Initialize client when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => new DontpadClient());
} else {
    new DontpadClient();
}
