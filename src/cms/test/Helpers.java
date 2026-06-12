package cms.test;

import cms.Json;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Helpers {

    private static String apiBase = "";
    private static String frontendBase = "";
    private static final Map<String, String> SEEDED = new LinkedHashMap<>();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();

    private Helpers() {}

    public static void setApiBase(String url) { apiBase = trimSlash(url); }
    public static void setFrontendBase(String url) { frontendBase = trimSlash(url); }
    public static String getApiBase() { return apiBase; }
    public static String getFrontendBase() { return frontendBase; }
    public static void resetSeedCache() { SEEDED.clear(); }

    private static String trimSlash(String s) { return s.endsWith("/") ? s.substring(0, s.length() - 1) : s; }

    public static boolean waitForHealth(String baseUrl, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            try {
                Response r = httpRequest("GET", baseUrl + "/health", null, Map.of());
                if (r.status == 200) return true;
            } catch (Exception ignored) {}
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    public static String pluralOf(String entity) {
        switch (entity) {
            case "BlogPosting": return "blog-postings";
            case "Person": return "persons";
            case "WebPage": return "web-pages";
            case "ImageObject": return "image-objects";
            case "CategoryCode": return "category-codes";
            case "CategoryCodeSet": return "category-code-sets";
            case "DefinedTerm": return "defined-terms";
            case "DefinedTermSet": return "defined-term-sets";
            case "Comment": return "comments";
            case "WebSite": return "web-sites";
            default: throw new RuntimeException("Unknown entity: " + entity);
        }
    }

    public static Map<String, Object> sampleFor(String entity) {
        switch (entity) {
        case "BlogPosting": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("headline", "sample");
            sample.put("articleBody", "sample");
            sample.put("author", Map.of("__ref", "Person"));
            return sample;
        }
        case "Person": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("name", "sample");
            return sample;
        }
        case "WebPage": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("headline", "sample");
            return sample;
        }
        case "ImageObject": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("contentUrl", "https://example.com/x");
            return sample;
        }
        case "CategoryCode": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("name", "sample");
            sample.put("codeValue", "sample");
            sample.put("inCodeSet", Map.of("__ref", "CategoryCodeSet"));
            return sample;
        }
        case "CategoryCodeSet": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("name", "sample");
            return sample;
        }
        case "DefinedTerm": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("name", "sample");
            sample.put("termCode", "sample");
            sample.put("inDefinedTermSet", Map.of("__ref", "DefinedTermSet"));
            return sample;
        }
        case "DefinedTermSet": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("name", "sample");
            return sample;
        }
        case "Comment": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("text", "sample");
            sample.put("author", Map.of("__ref", "Person"));
            sample.put("about", Map.of("__ref", "BlogPosting"));
            return sample;
        }
        case "WebSite": {
            Map<String, Object> sample = new LinkedHashMap<>();
            sample.put("name", "sample");
            sample.put("url", "https://example.com/x");
            return sample;
        }
            default: throw new RuntimeException("Unknown entity: " + entity);
        }
    }

    public static class Response {
        public final int status;
        public final Map<String, String> headers;
        public final String body;

        public Response(int status, Map<String, String> headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }
    }

    public static Response httpRequest(String method, String url, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10));
            HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
            for (Map.Entry<String, String> e : headers.entrySet()) b.header(e.getKey(), e.getValue());
            b.method(method, publisher);
            HttpResponse<String> r = CLIENT.send(b.build(), HttpResponse.BodyHandlers.ofString());
            Map<String, String> hdrs = new LinkedHashMap<>();
            r.headers().map().forEach((k, v) -> { if (!v.isEmpty()) hdrs.put(k.toLowerCase(), v.get(0)); });
            return new Response(r.statusCode(), hdrs, r.body());
        } catch (Exception e) {
            throw new RuntimeException("Request failed: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveRefs(Map<String, Object> sample) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : sample.entrySet()) {
            Object v = e.getValue();
            if (v instanceof List) {
                List<Object> list = new ArrayList<>();
                for (Object item : (List<?>) v) {
                    if (item instanceof Map && ((Map<?, ?>) item).containsKey("__ref")) {
                        list.add(ensureEntity((String) ((Map<?, ?>) item).get("__ref")));
                    } else {
                        list.add(item);
                    }
                }
                out.put(e.getKey(), list);
            } else if (v instanceof Map && ((Map<?, ?>) v).containsKey("__ref")) {
                out.put(e.getKey(), ensureEntity((String) ((Map<?, ?>) v).get("__ref")));
            } else {
                out.put(e.getKey(), v);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static String ensureEntity(String entity) {
        if (SEEDED.containsKey(entity)) return SEEDED.get(entity);
        Map<String, Object> sample = resolveRefs(sampleFor(entity));
        Response r = httpRequest("POST", apiBase + "/" + pluralOf(entity),
            Json.stringify(sample),
            Map.of("Content-Type", "application/json"));
        if (r.status != 201) throw new RuntimeException("ensureEntity(" + entity + ") failed: " + r.status + " " + r.body);
        Map<String, Object> body = (Map<String, Object>) Json.parse(r.body);
        String id = (String) body.get("id");
        SEEDED.put(entity, id);
        return id;
    }

    // Seed one fresh entity with chosen field overrides, bypassing the seed cache.
    // Used to plant a hostile field value (e.g. a "javascript:" URL) and check how
    // the frontend renders it back.
    @SuppressWarnings("unchecked")
    public static String seedWith(String entity, Map<String, Object> overrides) {
        Map<String, Object> payload = resolveRefs(sampleFor(entity));
        payload.putAll(overrides);
        Response r = httpRequest("POST", apiBase + "/" + pluralOf(entity),
            Json.stringify(payload),
            Map.of("Content-Type", "application/json"));
        if (r.status != 201) throw new RuntimeException("seedWith(" + entity + ") failed: " + r.status + " " + r.body);
        Map<String, Object> body = (Map<String, Object>) Json.parse(r.body);
        return (String) body.get("id");
    }

    private static String encodeOne(Object v) {
        if (v == null) return "";
        if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if ("Language".equals(m.get("@type"))) {
                Object alt = m.get("alternateName");
                return alt == null ? "" : alt.toString();
            }
            return Json.stringify(v);
        }
        if (v instanceof Boolean) return ((Boolean) v) ? "true" : "false";
        return v.toString();
    }

    public static String formBodyFor(String entity) {
        Map<String, Object> sample = resolveRefs(sampleFor(entity));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : sample.entrySet()) {
            Object v = e.getValue();
            if (v instanceof List) {
                for (Object item : (List<?>) v) appendPair(sb, e.getKey(), encodeOne(item));
            } else {
                appendPair(sb, e.getKey(), encodeOne(v));
            }
        }
        return sb.toString();
    }

    private static void appendPair(StringBuilder sb, String k, String v) {
        if (sb.length() > 0) sb.append('&');
        sb.append(URLEncoder.encode(k, StandardCharsets.UTF_8));
        sb.append('=');
        sb.append(URLEncoder.encode(v, StandardCharsets.UTF_8));
    }

    public static Response frontendGet(String path) {
        return httpRequest("GET", frontendBase + path, null, Map.of());
    }

    public static Response frontendPostForm(String path, String body) {
        return httpRequest("POST", frontendBase + path, body, Map.of("Content-Type", "application/x-www-form-urlencoded"));
    }
}
