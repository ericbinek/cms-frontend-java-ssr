package cms.views.Person;

import cms.ApiClient;
import cms.views.Layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DetailView {

    public static final String ENTITY = "Person";
    public static final String BASE = "/persons";
    public static final List<Map<String, Object>> PROPERTIES = new ArrayList<>();
    static {
        PROPERTIES.add(Map.of("name", "name", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.TRUE));
        PROPERTIES.add(Map.of("name", "givenName", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "familyName", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "alternateName", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "email", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "url", "kind", "InlineScalar", "use", "URL", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "description", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "image", "kind", "Ref", "targets", List.of("ImageObject"), "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "jobTitle", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "sameAs", "kind", "InlineScalar", "use", "URL", "cardinality", "many", "required", Boolean.FALSE));
    }

    private DetailView() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> render(Map<String, Object> opts) {
        String id = (String) opts.get("id");
        ApiClient.Response r = ApiClient.get(ENTITY, id);
        if (r.status == 404) return Layout.errorPage(404, ENTITY + " not found.");
        if (r.status != 200) {
            String msg = "Failed to load.";
            if (r.body instanceof Map && ((Map<?, ?>) r.body).get("message") instanceof String) msg = (String) ((Map<?, ?>) r.body).get("message");
            return Layout.errorPage(r.status, msg);
        }
        Map<String, Object> item = (Map<String, Object>) r.body;
        StringBuilder rows = new StringBuilder();
        for (Map<String, Object> p : PROPERTIES) {
            rows.append("<dt>").append(Layout.escapeHtml(p.get("name"))).append("</dt>")
                .append("<dd>").append(Layout.formatValue(item.get(p.get("name")), p)).append("</dd>");
        }
        String meta =
            "<dt>id</dt><dd><code>" + Layout.escapeHtml(item.get("id")) + "</code></dd>" +
            "<dt>dateCreated</dt><dd><time datetime=\"" + Layout.escapeHtml(item.getOrDefault("dateCreated", "")) + "\">" + Layout.escapeHtml(item.getOrDefault("dateCreated", "")) + "</time></dd>" +
            "<dt>dateModified</dt><dd><time datetime=\"" + Layout.escapeHtml(item.getOrDefault("dateModified", "")) + "\">" + Layout.escapeHtml(item.getOrDefault("dateModified", "")) + "</time></dd>";
        String idEsc = Layout.escapeHtml(item.get("id"));
        String body =
            "<article>\n" +
            "<dl>" + rows + meta + "</dl>\n" +
            "<p>\n" +
            "<a href=\"" + BASE + "/" + idEsc + "/edit\">Edit</a> ·\n" +
            "<a href=\"" + BASE + "/" + idEsc + "/delete\">Delete</a> ·\n" +
            "<a href=\"" + BASE + "\">Back to list</a>\n" +
            "</p>\n" +
            "</article>";
        Map<String, Object> opts2 = new LinkedHashMap<>();
        opts2.put("title", Layout.displayName(item, ENTITY));
        opts2.put("currentEntity", ENTITY);
        opts2.put("body", body);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", 200);
        out.put("html", Layout.layout(opts2));
        return out;
    }
}
