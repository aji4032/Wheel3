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
            height: 155px;
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
            height: 55px;
            width: 90px;
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
            font-size: 0.6rem;
            color: var(--text-secondary);
            margin-top: 4px;
            max-width: 90px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
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
            max-height: 50vh;
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
            <!-- Timeline filmstrip with time ruler and hover preview -->
            <div class="timeline-container">
                <div class="timeline-header" style="display: flex; justify-content: space-between; align-items: center;">
                    <span>Timeline</span>
                    <span id="timeline-hover-time" style="font-size: 0.75rem; text-transform: none; color: var(--accent-blue);"></span>
                </div>
                <!-- Interactive time track -->
                <div class="timeline-track-wrapper" style="position: relative; height: 30px; background-color: rgba(31, 41, 55, 0.4); border-bottom: 1px solid var(--border-color); cursor: pointer; user-select: none;" id="timeline-track-div">
                    <!-- Vertical hover cursor line -->
                    <div id="timeline-hover-cursor" style="position: absolute; top: 0; bottom: 0; width: 1px; background-color: var(--accent-blue); display: none; pointer-events: none; z-index: 5;"></div>
                    <!-- Vertical active cursor line -->
                    <div id="timeline-active-cursor" style="position: absolute; top: 0; bottom: 0; width: 2px; background-color: var(--accent-green); pointer-events: none; z-index: 4; left: 0;"></div>
                    
                    <!-- Tick marks -->
                    <div id="timeline-ticks" style="position: absolute; inset: 0; pointer-events: none;"></div>
                    
                    <!-- Hover Preview Tooltip -->
                    <div id="timeline-preview-tooltip" style="position: absolute; bottom: 35px; width: 150px; background-color: var(--bg-surface); border: 1px solid var(--border-color); border-radius: 6px; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.5); display: none; flex-direction: column; align-items: center; padding: 6px; pointer-events: none; z-index: 100;">
                        <img id="timeline-preview-img" style="width: 100%; height: 75px; object-fit: contain; border-radius: 4px; background-color: black;" src="">
                        <span id="timeline-preview-label" style="font-size: 0.65rem; color: var(--text-primary); text-align: center; margin-top: 4px; width: 100%; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;"></span>
                        <span id="timeline-preview-time" style="font-size: 0.65rem; color: var(--text-secondary); margin-top: 2px;"></span>
                    </div>
                </div>
                <!-- Thumbnail filmstrip scroll list -->
                <div id="timeline-filmstrip-div" class="timeline-filmstrip">
                    <!-- Filmstrip dynamically populated -->
                </div>
            </div>

            <!-- Viewers and Details -->
            <div class="viewer-container">
                <div class="content-panel">
                    <div class="tab-bar">
                        <button class="tab-btn active" onclick="switchTab('tab-screenshot')">Screenshot</button>
                        <button class="tab-btn" onclick="switchTab('tab-dom')">DOM Snapshot</button>
                        <button class="tab-btn" onclick="switchTab('tab-source')">HTML Code</button>
                        <button class="tab-btn" onclick="switchTab('tab-testcode')">Test Source</button>
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

                    <!-- Interactive DOM Snapshot Tab -->
                    <div id="tab-dom" class="tab-content">
                        <div style="flex: 1; display: flex; flex-direction: column; height: 100%; position: relative;">
                            <iframe id="dom-snapshot-iframe" style="flex: 1; width: 100%; height: 100%; border: none; background-color: white;" sandbox="allow-same-origin allow-scripts"></iframe>
                        </div>
                        <div id="dom-empty" class="empty-state" style="display: none;">
                            <div class="empty-state-icon">🌐</div>
                            <div>No DOM snapshot available for this step</div>
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

                    <!-- Test Source Tab -->
                    <div id="tab-testcode" class="tab-content">
                        <div id="testcode-view-div" class="source-viewer" style="display: flex; flex-direction: column; gap: 12px; height: 100%;">
                            <div id="testcode-file-path" style="font-size: 0.8rem; color: var(--accent-blue); font-family: 'JetBrains Mono', monospace; padding-bottom: 6px; border-bottom: 1px solid var(--border-color);"></div>
                            <div style="flex: 1; overflow-y: auto;">
                                <pre class="source-pre" style="margin: 0;"><code id="testcode-block" style="display: block;"></code></pre>
                            </div>
                        </div>
                        <div id="testcode-empty" class="empty-state" style="display: none;">
                            <div class="empty-state-icon">💻</div>
                            <div>No test source code snippet captured for this step</div>
                        </div>
                    </div>

                    <!-- Console Logs Tab -->
                    <div id="tab-console" class="tab-content">
                        <div class="tab-sub-header" style="padding: 10px 16px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; background-color: var(--bg-surface);">
                            <span style="font-size: 0.8rem; color: var(--text-secondary); font-weight: 500;">Console Logs</span>
                            <div style="display: flex; gap: 8px; align-items: center;">
                                <label for="console-filter-select" style="font-size: 0.75rem; color: var(--text-secondary);">Filter:</label>
                                <select id="console-filter-select" onchange="updateConsoleLogs()" style="background-color: var(--bg-panel); border: 1px solid var(--border-color); color: var(--text-primary); border-radius: 4px; padding: 4px 8px; font-size: 0.75rem; cursor: pointer; outline: none;">
                                    <option value="action" selected>Only this Action</option>
                                    <option value="all">All Session Logs</option>
                                </select>
                            </div>
                        </div>
                        <div class="table-container" style="flex: 1;">
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
                            <div class="network-table-pane" style="display: flex; flex-direction: column;">
                                <div class="tab-sub-header" style="padding: 10px 16px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; background-color: var(--bg-surface); margin-bottom: 12px;">
                                    <span style="font-size: 0.8rem; color: var(--text-secondary); font-weight: 500;">Network Requests</span>
                                    <div style="display: flex; gap: 8px; align-items: center;">
                                        <label for="network-filter-select" style="font-size: 0.75rem; color: var(--text-secondary);">Filter:</label>
                                        <select id="network-filter-select" onchange="updateNetworkRequests()" style="background-color: var(--bg-panel); border: 1px solid var(--border-color); color: var(--text-primary); border-radius: 4px; padding: 4px 8px; font-size: 0.75rem; cursor: pointer; outline: none;">
                                            <option value="action" selected>Only this Action</option>
                                            <option value="all">All Session Requests</option>
                                        </select>
                                    </div>
                                </div>
                                <div style="flex: 1; overflow-y: auto;">
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
            
            // Populate Timeline Filmstrip
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

            // Initialize time ticks and continuous timeline ruler events
            initTimelineRuler();

            if (traceData.actions.length > 0) {
                selectAction(0);
            }
        }

        function initTimelineRuler() {
            const trackDiv = document.getElementById('timeline-track-div');
            const hoverCursor = document.getElementById('timeline-hover-cursor');
            const hoverTimeSpan = document.getElementById('timeline-hover-time');
            const ticksDiv = document.getElementById('timeline-ticks');
            
            const tooltip = document.getElementById('timeline-preview-tooltip');
            const tooltipImg = document.getElementById('timeline-preview-img');
            const tooltipLabel = document.getElementById('timeline-preview-label');
            const tooltipTime = document.getElementById('timeline-preview-time');

            // Draw ticks for each action
            ticksDiv.innerHTML = '';
            traceData.actions.forEach((action, idx) => {
                const actTime = action.startTime - traceData.startTime;
                const pct = (actTime / traceData.duration) * 100;
                
                const tick = document.createElement('div');
                tick.style.position = 'absolute';
                tick.style.left = pct + '%';
                tick.style.top = '0';
                tick.style.bottom = '0';
                tick.style.width = '2px';
                tick.style.backgroundColor = action.status === 'failed' ? 'var(--accent-red)' : 'var(--accent-blue)';
                tick.style.opacity = '0.7';
                ticksDiv.appendChild(tick);
            });

            // Mouse moves over timeline track
            trackDiv.onmousemove = function(e) {
                const rect = trackDiv.getBoundingClientRect();
                const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
                const timeMs = Math.round(pct * traceData.duration);
                
                // Update text
                hoverTimeSpan.textContent = `Time: ${timeMs}ms`;
                
                // Update cursor line
                hoverCursor.style.left = (pct * 100) + '%';
                hoverCursor.style.display = 'block';

                // Find closest action
                let closestAction = null;
                let minDiff = Infinity;
                let closestIdx = -1;
                
                traceData.actions.forEach((act, idx) => {
                    const actTime = act.startTime - traceData.startTime;
                    const diff = Math.abs(actTime - timeMs);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closestAction = act;
                        closestIdx = idx;
                    }
                });

                if (closestAction) {
                    const screenshot = closestAction.screenshotAfter || closestAction.screenshotBefore;
                    if (screenshot) {
                        tooltipImg.src = screenshot;
                        tooltipImg.style.display = 'block';
                    } else {
                        tooltipImg.style.display = 'none';
                    }
                    tooltipLabel.textContent = closestAction.name;
                    tooltipTime.textContent = (closestAction.startTime - traceData.startTime) + 'ms';
                    
                    // Center tooltip horizontally above track, boundary checks
                    const tooltipWidth = 150;
                    let tooltipLeft = (pct * rect.width) - (tooltipWidth / 2);
                    tooltipLeft = Math.max(0, Math.min(rect.width - tooltipWidth, tooltipLeft));
                    tooltip.style.left = tooltipLeft + 'px';
                    tooltip.style.display = 'flex';
                }
            };

            trackDiv.onmouseleave = function() {
                hoverCursor.style.display = 'none';
                tooltip.style.display = 'none';
                hoverTimeSpan.textContent = '';
            };

            trackDiv.onclick = function(e) {
                const rect = trackDiv.getBoundingClientRect();
                const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
                const timeMs = Math.round(pct * traceData.duration);
                
                let minDiff = Infinity;
                let closestIdx = 0;
                traceData.actions.forEach((act, idx) => {
                    const actTime = act.startTime - traceData.startTime;
                    const diff = Math.abs(actTime - timeMs);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closestIdx = idx;
                    }
                });
                selectAction(closestIdx);
            };
        }

        function updateTimelineActiveCursor() {
            const action = traceData.actions[activeActionIndex];
            if (!action || traceData.duration === 0) return;
            const actTime = action.startTime - traceData.startTime;
            const pct = (actTime / traceData.duration) * 100;
            document.getElementById('timeline-active-cursor').style.left = pct + '%';
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

            // Populate active tab content
            updateActiveTabContent();

            // Update Timeline cursor
            updateTimelineActiveCursor();
        }

        function updateActiveTabContent() {
            // Find currently active tab and update it
            const activeTab = document.querySelector('.tab-content.active');
            if (!activeTab) return;

            if (activeTab.id === 'tab-screenshot') {
                updateScreenshotDisplay();
            } else if (activeTab.id === 'tab-dom') {
                updateDomSnapshot();
            } else if (activeTab.id === 'tab-source') {
                updateSourceDisplay();
            } else if (activeTab.id === 'tab-testcode') {
                updateTestSource();
            } else if (activeTab.id === 'tab-console') {
                updateConsoleLogs();
            } else if (activeTab.id === 'tab-network') {
                updateNetworkRequests();
            }
        }

        // Mapping helper: filmstrip might not contain all elements if some don't have screenshots.
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
            // sync with DOM snapshot state
            updateDomSnapshot();
            updateSourceDisplay();
        }

        function updateDomSnapshot() {
            const action = traceData.actions[activeActionIndex];
            const iframe = document.getElementById('dom-snapshot-iframe');
            const emptyDiv = document.getElementById('dom-empty');

            let sourceContent = null;
            if (activeScreenshotState === 'before') {
                sourceContent = action.pageSourceBefore;
            } else {
                sourceContent = action.pageSourceAfter || action.pageSourceBefore;
            }

            if (sourceContent) {
                iframe.style.display = 'block';
                emptyDiv.style.display = 'none';
                iframe.srcdoc = sourceContent;
            } else {
                iframe.style.display = 'none';
                emptyDiv.style.display = 'flex';
                iframe.srcdoc = '';
            }
        }

        function updateSourceDisplay() {
            const action = traceData.actions[activeActionIndex];
            const sourceDiv = document.getElementById('source-view-div');
            const emptyDiv = document.getElementById('source-empty');
            const codeBlock = document.getElementById('source-code-block');

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

        function updateTestSource() {
            const action = traceData.actions[activeActionIndex];
            const viewDiv = document.getElementById('testcode-view-div');
            const emptyDiv = document.getElementById('testcode-empty');
            const filePathDiv = document.getElementById('testcode-file-path');
            const codeBlock = document.getElementById('testcode-block');

            if (action && action.sourceCode && action.sourceCode.snippet && action.sourceCode.snippet.length > 0) {
                viewDiv.style.display = 'flex';
                emptyDiv.style.display = 'none';
                filePathDiv.textContent = `${action.sourceCode.file}:${action.sourceCode.line} (in ${action.sourceCode.method}())`;

                codeBlock.innerHTML = '';
                action.sourceCode.snippet.forEach(lineItem => {
                    const lineNum = lineItem.line;
                    const isTarget = lineNum === action.sourceCode.line;
                    
                    const lineRow = document.createElement('div');
                    lineRow.style.display = 'flex';
                    lineRow.style.padding = '2px 8px';
                    if (isTarget) {
                        lineRow.style.backgroundColor = 'rgba(59, 130, 246, 0.15)';
                        lineRow.style.borderLeft = '3px solid var(--accent-blue)';
                        lineRow.style.fontWeight = '600';
                    } else {
                        lineRow.style.paddingLeft = '11px'; 
                    }

                    const numSpan = document.createElement('span');
                    numSpan.style.width = '36px';
                    numSpan.style.color = 'var(--text-secondary)';
                    numSpan.style.userSelect = 'none';
                    numSpan.style.marginRight = '12px';
                    numSpan.style.textAlign = 'right';
                    numSpan.textContent = lineNum;

                    const contentSpan = document.createElement('span');
                    contentSpan.style.whiteSpace = 'pre';
                    contentSpan.style.fontFamily = "'JetBrains Mono', monospace";
                    contentSpan.textContent = lineItem.content;
                    if (isTarget) {
                        contentSpan.style.color = '#60a5fa';
                    }

                    lineRow.appendChild(numSpan);
                    lineRow.appendChild(contentSpan);
                    codeBlock.appendChild(lineRow);
                });
            } else {
                viewDiv.style.display = 'none';
                emptyDiv.style.display = 'flex';
            }
        }

        function updateConsoleLogs() {
            const tbody = document.getElementById('console-logs-tbody');
            tbody.innerHTML = '';

            const action = traceData.actions[activeActionIndex];
            const filterSelect = document.getElementById('console-filter-select');
            const filterVal = filterSelect ? filterSelect.value : 'action';

            let logsToShow = traceData.consoleLogs || [];
            if (filterVal === 'action' && action) {
                logsToShow = logsToShow.filter(log => log.timestamp >= action.startTime && log.timestamp <= action.endTime);
            }

            if (logsToShow.length === 0) {
                tbody.innerHTML = `<tr><td colspan="3" style="text-align: center; color: var(--text-secondary);">No console messages captured for this scope</td></tr>`;
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

            const action = traceData.actions[activeActionIndex];
            const filterSelect = document.getElementById('network-filter-select');
            const filterVal = filterSelect ? filterSelect.value : 'action';

            let reqsToShow = traceData.networkRequests || [];
            if (filterVal === 'action' && action) {
                reqsToShow = reqsToShow.filter(req => req.timestamp >= action.startTime && req.timestamp <= action.endTime);
            }

            if (reqsToShow.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-secondary);">No network requests captured for this scope</td></tr>`;
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
            document.querySelectorAll('#network-requests-tbody tr').forEach(r => r.classList.remove('net-row-selected'));
            row.classList.add('net-row-selected');

            const panel = document.getElementById('network-response-panel');
            panel.style.display = 'flex';
            document.getElementById('network-response-title').textContent = req.url ? req.url.split('/').pop().split('?')[0] || 'Response' : 'Response';

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

            const bodyPre = document.getElementById('net-resp-body-content');
            if (req.responseBody) {
                let bodyText = req.responseBody;
                try {
                    const parsed = JSON.parse(bodyText);
                    bodyText = JSON.stringify(parsed, null, 2);
                } catch(e) { }
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

            if (tabId === 'tab-screenshot') {
                updateScreenshotDisplay();
            } else if (tabId === 'tab-dom') {
                updateDomSnapshot();
            } else if (tabId === 'tab-source') {
                updateSourceDisplay();
            } else if (tabId === 'tab-testcode') {
                updateTestSource();
            } else if (tabId === 'tab-console') {
                updateConsoleLogs();
            } else if (tabId === 'tab-network') {
                updateNetworkRequests();
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

            const rect = action.elementRect;
            
            if (img.naturalWidth === 0) {
                overlay.style.display = 'none';
                return;
            }

            const scaleX = img.clientWidth / img.naturalWidth;
            const scaleY = img.clientHeight / img.naturalHeight;

            overlay.style.left = (rect.x * scaleX) + 'px';
            overlay.style.top = (rect.y * scaleY) + 'px';
            overlay.style.width = (rect.width * scaleX) + 'px';
            overlay.style.height = (rect.height * scaleY) + 'px';
            overlay.style.display = 'block';
        }

        window.addEventListener('resize', recalculateHighlightOverlay);
        
        window.onload = init;
    </script>
</body>
</html>
""";
}
