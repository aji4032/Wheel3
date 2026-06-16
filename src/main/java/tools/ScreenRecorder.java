package tools;

import cdphandler.CdpUtility;
import com.fasterxml.jackson.databind.JsonNode;
import logger.Log;
import logger.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * CDP-based screen recorder that captures browser frames via
 * {@code Page.startScreencast} and assembles them into an MJPEG AVI file.
 * <p>
 * Works in both headless and headed Chrome modes because it captures
 * the rendered page content through the DevTools Protocol, not the
 * physical screen.
 * <p>
 * Thread-safe: each test thread gets its own recording session via
 * {@link ThreadLocal}. Toggle recording globally with the system
 * property {@code record.video} (default {@code true}).
 *
 * <pre>
 * ScreenRecorder.startRecording(driver.getCdpUtility(), "myTest");
 * // ... test steps ...
 * File video = ScreenRecorder.stopRecording();
 * </pre>
 */
public class ScreenRecorder {
    private static final Logger log = Log.getLogger(ScreenRecorder.class);
    private static final String OUTPUT_DIR = "target/recordings";
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("record.video", "true"));

    private static final ThreadLocal<RecordingSession> session = new ThreadLocal<>();

    private ScreenRecorder() {
    }

    /**
     * Begins recording the browser viewport for the current thread.
     *
     * @param cdpUtility The CDP utility connected to the page target.
     * @param testName   A human-readable name used in the output filename.
     */
    public static void startRecording(CdpUtility cdpUtility, String testName) {
        if (!ENABLED || cdpUtility == null)
            return;

        try {
            RecordingSession rec = new RecordingSession(cdpUtility, testName);
            session.set(rec);
            rec.start();
            log.info("Screen recording started for: {}", testName);
        } catch (Exception e) {
            log.warn("Failed to start screen recording: {}", e.getMessage());
        }
    }

    /**
     * Stops the current recording and writes the AVI file to disk.
     *
     * @return The AVI file, or {@code null} if recording was not active.
     */
    public static File stopRecording() {
        RecordingSession rec = session.get();
        if (rec == null)
            return null;

        try {
            File result = rec.stop();
            log.info("Screen recording saved: {} ({} frames)", result.getAbsolutePath(), rec.getFrameCount());
            return result;
        } catch (Exception e) {
            log.warn("Failed to stop screen recording: {}", e.getMessage());
            return null;
        } finally {
            session.remove();
        }
    }

    /**
     * Deletes the most recently recorded file (typically called on test pass).
     */
    public static void deleteLastRecording(File file) {
        if (file != null && file.exists()) {
            if (file.delete()) {
                log.info("Deleted recording: {}", file.getName());
            }
        }
    }

    /**
     * Returns {@code true} if video recording is globally enabled.
     */
    public static boolean isEnabled() {
        return ENABLED;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Internal recording session
    // ═══════════════════════════════════════════════════════════════════════

    private static class RecordingSession {
        private final CdpUtility cdpUtility;
        private final String testName;
        private final List<byte[]> frames = new CopyOnWriteArrayList<>();
        private Consumer<JsonNode> eventListener;
        private int frameWidth = 0;
        private int frameHeight = 0;
        private long startTimeMs;
        private volatile byte[] lastFrame; // for deduplication

        RecordingSession(CdpUtility cdpUtility, String testName) {
            this.cdpUtility = cdpUtility;
            this.testName = sanitize(testName);
        }

        void start() {
            startTimeMs = System.currentTimeMillis();

            // Register listener for Page.screencastFrame events
            eventListener = event -> {
                String method = event.has("method") ? event.get("method").asText() : "";
                if ("Page.screencastFrame".equals(method)) {
                    JsonNode params = event.get("params");
                    if (params != null) {
                        String data = params.get("data").asText();
                        int sessionId = params.get("sessionId").asInt();

                        // Capture frame dimensions from metadata
                        if (params.has("metadata")) {
                            JsonNode meta = params.get("metadata");
                            if (meta.has("deviceWidth") && meta.has("deviceHeight")) {
                                frameWidth = meta.get("deviceWidth").asInt();
                                frameHeight = meta.get("deviceHeight").asInt();
                            }
                        }

                        // Decode and store the JPEG frame (skip if identical to previous)
                        byte[] jpegBytes = Base64.getDecoder().decode(data);
                        if (lastFrame == null || !Arrays.equals(jpegBytes, lastFrame)) {
                            frames.add(jpegBytes);
                            lastFrame = jpegBytes;
                        }

                        // Acknowledge the frame (async to avoid deadlocking the listener thread)
                        try {
                            cdpUtility.pageScreencastFrameAckAsync(sessionId);
                        } catch (Exception ignored) {
                            // Best effort — don't let ACK failure crash recording
                        }
                    }
                }
            };
            cdpUtility.getClient().addEventListener(eventListener);

            // Fetch the actual browser viewport size
            int maxWidth = 1280;
            int maxHeight = 720;
            try {
                JsonNode widthResult = cdpUtility.runtimeEvaluate("window.innerWidth", true);
                JsonNode heightResult = cdpUtility.runtimeEvaluate("window.innerHeight", true);
                if (widthResult != null && widthResult.has("value")) {
                    maxWidth = widthResult.get("value").asInt(1280);
                }
                if (heightResult != null && heightResult.has("value")) {
                    maxHeight = heightResult.get("value").asInt(720);
                }
            } catch (Exception e) {
                log.warn("Could not fetch viewport size, using defaults: {}", e.getMessage());
            }

            // Start the screencast: quality 50 (good for debugging, ~50% smaller),
            // capture every 2nd frame to further reduce frame count
            cdpUtility.pageStartScreencast("jpeg", 50, maxWidth, maxHeight, 2);
        }

        File stop() throws IOException {
            // Stop screencast
            try {
                cdpUtility.pageStopScreencast();
            } catch (Exception ignored) {
            }

            // Remove event listener
            if (eventListener != null) {
                cdpUtility.getClient().removeEventListener(eventListener);
            }

            // Write AVI
            File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File outputFile = new File(outputDir, testName + "_" + timestamp + ".avi");

            if (frames.isEmpty()) {
                log.warn("No frames captured for recording: {}", testName);
                // Write an empty file so caller has a non-null reference
                outputFile.createNewFile();
                return outputFile;
            }

            // Compute actual FPS from recording duration so playback matches real time
            long elapsedMs = System.currentTimeMillis() - startTimeMs;
            double elapsedSec = Math.max(elapsedMs / 1000.0, 1.0);
            int fps = (int) Math.round(frames.size() / elapsedSec);
            fps = Math.max(fps, 1); // at least 1 FPS
            log.info("Recording stats: {} frames in {}s → {} FPS", frames.size(),
                    String.format("%.1f", elapsedSec), fps);

            writeAvi(outputFile, frames, frameWidth > 0 ? frameWidth : 1280,
                    frameHeight > 0 ? frameHeight : 720, fps);
            return outputFile;
        }

        int getFrameCount() {
            return frames.size();
        }

        private static String sanitize(String name) {
            return name.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lightweight MJPEG AVI writer (pure Java, no external dependencies)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Writes a list of JPEG frames into a minimal RIFF/AVI container using the
     * MJPG (Motion JPEG) codec. The resulting file is playable in VLC, Windows
     * Media Player, and most video tools.
     */
    static void writeAvi(File output, List<byte[]> jpegFrames, int width, int height, int fps)
            throws IOException {

        int frameCount = jpegFrames.size();

        // Pre-calculate sizes
        // Each frame is wrapped: 'dc' chunk = 8 (chunk header) + padded frame size
        List<Integer> paddedSizes = new ArrayList<>(frameCount);
        int moviDataSize = 4; // 'movi' list type identifier
        for (byte[] frame : jpegFrames) {
            int padded = (frame.length + 1) & ~1; // pad to 2-byte boundary
            paddedSizes.add(padded);
            moviDataSize += 8 + padded; // chunk header (8) + data
        }

        int moviListSize = 8 + moviDataSize; // LIST + size + 'movi' + chunks
        int idx1Size = 8 + (frameCount * 16); // 'idx1' + size + entries
        int hdrlSize = 4 + 64 + (12 + 64 + 48); // 'hdrl' + avih(64) + strl(LIST 12 + strh 64 + strf 48)
        int hdrlListSize = 8 + hdrlSize;

        int riffSize = 4 + hdrlListSize + moviListSize + idx1Size; // 'AVI ' + hdrl + movi + idx1

        try (FileOutputStream fos = new FileOutputStream(output)) {
            ByteBuffer buf;

            // ── RIFF header ─────────────────────────────────────────────
            buf = allocate(12);
            buf.put("RIFF".getBytes());
            buf.putInt(riffSize);
            buf.put("AVI ".getBytes());
            fos.write(buf.array());

            // ── LIST hdrl ───────────────────────────────────────────────
            buf = allocate(12);
            buf.put("LIST".getBytes());
            buf.putInt(hdrlSize);
            buf.put("hdrl".getBytes());
            fos.write(buf.array());

            // ── avih (main AVI header) ──────────────────────────────────
            buf = allocate(64);
            buf.put("avih".getBytes());
            buf.putInt(56); // struct size
            buf.putInt(1_000_000 / fps); // microseconds per frame
            buf.putInt(0); // max bytes per sec (0 = unknown)
            buf.putInt(0); // padding granularity
            buf.putInt(0x10); // flags: AVIF_HASINDEX
            buf.putInt(frameCount); // total frames
            buf.putInt(0); // initial frames
            buf.putInt(1); // number of streams
            buf.putInt(0); // suggested buffer size
            buf.putInt(width);
            buf.putInt(height);
            buf.putInt(0);
            buf.putInt(0); // reserved[4]
            buf.putInt(0);
            buf.putInt(0);
            fos.write(buf.array());

            // ── LIST strl (stream list) ─────────────────────────────────
            int strlSize = 4 + 64 + 48; // 'strl' + strh + strf
            buf = allocate(12);
            buf.put("LIST".getBytes());
            buf.putInt(strlSize);
            buf.put("strl".getBytes());
            fos.write(buf.array());

            // ── strh (stream header) ────────────────────────────────────
            buf = allocate(64);
            buf.put("strh".getBytes());
            buf.putInt(56); // struct size
            buf.put("vids".getBytes()); // fccType
            buf.put("MJPG".getBytes()); // fccHandler
            buf.putInt(0); // flags
            buf.putShort((short) 0); // priority
            buf.putShort((short) 0); // language
            buf.putInt(0); // initial frames
            buf.putInt(1); // scale
            buf.putInt(fps); // rate
            buf.putInt(0); // start
            buf.putInt(frameCount); // length
            buf.putInt(0); // suggested buffer size
            buf.putInt(-1); // quality (-1 = default)
            buf.putInt(0); // sample size
            buf.putShort((short) 0); // rcFrame left
            buf.putShort((short) 0); // rcFrame top
            buf.putShort((short) width); // rcFrame right
            buf.putShort((short) height); // rcFrame bottom
            fos.write(buf.array());

            // ── strf (stream format — BITMAPINFOHEADER) ─────────────────
            buf = allocate(48);
            buf.put("strf".getBytes());
            buf.putInt(40); // struct size
            buf.putInt(40); // biSize
            buf.putInt(width); // biWidth
            buf.putInt(height); // biHeight
            buf.putShort((short) 1); // biPlanes
            buf.putShort((short) 24); // biBitCount
            buf.put("MJPG".getBytes()); // biCompression
            buf.putInt(width * height * 3); // biSizeImage
            buf.putInt(0); // biXPelsPerMeter
            buf.putInt(0); // biYPelsPerMeter
            buf.putInt(0); // biClrUsed
            buf.putInt(0); // biClrImportant
            fos.write(buf.array());

            // ── LIST movi ───────────────────────────────────────────────
            buf = allocate(12);
            buf.put("LIST".getBytes());
            buf.putInt(moviDataSize);
            buf.put("movi".getBytes());
            fos.write(buf.array());

            // ── Frame data ('00dc' chunks) ──────────────────────────────
            int moviOffset = 4; // offset inside movi (after 'movi' tag)
            List<int[]> idx1Entries = new ArrayList<>(frameCount);

            for (int i = 0; i < frameCount; i++) {
                byte[] frame = jpegFrames.get(i);
                int padded = paddedSizes.get(i);

                idx1Entries.add(new int[] { moviOffset, frame.length });

                buf = allocate(8);
                buf.put("00dc".getBytes()); // stream 0, compressed
                buf.putInt(frame.length);
                fos.write(buf.array());

                fos.write(frame);
                // Pad to 2-byte boundary
                if (frame.length != padded) {
                    fos.write(0);
                }
                moviOffset += 8 + padded;
            }

            // ── idx1 (AVI index) ────────────────────────────────────────
            buf = allocate(8);
            buf.put("idx1".getBytes());
            buf.putInt(frameCount * 16);
            fos.write(buf.array());

            for (int[] entry : idx1Entries) {
                buf = allocate(16);
                buf.put("00dc".getBytes());
                buf.putInt(0x10); // AVIIF_KEYFRAME
                buf.putInt(entry[0]); // offset in movi
                buf.putInt(entry[1]); // frame size
                fos.write(buf.array());
            }
        }
    }

    private static ByteBuffer allocate(int size) {
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }
}
