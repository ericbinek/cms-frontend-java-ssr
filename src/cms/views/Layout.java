package cms.views;

import cms.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    private static final Set<String> LONG_TEXT_HINT = Set.of("articleBody", "description", "text");
    private static final Pattern FORM_ISO_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$");

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

    @SuppressWarnings("unchecked")
    public static String formatValue(Object value, Map<String, Object> prop) {
        if (value == null || "".equals(value)) return "<em>—</em>";
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "<em>—</em>";
            Map<String, Object> single = new LinkedHashMap<>(prop);
            single.put("cardinality", "one");
            StringBuilder sb = new StringBuilder("<ul>");
            for (Object v : list) sb.append("<li>").append(formatValue(v, single)).append("</li>");
            sb.append("</ul>");
            return sb.toString();
        }
        String kind = (String) prop.get("kind");
        if ("Ref".equals(kind)) {
            List<String> targets = (List<String>) prop.get("targets");
            String target = targets.get(0);
            String plural = PLURALS.getOrDefault(target, target.toLowerCase() + "s");
            return "<a href=\"/" + plural + "/" + escapeHtml(value) + "\">" + escapeHtml(target) + ": " + escapeHtml(value) + "</a>";
        }
        if ("Embed".equals(kind)) {
            if ("Language".equals(prop.get("use")) && value instanceof Map) {
                Map<String, Object> m = (Map<String, Object>) value;
                Object code = m.getOrDefault("alternateName", m.getOrDefault("name", ""));
                return "<span lang=\"" + escapeHtml(code) + "\">" + escapeHtml(code) + "</span>";
            }
            return "<code>" + escapeHtml(Json.stringify(value)) + "</code>";
        }
        if ("Enum".equals(kind)) return escapeHtml(value);
        return formatScalar(value, (String) prop.get("use"));
    }

    public static String renderField(Map<String, Object> prop, Object value, Map<String, List<Map<String, String>>> refOptions, List<String> errors) {
        if (refOptions == null) refOptions = Map.of();
        if (errors == null) errors = List.of();
        String name = (String) prop.get("name");
        boolean required = Boolean.TRUE.equals(prop.get("required"));
        String fieldId = "field-" + name;
        String requiredAttr = required ? " required" : "";
        String requiredMark = required ? " <span aria-hidden=\"true\">*</span>" : "";
        String ariaInvalid = errors.isEmpty() ? "" : " aria-invalid=\"true\"";
        String labelText = escapeHtml(name) + requiredMark;
        StringBuilder help = new StringBuilder();
        if (!errors.isEmpty()) {
            help.append("<small role=\"alert\">");
            for (int i = 0; i < errors.size(); i++) {
                if (i > 0) help.append("; ");
                help.append(escapeHtml(errors.get(i)));
            }
            help.append("</small>");
        }
        String input = renderInput(prop, value, fieldId, requiredAttr, ariaInvalid, refOptions);
        return "<p>\n<label for=\"" + fieldId + "\">" + labelText + "</label><br>\n" + input + "\n" + help + "\n</p>";
    }

    @SuppressWarnings("unchecked")
    private static String renderInput(Map<String, Object> prop, Object value, String fieldId, String requiredAttr, String ariaInvalid, Map<String, List<Map<String, String>>> refOptions) {
        String name = escapeHtml(prop.get("name"));
        String kind = (String) prop.get("kind");
        boolean required = Boolean.TRUE.equals(prop.get("required"));
        String cardinality = (String) prop.get("cardinality");

        if ("Enum".equals(kind)) {
            List<String> values = (List<String>) prop.get("values");
            StringBuilder opts = new StringBuilder();
            for (String v : values) {
                String sel = v.equals(value) ? " selected" : "";
                opts.append("<option value=\"").append(escapeHtml(v)).append("\"").append(sel).append(">").append(escapeHtml(v)).append("</option>");
            }
            String placeholder = required ? "" : "<option value=\"\">—</option>";
            return "<select id=\"" + fieldId + "\" name=\"" + name + "\"" + requiredAttr + ariaInvalid + ">" + placeholder + opts + "</select>";
        }
        if ("Ref".equals(kind)) {
            List<Object> current;
            if ("many".equals(cardinality)) {
                current = value instanceof List ? new ArrayList<>((List<Object>) value) : (value == null ? new ArrayList<>() : new ArrayList<>(List.of(value)));
            } else {
                Object single = value instanceof List ? (((List<?>) value).isEmpty() ? null : ((List<?>) value).get(0)) : value;
                current = single == null ? new ArrayList<>() : new ArrayList<>(List.of(single));
            }
            StringBuilder opts = new StringBuilder();
            List<Map<String, String>> options = refOptions.getOrDefault((String) prop.get("name"), List.of());
            for (Map<String, String> o : options) {
                boolean sel = current.contains(o.get("value"));
                opts.append("<option value=\"").append(escapeHtml(o.get("value"))).append("\"")
                    .append(sel ? " selected" : "").append(">").append(escapeHtml(o.get("label"))).append("</option>");
            }
            String multiple = "many".equals(cardinality) ? " multiple" : "";
            String placeholder = "one".equals(cardinality) && !required ? "<option value=\"\">—</option>" : "";
            return "<select id=\"" + fieldId + "\" name=\"" + name + "\"" + multiple + requiredAttr + ariaInvalid + ">" + placeholder + opts + "</select>";
        }
        if ("Embed".equals(kind) && "Language".equals(prop.get("use"))) {
            Object v;
            if (value instanceof Map) {
                Object alt = ((Map<?, ?>) value).get("alternateName");
                v = alt == null ? "" : alt;
            } else {
                v = value == null ? "" : value;
            }
            return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"text\" value=\"" + escapeHtml(v) + "\"" + requiredAttr + ariaInvalid + ">";
        }
        if ("many".equals(cardinality)) {
            String v;
            if (value instanceof List) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < ((List<?>) value).size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(((List<?>) value).get(i));
                }
                v = sb.toString();
            } else {
                v = value == null ? "" : value.toString();
            }
            return "<textarea id=\"" + fieldId + "\" name=\"" + name + "\" rows=\"3\"" + requiredAttr + ariaInvalid + ">" + escapeHtml(v) + "</textarea>";
        }
        String use = (String) prop.get("use");
        if ("Text".equals(use) && LONG_TEXT_HINT.contains(prop.get("name"))) {
            return "<textarea id=\"" + fieldId + "\" name=\"" + name + "\" rows=\"6\"" + requiredAttr + ariaInvalid + ">" + escapeHtml(value) + "</textarea>";
        }
        if ("URL".equals(use)) {
            return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"url\" value=\"" + escapeHtml(value) + "\"" + requiredAttr + ariaInvalid + ">";
        }
        if ("Integer".equals(use)) {
            String v = value == null ? "" : escapeHtml(value);
            return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"number\" step=\"1\" value=\"" + v + "\"" + requiredAttr + ariaInvalid + ">";
        }
        if ("Number".equals(use)) {
            String v = value == null ? "" : escapeHtml(value);
            return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"number\" step=\"any\" value=\"" + v + "\"" + requiredAttr + ariaInvalid + ">";
        }
        if ("Boolean".equals(use)) {
            boolean checked = Boolean.TRUE.equals(value) || "true".equals(value) || "on".equals(value);
            return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"checkbox\" value=\"true\"" + (checked ? " checked" : "") + ariaInvalid + ">";
        }
        if ("DateTime".equals(use) || "Date".equals(use) || "Time".equals(use)) {
            String v = "";
            if (value instanceof String) {
                String s = ((String) value).replaceAll("Z$", "");
                v = s.length() > 16 ? s.substring(0, 16) : s;
            }
            return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"datetime-local\" value=\"" + escapeHtml(v) + "\"" + requiredAttr + ariaInvalid + ">";
        }
        return "<input id=\"" + fieldId + "\" name=\"" + name + "\" type=\"text\" value=\"" + escapeHtml(value) + "\"" + requiredAttr + ariaInvalid + ">";
    }

    @SuppressWarnings("unchecked")
    private static Object coerceFormValue(Object raw, Map<String, Object> prop) {
        if (raw == null || "".equals(raw)) return null;
        String kind = (String) prop.get("kind");
        if ("Enum".equals(kind) || "Ref".equals(kind)) return raw.toString();
        if ("Embed".equals(kind) && "Language".equals(prop.get("use"))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("@type", "Language");
            m.put("alternateName", raw.toString());
            return m;
        }
        String use = (String) prop.get("use");
        if ("Integer".equals(use)) {
            try { return Long.parseLong(raw.toString()); } catch (NumberFormatException e) { return raw; }
        }
        if ("Number".equals(use)) {
            try { return Double.parseDouble(raw.toString()); } catch (NumberFormatException e) { return raw; }
        }
        if ("Boolean".equals(use)) {
            String s = raw.toString();
            return "true".equals(s) || "on".equals(s) || "1".equals(s);
        }
        if ("DateTime".equals(use) || "Date".equals(use) || "Time".equals(use)) {
            String s = raw.toString();
            if (FORM_ISO_PATTERN.matcher(s).matches()) return s + ":00Z";
            return s;
        }
        return raw.toString();
    }

    private static Map<String, Object> parseFormPairs(String raw) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = decodeFormPart(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decodeFormPart(pair.substring(eq + 1));
            Object existing = out.get(key);
            if (existing == null) {
                out.put(key, value);
            } else if (existing instanceof List) {
                @SuppressWarnings("unchecked") List<Object> lst = (List<Object>) existing;
                lst.add(value);
            } else {
                List<Object> lst = new ArrayList<>();
                lst.add(existing);
                lst.add(value);
                out.put(key, lst);
            }
        }
        return out;
    }

    private static String decodeFormPart(String s) {
        try {
            return java.net.URLDecoder.decode(s.replace('+', ' '), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseFormBody(String raw, List<Map<String, Object>> properties) {
        Map<String, Object> pairs = parseFormPairs(raw);
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map<String, Object> prop : properties) {
            String name = (String) prop.get("name");
            String cardinality = (String) prop.get("cardinality");
            String kind = (String) prop.get("kind");
            String use = (String) prop.get("use");

            if ("many".equals(cardinality)) {
                List<Object> rawValues;
                if ("Ref".equals(kind)) {
                    Object existing = pairs.get(name);
                    if (existing instanceof List) rawValues = new ArrayList<>((List<Object>) existing);
                    else if (existing != null) { rawValues = new ArrayList<>(); rawValues.add(existing); }
                    else rawValues = new ArrayList<>();
                    rawValues.removeIf(v -> v == null || "".equals(v));
                } else {
                    Object single = pairs.get(name);
                    String text = single instanceof List ? String.join("\n", ((List<?>) single).stream().map(Object::toString).toList()) : (single == null ? "" : single.toString());
                    rawValues = new ArrayList<>();
                    for (String line : text.split("\\r?\\n")) {
                        String t = line.trim();
                        if (!t.isEmpty()) rawValues.add(t);
                    }
                }
                List<Object> coerced = new ArrayList<>();
                for (Object v : rawValues) {
                    Object c = coerceFormValue(v, prop);
                    if (c != null) coerced.add(c);
                }
                if (!coerced.isEmpty()) out.put(name, coerced);
            } else if ("InlineScalar".equals(kind) && "Boolean".equals(use)) {
                out.put(name, pairs.containsKey(name));
            } else {
                Object rawValue = pairs.get(name);
                Object v = coerceFormValue(rawValue, prop);
                if (v != null) out.put(name, v);
            }
        }
        return out;
    }

    public static Map<String, Object> formValuesFromItem(Map<String, Object> item, List<Map<String, Object>> properties) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (item == null) return out;
        for (Map<String, Object> p : properties) {
            String name = (String) p.get("name");
            if (item.containsKey(name)) out.put(name, item.get(name));
        }
        return out;
    }
}
