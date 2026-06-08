package cdphandler;

public class TraceViewerTemplate {
    public static final String HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Wheel3 Trace Viewer</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">
    <script src="trace-data.js"></script>
    <style>
        :root {
            --bg-base: #090d16;
            --bg-surface: #111827;
            --bg-panel: #1f2937;
            --border-color: #374151;
            --text-primary: #f3f4f6;
            --text-secondary: #9ca3af;
            --accent-blue: #3b82f6;
            --accent-green: #10b981;
            --accent-red: #ef4444;
            --accent-purple: #8b5cf6;
            --accent-orange: #f59e0b;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Inter', sans-serif;
            background-color: var(--bg-base);
            color: var(--text-primary);
            height: 100vh;
            overflow: hidden;
            display: flex;
            flex-direction: column;
        }

        header {
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-color);
            padding: 12px 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            z-index: 10;
        }

        .logo-section {
            display: flex;
            align-items: center;
            gap: 12px;
        }

        .logo-section h1 {
            font-size: 1.25rem;
            font-weight: 700;
            letter-spacing: 0.05em;
            background: linear-gradient(135deg, #60a5fa, #3b82f6);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        .badge {
            font-size: 0.75rem;
            font-weight: 600;
            padding: 4px 8px;
            border-radius: 9999px;
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .badge-pass {
            background-color: rgba(16, 185, 129, 0.15);
            color: var(--accent-green);
            border: 1px solid rgba(16, 185, 129, 0.3);
        }

        .badge-fail {
            background-color: rgba(239, 68, 68, 0.15);
            color: var(--accent-red);
            border: 1px solid rgba(239, 68, 68, 0.3);
        }

        .meta-group {
            display: flex;
            gap: 24px;
            font-size: 0.85rem;
            color: var(--text-secondary);
        }

        .meta-item strong {
            color: var(--text-primary);
        }

        main {
            flex: 1;
            display: flex;
            overflow: hidden;
        }

        /* Sidebar Actions List */
        .sidebar {
            width: 320px;
            background-color: var(--bg-surface);
            border-right: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            overflow: hidden;
            flex-shrink: 0;
        }

        .sidebar-header {
            padding: 16px;
            border-bottom: 1px solid var(--border-color);
            font-weight: 600;
            font-size: 0.9rem;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.05em;
        }

        .action-list {
            flex: 1;
            overflow-y: auto;
            list-style: none;
        }

        .action-item {
            padding: 12px 16px;
            border-bottom: 1px solid rgba(55, 65, 81, 0.5);
            cursor: pointer;
            transition: all 0.2s ease;
            position: relative;
        }

        .action-item:hover {
            background-color: rgba(59, 130, 246, 0.05);
        }

        .action-item.active {
            background-color: rgba(59, 130, 246, 0.12);
            border-left: 3px solid var(--accent-blue);
        }

        .action-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 4px;
        }

        .action-name {
            font-weight: 600;
            font-size: 0.85rem;
            color: var(--text-primary);
        }

        .action-duration {
            font-size: 0.75rem;
            color: var(--text-secondary);
        }

        .action-target {
            font-size: 0.75rem;
            color: var(--text-secondary);
            font-family: 'JetBrains Mono', monospace;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            margin-top: 2px;
        }

        .status-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;
            display: inline-block;
        }

        .status-dot-success { background-color: var(--accent-green); }
        .status-dot-failed { background-color: var(--accent-red); }

        /* Detail Workspace */
        .workspace {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        /* Timeline / Film strip */
        .timeline-container {
            height: 120px;
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            overflow: hidden;
            flex-shrink: 0;
        }

        .timeline-header {
            padding: 8px 16px;
            font-size: 0.75rem;
            color: var(--text-secondary);
            font-weight: 600;
            text-transform: uppercase;
            border-bottom: 1px solid rgba(55, 65, 81, 0.4);
        }

        .timeline-filmstrip {
            flex: 1;
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 16px;
            overflow-x: auto;
            scroll-behavior: smooth;
        }

        .filmstrip-thumb {
            height: 60px;
            width: 100px;
            border-radius: 4px;
            border: 2px solid var(--border-color);
            cursor: pointer;
            object-fit: cover;
            transition: all 0.2s ease;
            opacity: 0.6;
            flex-shrink: 0;
        }

        .filmstrip-thumb:hover {
            opacity: 0.9;
            border-color: var(--text-secondary);
        }

        .filmstrip-thumb.active {
            opacity: 1;
            border-color: var(--accent-blue);
            box-shadow: 0 0 8px rgba(59, 130, 246, 0.4);
        }

        .filmstrip-item-wrapper {
            position: relative;
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .filmstrip-label {
            font-size: 0.65rem;
            color: var(--text-secondary);
            margin-top: 4px;
        }

        /* View Panels */
        .viewer-container {
            flex: 1;
            display: flex;
            overflow: hidden;
        }

        .content-panel {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            background-color: var(--bg-base);
        }

        /* Tab bar */
        .tab-bar {
            display: flex;
            background-color: var(--bg-surface);
            border-bottom: 1px solid var(--border-color);
            padding: 0 16px;
        }

        .tab-btn {
            background: none;
            border: none;
            color: var(--text-secondary);
            padding: 12px 16px;
            cursor: pointer;
            font-size: 0.85rem;
            font-weight: 500;
            border-bottom: 2px solid transparent;
            transition: all 0.2s ease;
        }

        .tab-btn:hover {
            color: var(--text-primary);
        }

        .tab-btn.active {
            color: var(--accent-blue);
            border-bottom-color: var(--accent-blue);
            font-weight: 600;
        }

        /* Tab Contents */
        .tab-content {
            display: none;
            flex: 1;
            overflow: hidden;
            position: relative;
        }

        .tab-content.active {
            display: flex;
            flex-direction: column;
        }

        /* Viewport Screenshot Tab */
        .screenshot-viewer {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 24px;
            overflow: auto;
            position: relative;
        }

        .screenshot-controls {
            position: absolute;
            top: 16px;
            left: 16px;
            display: flex;
            gap: 8px;
            background-color: rgba(17, 24, 39, 0.85);
            backdrop-filter: blur(8px);
            padding: 4px;
            border-radius: 8px;
            border: 1px solid var(--border-color);
            z-index: 5;
        }

        .control-btn {
            background: none;
            border: none;
            color: var(--text-secondary);
            padding: 6px 12px;
            font-size: 0.75rem;
            font-weight: 600;
            border-radius: 6px;
            cursor: pointer;
            transition: all 0.2s ease;
        }

        .control-btn.active {
            background-color: var(--accent-blue);
            color: var(--text-primary);
        }

        .screenshot-frame {
            position: relative;
            display: inline-block;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
            border-radius: 6px;
            border: 1px solid var(--border-color);
            overflow: hidden;
        }

        .screenshot-img {
            max-height: 55vh;
            max-width: 100%;
            display: block;
            object-fit: contain;
        }

        .element-highlight {
            position: absolute;
            border: 2px dashed var(--accent-red);
            background-color: rgba(239, 68, 68, 0.15);
            box-shadow: 0 0 12px var(--accent-red);
            pointer-events: none;
            z-index: 2;
            transition: all 0.15s ease-out;
        }

        /* Source Code Tab */
        .source-viewer {
            flex: 1;
            padding: 20px;
            overflow: auto;
            background-color: #0d1117;
            font-family: 'JetBrains Mono', monospace;
            font-size: 0.85rem;
            line-height: 1.5;
            color: #c9d1d9;
        }

        .source-pre {
            white-space: pre-wrap;
            word-break: break-all;
        }

        /* Console and Network Tab */
        .table-container {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
        }

        .log-table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.8rem;
            text-align: left;
        }

        .log-table th, .log-table td {
            padding: 10px 12px;
            border-bottom: 1px solid rgba(55, 65, 81, 0.5);
        }

        .log-table th {
            color: var(--text-secondary);
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.75rem;
            background-color: var(--bg-surface);
            position: sticky;
            top: 0;
            z-index: 1;
        }

        .log-table tr:hover {
            background-color: rgba(255, 255, 255, 0.02);
        }

        .log-level-error { color: var(--accent-red); font-weight: 600; }
        .log-level-warning { color: var(--accent-orange); font-weight: 600; }
        .log-level-info { color: var(--accent-blue); }
        .log-level-log { color: var(--text-primary); }

        .net-status-success { color: var(--accent-green); font-weight: 600; }
        .net-status-redirect { color: var(--accent-orange); }
        .net-status-error { color: var(--accent-red); font-weight: 600; }

        .network-split {
            display: flex;
            flex: 1;
            overflow: hidden;
            height: 100%;
        }

        .network-table-pane {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            min-width: 0;
        }

        .network-response-panel {
            width: 380px;
            flex-shrink: 0;
            border-left: 1px solid var(--border-color);
            background-color: var(--bg-surface);
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .network-response-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 16px;
            font-size: 0.8rem;
            font-weight: 600;
            color: var(--text-secondary);
            text-transform: uppercase;
            letter-spacing: 0.05em;
            border-bottom: 1px solid var(--border-color);
        }

        .net-resp-content {
            flex: 1;
            overflow-y: auto;
            display: none;
        }

        .net-resp-content.active {
            display: flex;
            flex-direction: column;
        }

        .net-row-selected {
            background-color: rgba(59, 130, 246, 0.12) !important;
            border-left: 3px solid var(--accent-blue);
        }

        .net-row-has-body td:last-child::after {
            content: ' ↗';
            font-size: 0.7rem;
            color: var(--accent-blue);
        }

        /* Right Panel: Action Metadata & Details */
        .detail-panel {
            width: 340px;
            background-color: var(--bg-surface);
            border-left: 1px solid var(--border-color);
            display: flex;
            flex-direction: column;
            overflow-y: auto;
            padding: 24px;
            flex-shrink: 0;
        }

        .detail-section {
            margin-bottom: 24px;
        }

        .detail-section-title {
            font-size: 0.75rem;
            color: var(--text-secondary);
            font-weight: 600;
            text-transform: uppercase;
            margin-bottom: 12px;
            letter-spacing: 0.05em;
            border-bottom: 1px solid rgba(55, 65, 81, 0.5);
            padding-bottom: 6px;
        }

        .detail-row {
            display: flex;
            flex-direction: column;
            gap: 4px;
            margin-bottom: 12px;
        }

        .detail-label {
            font-size: 0.75rem;
            color: var(--text-secondary);
        }

        .detail-value {
            font-size: 0.85rem;
            font-weight: 500;
            word-break: break-all;
        }

        .detail-value-code {
            font-family: 'JetBrains Mono', monospace;
            background-color: var(--bg-panel);
            padding: 6px 10px;
            border-radius: 6px;
            border: 1px solid var(--border-color);
            font-size: 0.8rem;
        }

        .error-card {
            background-color: rgba(239, 68, 68, 0.1);
            border: 1px solid rgba(239, 68, 68, 0.3);
            border-radius: 8px;
            padding: 16px;
            color: #fca5a5;
            font-size: 0.8rem;
            font-family: 'JetBrains Mono', monospace;
            line-height: 1.5;
            margin-bottom: 24px;
        }

        /* Empty State */
        .empty-state {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            flex: 1;
            color: var(--text-secondary);
            gap: 16px;
            padding: 40px;
            text-align: center;
        }

        .empty-state-icon {
            font-size: 3rem;
            opacity: 0.5;
        }

        /* Scrollbar styles */
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }
        ::-webkit-scrollbar-track {
            background: var(--bg-base);
        }
        ::-webkit-scrollbar-thumb {
            background: var(--border-color);
            border-radius: 4px;
        }
        ::-webkit-scrollbar-thumb:hover {
            background: var(--text-secondary);
        }
    </style>
