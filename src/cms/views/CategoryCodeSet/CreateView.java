package cms.views.CategoryCodeSet;

import cms.ApiClient;
import cms.views.Layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CreateView {

    public static final String ENTITY = "CategoryCodeSet";
    public static final String BASE = "/category-code-sets";
    public static final List<Map<String, Object>> PROPERTIES = new ArrayList<>();
    static {
        PROPERTIES.add(Map.of("name", "name", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.TRUE));
        PROPERTIES.add(Map.of("name", "description", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "url", "kind", "InlineScalar", "use", "URL", "cardinality", "one", "required", Boolean.FALSE));
    }

    private CreateView() {}

    private static Map<String, List<Map<String, String>>> loadRefOptions() { return Map.of(); }

    @SuppressWarnings("unchecked")
    private static List<String> extractErrorList(Object body) {
        if (body instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) body;
            Object details = m.get("details");
            if (details instanceof List && !((List<?>) details).isEmpty()) {
                List<String> out = new ArrayList<>();
                for (Object d : (List<?>) details) out.add(d.toString());
                return out;
            }
            Object message = m.get("message");
            if (message instanceof String) return List.of((String) message);
        }
        return List.of("Request failed.");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> renderForm(Map<String, Object> opts) {
        Map<String, Object> values = (Map<String, Object>) opts.getOrDefault("values", Map.of());
        List<String> errors = (List<String>) opts.getOrDefault("errors", List.of());
        Map<String, List<String>> fieldErrors = (Map<String, List<String>>) opts.getOrDefault("fieldErrors", Map.of());
        Map<String, List<Map<String, String>>> refOptions = loadRefOptions();
        StringBuilder fields = new StringBuilder();
        for (Map<String, Object> p : PROPERTIES) {
            fields.append(Layout.renderField(p, values.get(p.get("name")), refOptions, fieldErrors.getOrDefault(p.get("name"), List.of()))).append("\n");
        }
        StringBuilder errorBlock = new StringBuilder();
        if (!errors.isEmpty()) {
            errorBlock.append("<div role=\"alert\"><p>Could not save:</p><ul>");
            for (String e : errors) errorBlock.append("<li>").append(Layout.escapeHtml(e)).append("</li>");
            errorBlock.append("</ul></div>");
        }
        String body = errorBlock + "\n" +
            "<form method=\"POST\" action=\"" + BASE + "/new\">\n" +
            fields +
            "<p><button type=\"submit\">Create</button> · <a href=\"" + BASE + "\">Cancel</a></p>\n" +
            "</form>";
        Map<String, Object> opts2 = new LinkedHashMap<>();
        opts2.put("title", "New " + ENTITY);
        opts2.put("currentEntity", ENTITY);
        opts2.put("body", body);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", errors.isEmpty() ? 200 : 400);
        out.put("html", Layout.layout(opts2));
        return out;
    }

    public static Map<String, Object> handleSubmit(Map<String, Object> opts) {
        String form = (String) opts.getOrDefault("form", "");
        Map<String, Object> payload = Layout.parseFormBody(form, PROPERTIES);
        ApiClient.Response r = ApiClient.create(ENTITY, payload);
        if (r.status == 201 && r.body instanceof Map && ((Map<?, ?>) r.body).get("id") != null) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", 303);
            out.put("redirect", BASE + "/" + ((Map<?, ?>) r.body).get("id"));
            return out;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", 400);
        out.put("errors", extractErrorList(r.body));
        out.put("values", payload);
        return out;
    }
}
