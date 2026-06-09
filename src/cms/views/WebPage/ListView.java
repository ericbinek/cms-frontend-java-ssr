package cms.views.WebPage;

import cms.ApiClient;
import cms.views.Layout;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ListView {

    public static final String ENTITY = "WebPage";
    public static final String BASE = "/web-pages";
    public static final List<Map<String, Object>> PROPERTIES = new ArrayList<>();
    public static final List<String> EXTRA_COLS = List.of("datePublished");

    static {
        PROPERTIES.add(Map.of("name", "headline", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.TRUE));
        PROPERTIES.add(Map.of("name", "description", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "text", "kind", "InlineScalar", "use", "Text", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "author", "kind", "Ref", "targets", List.of("Person"), "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "primaryImageOfPage", "kind", "Ref", "targets", List.of("ImageObject"), "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "isPartOf", "kind", "Ref", "targets", List.of("WebPage"), "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "datePublished", "kind", "InlineScalar", "use", "DateTime", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "dateModified", "kind", "InlineScalar", "use", "DateTime", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "dateCreated", "kind", "InlineScalar", "use", "DateTime", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "url", "kind", "InlineScalar", "use", "URL", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "inLanguage", "kind", "Embed", "use", "Language", "cardinality", "one", "required", Boolean.FALSE));
        PROPERTIES.add(Map.of("name", "creativeWorkStatus", "kind", "Enum", "values", List.of("Draft", "Pending", "Published", "Archived"), "cardinality", "one", "required", Boolean.FALSE));
    }

    private ListView() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> render(Map<String, Object> opts) {
        String url = (String) opts.getOrDefault("url", BASE);
        Map<String, Object> query = new LinkedHashMap<>();
        String q = URI.create(url).getRawQuery();
        if (q != null) {
            for (String pair : q.split("&")) {
                int eq = pair.indexOf('=');
                if (eq < 0) continue;
                String k = pair.substring(0, eq);
                String v = pair.substring(eq + 1);
                if (k.equals("limit") || k.equals("offset") || k.equals("sort") || k.equals("order")) {
                    query.put(k, java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        }
        ApiClient.Response r = ApiClient.list(ENTITY, query);
        if (r.status != 200) {
            String msg = "unknown error";
            if (r.body instanceof Map) {
                Object m = ((Map<?, ?>) r.body).get("message");
                if (m instanceof String) msg = (String) m;
            }
            Map<String, Object> opts2 = new LinkedHashMap<>();
            opts2.put("title", ENTITY + "s");
            opts2.put("currentEntity", ENTITY);
            opts2.put("body", "<p role=\"alert\">Failed to load: " + Layout.escapeHtml(msg) + "</p>");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", r.status);
            out.put("html", Layout.layout(opts2));
            return out;
        }
        Map<String, Object> body = (Map<String, Object>) r.body;
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        Number total = (Number) body.get("total");

        StringBuilder headers = new StringBuilder();
        List<String> headerList = new ArrayList<>();
        headerList.add("Name");
        headerList.add("Created");
        headerList.addAll(EXTRA_COLS);
        headerList.add("Actions");
        for (String h : headerList) headers.append("<th scope=\"col\">").append(Layout.escapeHtml(h)).append("</th>");

        Map<String, Map<String, Object>> propByName = new LinkedHashMap<>();
        for (Map<String, Object> p : PROPERTIES) propByName.put((String) p.get("name"), p);

        StringBuilder rows = new StringBuilder();
        for (Map<String, Object> item : items) {
            StringBuilder extras = new StringBuilder();
            for (String col : EXTRA_COLS) {
                Map<String, Object> p = propByName.get(col);
                extras.append("<td>").append(p != null ? Layout.formatValue(item.get(col), p) : Layout.escapeHtml(item.getOrDefault(col, "").toString())).append("</td>");
            }
            String id = Layout.escapeHtml(item.get("id"));
            rows.append("<tr>\n")
                .append("<td><a href=\"").append(BASE).append("/").append(id).append("\">").append(Layout.escapeHtml(Layout.displayName(item, ENTITY))).append("</a></td>\n")
                .append("<td><time datetime=\"").append(Layout.escapeHtml(item.getOrDefault("dateCreated", ""))).append("\">").append(Layout.escapeHtml(item.getOrDefault("dateCreated", ""))).append("</time></td>\n")
                .append(extras).append("\n")
                .append("<td><a href=\"").append(BASE).append("/").append(id).append("/edit\">Edit</a> · <a href=\"").append(BASE).append("/").append(id).append("/delete\">Delete</a></td>\n")
                .append("</tr>");
        }
        if (rows.length() == 0) {
            int cols = 3 + EXTRA_COLS.size();
            rows.append("<tr><td colspan=\"").append(cols).append("\"><em>No items.</em></td></tr>");
        }

        String bodyHtml =
            "<p><a href=\"" + BASE + "/new\">New " + Layout.escapeHtml(ENTITY) + "</a></p>\n" +
            "<p>Showing " + items.size() + " of " + total + ".</p>\n" +
            "<table>\n" +
            "<caption>" + Layout.escapeHtml(ENTITY) + " list</caption>\n" +
            "<thead><tr>" + headers + "</tr></thead>\n" +
            "<tbody>" + rows + "</tbody>\n" +
            "</table>";
        Map<String, Object> layoutOpts = new LinkedHashMap<>();
        layoutOpts.put("title", ENTITY + "s");
        layoutOpts.put("currentEntity", ENTITY);
        layoutOpts.put("body", bodyHtml);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", 200);
        out.put("html", Layout.layout(layoutOpts));
        return out;
    }
}
