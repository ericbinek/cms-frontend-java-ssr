package cms.views.Comment;

import cms.ApiClient;
import cms.views.Layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DetailView {

    public static final String ENTITY = "Comment";
    public static final String BASE = "/comments";
    public static final List<Map<String, Object>> PROPERTIES = new ArrayList<>();
    static {
        PROPERTIES.add(Map.of("name", "text", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.TRUE));
        PROPERTIES.add(Map.of("name", "author", "kind", "Ref", "targets", List.of("Person"), "cardinality", "one", "required", Boolean.TRUE));
        PROPERTIES.add(Map.of("name", "about", "kind", "Ref", "targets", List.of("BlogPosting"), "cardinality", "one", "required", Boolean.TRUE));
        PROPERTIES.add(Map.of("name", "parentItem", "kind", "Ref", "targets", List.of("Comment"), "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "dateCreated", "kind", "InlineScalar", "use", "DateTime", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "dateModified", "kind", "InlineScalar", "use", "DateTime", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "upvoteCount", "kind", "InlineScalar", "use", "Integer", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "downvoteCount", "kind", "InlineScalar", "use", "Integer", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "creativeWorkStatus", "kind", "Enum", "values", List.of("Pending", "Approved", "Spam", "Trash"), "cardinality", "one", "required", Boolean.FALSE));
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
