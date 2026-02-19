package cdphandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import tools.Log;

public class OllamaUtility {

        private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
        private static final int MAX_HTML_LENGTH = 15000;
        private static final OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(120, TimeUnit.SECONDS)
                        .readTimeout(120, TimeUnit.SECONDS)
                        .writeTimeout(120, TimeUnit.SECONDS)
                        .build();
        private static final ObjectMapper mapper = new ObjectMapper();

        // Precompiled patterns for HTML sanitization
        private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script[^>]*>.*?</script>",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        private static final Pattern STYLE_PATTERN = Pattern.compile("<style[^>]*>.*?</style>",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        private static final Pattern SVG_PATTERN = Pattern.compile("<svg[^>]*>.*?</svg>",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        private static final Pattern NOSCRIPT_PATTERN = Pattern.compile("<noscript[^>]*>.*?</noscript>",
                        Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        private static final Pattern COMMENT_PATTERN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
        private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s{2,}");

        /**
         * Strips scripts, styles, SVGs, comments, and excessive whitespace from HTML
         * to reduce token count for the LLM.
         */
        private static String sanitizeHtml(String html) {
                String cleaned = html;
                cleaned = SCRIPT_PATTERN.matcher(cleaned).replaceAll("");
                cleaned = STYLE_PATTERN.matcher(cleaned).replaceAll("");
                cleaned = SVG_PATTERN.matcher(cleaned).replaceAll("");
                cleaned = NOSCRIPT_PATTERN.matcher(cleaned).replaceAll("");
                cleaned = COMMENT_PATTERN.matcher(cleaned).replaceAll("");
                cleaned = WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ");
                cleaned = cleaned.trim();

                if (cleaned.length() > MAX_HTML_LENGTH) {
                        cleaned = cleaned.substring(0, MAX_HTML_LENGTH) + "\n<!-- HTML truncated -->";
                }

                return cleaned;
        }

        public static String getSelector(String html, String query) {
                String cleanedHtml = sanitizeHtml(html);

                Log.info("Original HTML size: " + html.length()
                                + " chars, Cleaned HTML size: " + cleanedHtml.length() + " chars");

                String prompt = "Given this HTML:\n" + cleanedHtml + "\n\n" +
                                "Find the element matching: \"" + query + "\"\n\n" +
                                "Return ONLY the CSS selector or XPath (starting with /) for the element. " +
                                "No explanation, no code blocks, just the selector string.";

                try {
                        ObjectNode jsonBody = mapper.createObjectNode();
                        jsonBody.put("model", "qwen2.5-coder:7b");
                        jsonBody.put("prompt", prompt);
                        jsonBody.put("stream", false);

                        RequestBody body = RequestBody.create(
                                        mapper.writeValueAsString(jsonBody),
                                        MediaType.get("application/json; charset=utf-8"));

                        Request request = new Request.Builder()
                                        .url(OLLAMA_API_URL)
                                        .post(body)
                                        .build();

                        Log.info("Sending request to Ollama...");

                        try (Response response = client.newCall(request).execute()) {
                                if (!response.isSuccessful()) {
                                        throw new IOException("Unexpected code " + response);
                                }

                                String responseBody = response.body().string();
                                String result = mapper.readTree(responseBody).get("response").asText().trim();
                                Log.info("Got selector: " + result);
                                return result;
                        }
                } catch (IOException e) {
                        throw new RuntimeException(e);
                }
        }
}