</head>
<body>

    <header>
        <div class="logo-section">
            <h1>WHEEL3 TRACE</h1>
            <span id="overall-status-badge" class="badge badge-pass">Pass</span>
        </div>
        <div class="meta-group">
            <div class="meta-item">Test: <strong id="meta-test-name">-</strong></div>
            <div class="meta-item">Duration: <strong id="meta-duration">0ms</strong></div>
            <div class="meta-item">Actions: <strong id="meta-actions-count">0</strong></div>
        </div>
    </header>

    <main>
        <!-- Left Sidebar: Actions List -->
        <div class="sidebar">
            <div class="sidebar-header">Actions List</div>
            <ul id="action-list-ul" class="action-list">
                <!-- Actions populated dynamically -->
            </ul>
        </div>

        <!-- Right Workspace -->
        <div class="workspace">
            <!-- Timeline filmstrip -->
            <div class="timeline-container">
                <div class="timeline-header">Timeline</div>
                <div id="timeline-filmstrip-div" class="timeline-filmstrip">
                    <!-- Filmstrip dynamically populated -->
                </div>
            </div>

            <!-- Viewers and Details -->
            <div class="viewer-container">
                <div class="content-panel">
                    <div class="tab-bar">
                        <button class="tab-btn active" onclick="switchTab('tab-screenshot')">Screenshot</button>
                        <button class="tab-btn" onclick="switchTab('tab-source')">HTML Source</button>
                        <button class="tab-btn" onclick="switchTab('tab-console')">Console Logs</button>
                        <button class="tab-btn" onclick="switchTab('tab-network')">Network Requests</button>
                    </div>

                    <!-- Screenshot View Tab -->
                    <div id="tab-screenshot" class="tab-content active">
                        <div class="screenshot-viewer">
                            <div class="screenshot-controls">
                                <button id="btn-snap-before" class="control-btn" onclick="switchScreenshotState('before')">Before</button>
                                <button id="btn-snap-after" class="control-btn active" onclick="switchScreenshotState('after')">After</button>
                            </div>
                            <div class="screenshot-frame" id="screenshot-frame-div">
                                <img id="viewport-image" class="screenshot-img" src="" alt="Screenshot" onload="recalculateHighlightOverlay()">
                                <div id="element-highlight-overlay" class="element-highlight"></div>
                            </div>
                            <div id="screenshot-empty" class="empty-state" style="display: none;">
                                <div class="empty-state-icon">🖼️</div>
                                <div>No screenshot available for this step</div>
                            </div>
                        </div>
                    </div>

                    <!-- HTML Source Tab -->
                    <div id="tab-source" class="tab-content">
                        <div id="source-view-div" class="source-viewer">
                            <pre class="source-pre"><code id="source-code-block"></code></pre>
                        </div>
                        <div id="source-empty" class="empty-state" style="display: none;">
                            <div class="empty-state-icon">📄</div>
                            <div>No HTML DOM source captured for this step</div>
                        </div>
                    </div>

                    <!-- Console Logs Tab -->
                    <div id="tab-console" class="tab-content">
                        <div class="table-container">
                            <table class="log-table">
                                <thead>
                                    <tr>
                                        <th style="width: 15%;">Level</th>
                                        <th style="width: 20%;">Timestamp</th>
                                        <th style="width: 65%;">Message</th>
                                    </tr>
                                </thead>
                                <tbody id="console-logs-tbody">
                                    <!-- Populated dynamically -->
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- Network Requests Tab -->
                    <div id="tab-network" class="tab-content">
                        <div class="network-split">
                            <div class="network-table-pane">
                                <table class="log-table">
                                    <thead>
                                        <tr>
                                            <th style="width: 8%;">Method</th>
                                            <th style="width: 12%;">Status</th>
                                            <th style="width: 50%;">URL</th>
                                            <th style="width: 15%;">Type</th>
                                            <th style="width: 15%;">MIME</th>
                                        </tr>
                                    </thead>
                                    <tbody id="network-requests-tbody">
                                        <!-- Populated dynamically -->
                                    </tbody>
                                </table>
                            </div>
                            <div id="network-response-panel" class="network-response-panel" style="display:none;">
                                <div class="network-response-header">
                                    <span id="network-response-title">Response</span>
                                    <button onclick="closeNetworkResponse()" style="background:none;border:none;color:var(--text-secondary);cursor:pointer;font-size:1.1rem;">✕</button>
                                </div>
                                <div class="tab-bar" style="padding:0 12px;">
                                    <button class="tab-btn active" id="net-resp-tab-btn-headers" onclick="switchNetRespTab('headers')">Headers</button>
                                    <button class="tab-btn" id="net-resp-tab-btn-body" onclick="switchNetRespTab('body')">Body</button>
                                </div>
                                <div id="net-resp-headers" class="net-resp-content active">
                                    <div id="net-resp-headers-content" class="source-viewer" style="padding:12px;"></div>
                                </div>
                                <div id="net-resp-body" class="net-resp-content" style="display:none;">
                                    <pre id="net-resp-body-content" class="source-viewer" style="padding:12px;white-space:pre-wrap;word-break:break-all;"></pre>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Right Detail Panel -->
                <div class="detail-panel">
                    <div id="action-error-container"></div>

                    <div class="detail-section">
                        <div class="detail-section-title">Action Details</div>
                        <div class="detail-row">
                            <span class="detail-label">Name</span>
                            <span id="detail-action-name" class="detail-value" style="font-weight: 600;">-</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Target Type</span>
                            <span id="detail-action-type" class="detail-value">-</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Selector / Target</span>
                            <span id="detail-action-target" class="detail-value-code">-</span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Duration</span>
                            <span id="detail-action-duration" class="detail-value">-</span>
                        </div>
                    </div>

                    <div class="detail-section">
                        <div class="detail-section-title">Arguments</div>
                        <div id="detail-action-args" class="detail-value-code">-</div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <script>
        let traceData = window.traceData || {
            title: "Empty Trace",
            startTime: Date.now(),
            endTime: Date.now(),
            duration: 0,
            actions: []
        };

        let activeActionIndex = 0;
        let activeScreenshotState = 'after'; // 'before' | 'after'

        function init() {
            // Update Headers
            document.getElementById('meta-test-name').textContent = traceData.title;
            document.getElementById('meta-duration').textContent = traceData.duration + 'ms';
            document.getElementById('meta-actions-count').textContent = traceData.actions.length;

            const hasFailure = traceData.actions.some(a => a.status === 'failed');
            const statusBadge = document.getElementById('overall-status-badge');
            if (hasFailure) {
                statusBadge.textContent = 'Fail';
                statusBadge.className = 'badge badge-fail';
            } else {
                statusBadge.textContent = 'Pass';
                statusBadge.className = 'badge badge-pass';
            }

            // Populate Actions
            const ul = document.getElementById('action-list-ul');
            ul.innerHTML = '';
            
            // Populate Timeline
            const filmstrip = document.getElementById('timeline-filmstrip-div');
            filmstrip.innerHTML = '';

            traceData.actions.forEach((action, idx) => {
                // List Item
                const li = document.createElement('li');
                li.className = `action-item ${idx === 0 ? 'active' : ''}`;
                li.onclick = () => selectAction(idx);
                
                const dotClass = action.status === 'failed' ? 'status-dot-failed' : 'status-dot-success';
                const targetText = action.target ? action.target : (action.args && action.args.length > 0 ? JSON.stringify(action.args) : '');

                li.innerHTML = `
                    <div class="action-header">
                        <span class="action-name">
                            <span class="status-dot ${dotClass}"></span>
                            ${action.name}
                        </span>
                        <span class="action-duration">${action.duration}ms</span>
                    </div>
                    <div class="action-target" title="${targetText}">${targetText}</div>
                `;
                ul.appendChild(li);

                // Filmstrip item (only if it has screenshots)
                const screenshotFile = action.screenshotAfter || action.screenshotBefore;
                if (screenshotFile) {
                    const wrap = document.createElement('div');
                    wrap.className = 'filmstrip-item-wrapper';

                    const img = document.createElement('img');
                    img.className = `filmstrip-thumb ${idx === 0 ? 'active' : ''}`;
                    img.src = screenshotFile;
                    img.alt = `Step ${idx + 1}`;
                    img.onclick = () => selectAction(idx);

                    const label = document.createElement('span');
                    label.className = 'filmstrip-label';
                    label.textContent = `${action.name} (${action.duration}ms)`;

                    wrap.appendChild(img);
                    wrap.appendChild(label);
                    filmstrip.appendChild(wrap);
                }
            });

            if (traceData.actions.length > 0) {
                selectAction(0);
            }
        }

        function selectAction(index) {
            activeActionIndex = index;
            const action = traceData.actions[index];
            if (!action) return;

            // Update active state in sidebar
            const items = document.querySelectorAll('.action-item');
            items.forEach((item, idx) => {
                if (idx === index) item.classList.add('active');
                else item.classList.remove('active');
            });

            // Update active state in filmstrip
            const thumbs = document.querySelectorAll('.filmstrip-thumb');
            thumbs.forEach((thumb, idx) => {
                const targetActionIdx = findActionIndexFromThumbIndex(idx);
                if (targetActionIdx === index) thumb.classList.add('active');
                else thumb.classList.remove('active');
            });

            // Populate Metadata Sidebar
            document.getElementById('detail-action-name').textContent = action.name;
            document.getElementById('detail-action-type').textContent = action.type || 'driver';
            document.getElementById('detail-action-target').textContent = action.target || 'N/A';
            document.getElementById('detail-action-duration').textContent = action.duration + 'ms';
            document.getElementById('detail-action-args').textContent = action.args && action.args.length > 0 ? JSON.stringify(action.args, null, 2) : '[]';

            // Populate Error Card if failed
            const errorContainer = document.getElementById('action-error-container');
            if (action.status === 'failed' && action.error) {
                errorContainer.innerHTML = `
                    <div class="detail-section-title" style="color: var(--accent-red)">Error Details</div>
                    <div class="error-card">${action.error}</div>
                `;
            } else {
                errorContainer.innerHTML = '';
            }

            // Populate Screenshot View
            updateScreenshotDisplay();

            // Populate HTML DOM Source View
            updateSourceDisplay();

            // Populate Console Logs (Filter by action duration, or show logs occurring during this action)
            updateConsoleLogs();

            // Populate Network Requests
            updateNetworkRequests();
        }

        // Mapping helper: filmstrip might not contain all elements if some don't have screenshots.
        // But in our case we only add thumbnail if screenshot exists.
        function findActionIndexFromThumbIndex(thumbIdx) {
            let foundThumbCount = 0;
            for (let i = 0; i < traceData.actions.length; i++) {
                if (traceData.actions[i].screenshotAfter || traceData.actions[i].screenshotBefore) {
                    if (foundThumbCount === thumbIdx) return i;
                    foundThumbCount++;
                }
            }
            return 0;
        }

        function updateScreenshotDisplay() {
            const action = traceData.actions[activeActionIndex];
            const beforeBtn = document.getElementById('btn-snap-before');
            const afterBtn = document.getElementById('btn-snap-after');
            const frameDiv = document.getElementById('screenshot-frame-div');
            const emptyDiv = document.getElementById('screenshot-empty');
            const img = document.getElementById('viewport-image');

            beforeBtn.style.display = action.screenshotBefore ? 'block' : 'none';
            afterBtn.style.display = action.screenshotAfter ? 'block' : 'none';

            // Pick screen to display
            let screenToShow = null;
            if (activeScreenshotState === 'before' && action.screenshotBefore) {
                screenToShow = action.screenshotBefore;
                beforeBtn.classList.add('active');
                afterBtn.classList.remove('active');
            } else if (action.screenshotAfter) {
                screenToShow = action.screenshotAfter;
                afterBtn.classList.add('active');
                beforeBtn.classList.remove('active');
            } else if (action.screenshotBefore) {
                screenToShow = action.screenshotBefore;
                beforeBtn.classList.add('active');
                afterBtn.classList.remove('active');
                activeScreenshotState = 'before';
            }

            if (screenToShow) {
                img.src = screenToShow;
                frameDiv.style.display = 'block';
                emptyDiv.style.display = 'none';
            } else {
                frameDiv.style.display = 'none';
                emptyDiv.style.display = 'flex';
            }
        }

        function switchScreenshotState(state) {
            activeScreenshotState = state;
            updateScreenshotDisplay();
        }

        function updateSourceDisplay() {
            const action = traceData.actions[activeActionIndex];
            const sourceDiv = document.getElementById('source-view-div');
            const emptyDiv = document.getElementById('source-empty');
            const codeBlock = document.getElementById('source-code-block');

            // pageSourceBefore/After are now inline HTML strings, not file paths
            let sourceContent = null;
            if (activeScreenshotState === 'before') {
                sourceContent = action.pageSourceBefore;
            } else {
                sourceContent = action.pageSourceAfter || action.pageSourceBefore;
            }

            if (sourceContent) {
                sourceDiv.style.display = 'block';
                emptyDiv.style.display = 'none';
                codeBlock.textContent = sourceContent;
            } else {
                sourceDiv.style.display = 'none';
                emptyDiv.style.display = 'flex';
            }
        }

        function updateConsoleLogs() {
            const tbody = document.getElementById('console-logs-tbody');
            tbody.innerHTML = '';

            // Show all console logs captured across the entire session
            const logsToShow = traceData.consoleLogs || [];

            if (logsToShow.length === 0) {
                tbody.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--text-secondary);">No console messages captured during this trace</td></tr>`;
                return;
            }

            logsToShow.forEach(log => {
                const tr = document.createElement('tr');
                const time = new Date(log.timestamp).toLocaleTimeString();
                let lvlClass = 'log-level-log';
                if (log.level === 'error') lvlClass = 'log-level-error';
                else if (log.level === 'warning' || log.level === 'warn') lvlClass = 'log-level-warning';
                else if (log.level === 'info') lvlClass = 'log-level-info';

                tr.innerHTML = `
                    <td class="${lvlClass}">${log.level}</td>
                    <td style="color: var(--text-secondary);">${time}</td>
                    <td style="font-family: 'JetBrains Mono', monospace; word-break: break-all;">${log.message}</td>
                `;
                tbody.appendChild(tr);
            });
        }

        function updateNetworkRequests() {
            const tbody = document.getElementById('network-requests-tbody');
            tbody.innerHTML = '';
            closeNetworkResponse();

            // Show all network requests captured across the entire session
            const reqsToShow = traceData.networkRequests || [];

            if (reqsToShow.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-secondary);">No network requests captured during this trace</td></tr>`;
                return;
            }

            reqsToShow.forEach((req, idx) => {
                const tr = document.createElement('tr');
                tr.style.cursor = 'pointer';
                let statusClass = 'net-status-success';
                if (typeof req.status === 'number') {
                    if (req.status >= 400) statusClass = 'net-status-error';
                    else if (req.status >= 300) statusClass = 'net-status-redirect';
                }
                const hasBody = !!req.responseBody;
                if (hasBody) tr.classList.add('net-row-has-body');

                const shortUrl = req.url ? (req.url.length > 60 ? '...' + req.url.slice(-57) : req.url) : '';
                tr.innerHTML = `
                    <td style="font-weight:600;font-family:'JetBrains Mono',monospace;">${req.method || 'GET'}</td>
                    <td class="${statusClass}">${req.status || 'Pending'}</td>
                    <td style="font-family:'JetBrains Mono',monospace;word-break:break-all;" title="${req.url}">${shortUrl}</td>
                    <td style="color:var(--text-secondary);">${req.type || 'fetch'}</td>
                    <td style="color:var(--text-secondary);font-size:0.75rem;">${req.mimeType || ''}</td>
                `;
                tr.onclick = () => openNetworkResponse(tr, req);
                tbody.appendChild(tr);
            });
        }

        function openNetworkResponse(row, req) {
            // Deselect previous
            document.querySelectorAll('#network-requests-tbody tr').forEach(r => r.classList.remove('net-row-selected'));
            row.classList.add('net-row-selected');

            const panel = document.getElementById('network-response-panel');
            panel.style.display = 'flex';
            document.getElementById('network-response-title').textContent = req.url ? req.url.split('/').pop().split('?')[0] || 'Response' : 'Response';

            // Populate headers
            const headersDiv = document.getElementById('net-resp-headers-content');
            const headers = req.responseHeaders || {};
            const headerKeys = Object.keys(headers);
            if (headerKeys.length === 0) {
                headersDiv.innerHTML = '<span style="color:var(--text-secondary);">No response headers captured</span>';
            } else {
                headersDiv.innerHTML = headerKeys.map(k =>
                    `<div style="margin-bottom:6px;"><span style="color:var(--accent-blue);font-family:'JetBrains Mono',monospace;font-size:0.8rem;">${escHtml(k)}: </span><span style="font-family:'JetBrains Mono',monospace;font-size:0.8rem;color:var(--text-primary);">${escHtml(headers[k])}</span></div>`
                ).join('');
            }

            // Populate body
            const bodyPre = document.getElementById('net-resp-body-content');
            if (req.responseBody) {
                let bodyText = req.responseBody;
                // Try to pretty-print JSON
                try {
                    const parsed = JSON.parse(bodyText);
                    bodyText = JSON.stringify(parsed, null, 2);
                } catch(e) { /* not JSON, use raw */ }
                bodyPre.textContent = bodyText;
                document.getElementById('net-resp-tab-btn-body').style.opacity = '1';
            } else {
                bodyPre.textContent = 'No response body captured (only XHR/Fetch bodies are recorded)';
                document.getElementById('net-resp-tab-btn-body').style.opacity = '0.4';
            }

            switchNetRespTab('headers');
        }

        function closeNetworkResponse() {
            document.getElementById('network-response-panel').style.display = 'none';
            document.querySelectorAll('#network-requests-tbody tr').forEach(r => r.classList.remove('net-row-selected'));
        }

        function switchNetRespTab(tab) {
            document.getElementById('net-resp-headers').style.display = tab === 'headers' ? 'flex' : 'none';
            document.getElementById('net-resp-body').style.display = tab === 'body' ? 'flex' : 'none';
            document.getElementById('net-resp-tab-btn-headers').classList.toggle('active', tab === 'headers');
            document.getElementById('net-resp-tab-btn-body').classList.toggle('active', tab === 'body');
        }

        function escHtml(str) {
            return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        }

        function switchTab(tabId) {
            const tabs = document.querySelectorAll('.tab-content');
            const btns = document.querySelectorAll('.tab-btn');
            
            tabs.forEach(tab => {
                if (tab.id === tabId) tab.classList.add('active');
                else tab.classList.remove('active');
            });

            btns.forEach(btn => {
                if (btn.getAttribute('onclick').includes(tabId)) btn.classList.add('active');
                else btn.classList.remove('active');
            });

            if (tabId === 'tab-source') {
                updateSourceDisplay();
            }
        }

        function recalculateHighlightOverlay() {
            const action = traceData.actions[activeActionIndex];
            const overlay = document.getElementById('element-highlight-overlay');
            const img = document.getElementById('viewport-image');
            
            if (!action || !action.elementRect || !action.elementRect.width || !action.elementRect.height) {
                overlay.style.display = 'none';
                return;
            }

            // Bounding box: { x, y, width, height }
            const rect = action.elementRect;
            
            // Wait for image dimensions to load properly
            if (img.naturalWidth === 0) {
                overlay.style.display = 'none';
                return;
            }

            const scaleX = img.clientWidth / img.naturalWidth;
            const scaleY = img.clientHeight / img.naturalHeight;

            // Offset alignment if image is centered inside screenshot-frame
            // Note: Since display is inline-block and image is the only element, 
            // the frame wraps the image perfectly, so style left/top maps to the image left/top.
            overlay.style.left = (rect.x * scaleX) + 'px';
            overlay.style.top = (rect.y * scaleY) + 'px';
            overlay.style.width = (rect.width * scaleX) + 'px';
            overlay.style.height = (rect.height * scaleY) + 'px';
            overlay.style.display = 'block';
        }

        window.addEventListener('resize', recalculateHighlightOverlay);
        
        // Initialize on load
        window.onload = init;
    </script>
</body>
</html>
""";
}
