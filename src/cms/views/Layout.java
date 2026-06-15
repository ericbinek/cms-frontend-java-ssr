package cms.views;

import cms.Json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Layout {

    public static final List<String> ENTITIES = List.of("BlogPosting", "Person", "WebPage", "ImageObject", "CategoryCode", "CategoryCodeSet", "DefinedTerm", "DefinedTermSet", "Comment", "WebSite");
    public static final Map<String, String> PLURALS = new LinkedHashMap<>();
    public static final Map<String, List<String>> DISPLAY_KEYS = new LinkedHashMap<>();
    static {
        PLURALS.put("BlogPosting", "blog-postings");
        PLURALS.put("Person", "persons");
        PLURALS.put("WebPage", "web-pages");
        PLURALS.put("ImageObject", "image-objects");
        PLURALS.put("CategoryCode", "category-codes");
        PLURALS.put("CategoryCodeSet", "category-code-sets");
        PLURALS.put("DefinedTerm", "defined-terms");
        PLURALS.put("DefinedTermSet", "defined-term-sets");
        PLURALS.put("Comment", "comments");
        PLURALS.put("WebSite", "web-sites");
        DISPLAY_KEYS.put("BlogPosting", List.of("headline", "alternativeHeadline"));
        DISPLAY_KEYS.put("Person", List.of("name", "givenName", "familyName"));
        DISPLAY_KEYS.put("WebPage", List.of("headline"));
        DISPLAY_KEYS.put("ImageObject", List.of("name", "caption", "contentUrl"));
        DISPLAY_KEYS.put("CategoryCode", List.of("name", "codeValue"));
        DISPLAY_KEYS.put("CategoryCodeSet", List.of("name"));
        DISPLAY_KEYS.put("DefinedTerm", List.of("name", "termCode"));
        DISPLAY_KEYS.put("DefinedTermSet", List.of("name"));
        DISPLAY_KEYS.put("Comment", List.of("text"));
        DISPLAY_KEYS.put("WebSite", List.of("name"));
    }

    private Layout() {}

    public static String pluralOf(String entity) {
        return PLURALS.getOrDefault(entity, entity.toLowerCase() + "s");
    }

    public static String escapeHtml(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String layout(Map<String, Object> opts) {
        String title = (String) opts.getOrDefault("title", "CMS");
        String body = (String) opts.getOrDefault("body", "");
        Object currentEntity = opts.get("currentEntity");
        Object flash = opts.get("flash");
        StringBuilder nav = new StringBuilder();
        for (String e : ENTITIES) {
            String current = e.equals(currentEntity) ? " aria-current=\"page\"" : "";
            nav.append("<li><a href=\"/").append(PLURALS.get(e)).append("\"").append(current).append(">").append(escapeHtml(e)).append("</a></li>");
        }
        String flashEl = flash != null ? "<p role=\"status\">" + escapeHtml(flash) + "</p>" : "";
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "<meta charset=\"utf-8\">\n" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
            "<title>" + escapeHtml(title) + " — CMS</title>\n" +
            "<link rel=\"stylesheet\" href=\"/style.css\">\n" +
            "</head>\n" +
            "<body>\n" +
            "<header>\n" +
            "<nav aria-label=\"Primary\">\n" +
            "<ul>" + nav + "</ul>\n" +
            "</nav>\n" +
            "</header>\n" +
            "<main>\n" +
            "<h1>" + escapeHtml(title) + "</h1>\n" +
            flashEl + "\n" +
            body + "\n" +
            "</main>\n" +
            "</body>\n" +
            "</html>\n";
    }

    public static String displayName(Map<String, Object> item, String entity) {
        if (item == null) return "";
        for (String k : DISPLAY_KEYS.getOrDefault(entity, List.of("name", "headline"))) {
            Object v = item.get(k);
            if (v instanceof String && !((String) v).isEmpty()) return (String) v;
        }
        Object id = item.get("id");
        return id == null ? "" : id.toString();
    }

    public static Map<String, Object> errorPage(int status, String message) {
        Map<String, Object> opts = new LinkedHashMap<>();
        opts.put("title", status == 404 ? "Not Found" : "Error");
        opts.put("body", "<p role=\"alert\">" + escapeHtml(message) + "</p>");
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("html", layout(opts));
        return response;
    }

    // Only http(s), mailto and site-relative values may become clickable links.
    // A stored "javascript:" or "data:" URL is rendered as inert escaped text, so
    // a bad value in the data store cannot turn into stored XSS when a user clicks
    // it.
    private static boolean isSafeHref(Object value) {
        if (!(value instanceof String)) return false;
        String v = ((String) value).trim().toLowerCase();
        return v.startsWith("http://") || v.startsWith("https://")
            || v.startsWith("mailto:") || v.startsWith("/");
    }

    private static String formatScalar(Object value, String use) {
        if ("URL".equals(use)) {
            if (!isSafeHref(value)) return escapeHtml(value);
            String v = escapeHtml(value);
            return "<a href=\"" + v + "\" rel=\"noopener noreferrer\">" + v + "</a>";
        }
        if ("DateTime".equals(use) || "Date".equals(use) || "Time".equals(use)) {
            String v = escapeHtml(value);
            return "<time datetime=\"" + v + "\">" + v + "</time>";
        }
        if ("Boolean".equals(use)) return Boolean.TRUE.equals(value) ? "Yes" : "No";
        return escapeHtml(value);
    }

    public static String formatValue(Object value, PropertySpec prop) {
        if (value == null || "".equals(value)) return "<em>—</em>";
        if (value instanceof List<?> list) {
            if (list.isEmpty()) return "<em>—</em>";
            StringBuilder sb = new StringBuilder("<ul>");
            for (Object v : list) sb.append("<li>").append(formatValue(v, prop)).append("</li>");
            sb.append("</ul>");
            return sb.toString();
        }
        return switch (prop) {
            case PropertySpec.Ref ref -> {
                String target = ref.targets().get(0);
                String plural = PLURALS.getOrDefault(target, target.toLowerCase() + "s");
                yield "<a href=\"/" + plural + "/" + escapeHtml(value) + "\">" + escapeHtml(target) + ": " + escapeHtml(value) + "</a>";
            }
            case PropertySpec.Embed em -> {
                if ("Language".equals(em.use()) && value instanceof Map<?, ?> m) {
                    Object code = m.get("alternateName");
                    if (code == null) code = m.get("name");
                    if (code == null) code = "";
                    yield "<span lang=\"" + escapeHtml(code) + "\">" + escapeHtml(code) + "</span>";
                }
                yield "<code>" + escapeHtml(Json.stringify(value)) + "</code>";
            }
            case PropertySpec.Enumerated e -> escapeHtml(value);
            case PropertySpec.Scalar s -> formatScalar(value, s.use());
        };
    }
}
