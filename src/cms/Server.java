package cms;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cms.views.Layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class Server {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE);
    private static final int MAX_BODY_SIZE = 1024 * 1024;

    @FunctionalInterface
    public interface RenderFn { Map<String, Object> apply(Map<String, Object> opts); }

    public static final class EntityRoute {
        public final String entity;
        public final String plural;
        public final RenderFn list;
        public final RenderFn detail;
        public final RenderFn createForm;
        public final RenderFn createSubmit;
        public final RenderFn editForm;
        public final RenderFn editSubmit;
        public final RenderFn deleteForm;
        public final RenderFn deleteSubmit;

        public EntityRoute(String entity, String plural,
                           RenderFn list, RenderFn detail,
                           RenderFn createForm, RenderFn createSubmit,
                           RenderFn editForm, RenderFn editSubmit,
                           RenderFn deleteForm, RenderFn deleteSubmit) {
            this.entity = entity; this.plural = plural;
            this.list = list; this.detail = detail;
            this.createForm = createForm; this.createSubmit = createSubmit;
            this.editForm = editForm; this.editSubmit = editSubmit;
            this.deleteForm = deleteForm; this.deleteSubmit = deleteSubmit;
        }
    }

    public static final List<EntityRoute> ROUTES = new ArrayList<>();
    static {
        ROUTES.add(new EntityRoute("BlogPosting", "blog-postings", cms.views.BlogPosting.ListView::render, cms.views.BlogPosting.DetailView::render, cms.views.BlogPosting.CreateView::renderForm, cms.views.BlogPosting.CreateView::handleSubmit, cms.views.BlogPosting.EditView::renderForm, cms.views.BlogPosting.EditView::handleSubmit, cms.views.BlogPosting.DeleteView::renderForm, cms.views.BlogPosting.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("Person", "persons", cms.views.Person.ListView::render, cms.views.Person.DetailView::render, cms.views.Person.CreateView::renderForm, cms.views.Person.CreateView::handleSubmit, cms.views.Person.EditView::renderForm, cms.views.Person.EditView::handleSubmit, cms.views.Person.DeleteView::renderForm, cms.views.Person.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("WebPage", "web-pages", cms.views.WebPage.ListView::render, cms.views.WebPage.DetailView::render, cms.views.WebPage.CreateView::renderForm, cms.views.WebPage.CreateView::handleSubmit, cms.views.WebPage.EditView::renderForm, cms.views.WebPage.EditView::handleSubmit, cms.views.WebPage.DeleteView::renderForm, cms.views.WebPage.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("ImageObject", "image-objects", cms.views.ImageObject.ListView::render, cms.views.ImageObject.DetailView::render, cms.views.ImageObject.CreateView::renderForm, cms.views.ImageObject.CreateView::handleSubmit, cms.views.ImageObject.EditView::renderForm, cms.views.ImageObject.EditView::handleSubmit, cms.views.ImageObject.DeleteView::renderForm, cms.views.ImageObject.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("CategoryCode", "category-codes", cms.views.CategoryCode.ListView::render, cms.views.CategoryCode.DetailView::render, cms.views.CategoryCode.CreateView::renderForm, cms.views.CategoryCode.CreateView::handleSubmit, cms.views.CategoryCode.EditView::renderForm, cms.views.CategoryCode.EditView::handleSubmit, cms.views.CategoryCode.DeleteView::renderForm, cms.views.CategoryCode.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("CategoryCodeSet", "category-code-sets", cms.views.CategoryCodeSet.ListView::render, cms.views.CategoryCodeSet.DetailView::render, cms.views.CategoryCodeSet.CreateView::renderForm, cms.views.CategoryCodeSet.CreateView::handleSubmit, cms.views.CategoryCodeSet.EditView::renderForm, cms.views.CategoryCodeSet.EditView::handleSubmit, cms.views.CategoryCodeSet.DeleteView::renderForm, cms.views.CategoryCodeSet.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("DefinedTerm", "defined-terms", cms.views.DefinedTerm.ListView::render, cms.views.DefinedTerm.DetailView::render, cms.views.DefinedTerm.CreateView::renderForm, cms.views.DefinedTerm.CreateView::handleSubmit, cms.views.DefinedTerm.EditView::renderForm, cms.views.DefinedTerm.EditView::handleSubmit, cms.views.DefinedTerm.DeleteView::renderForm, cms.views.DefinedTerm.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("DefinedTermSet", "defined-term-sets", cms.views.DefinedTermSet.ListView::render, cms.views.DefinedTermSet.DetailView::render, cms.views.DefinedTermSet.CreateView::renderForm, cms.views.DefinedTermSet.CreateView::handleSubmit, cms.views.DefinedTermSet.EditView::renderForm, cms.views.DefinedTermSet.EditView::handleSubmit, cms.views.DefinedTermSet.DeleteView::renderForm, cms.views.DefinedTermSet.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("Comment", "comments", cms.views.Comment.ListView::render, cms.views.Comment.DetailView::render, cms.views.Comment.CreateView::renderForm, cms.views.Comment.CreateView::handleSubmit, cms.views.Comment.EditView::renderForm, cms.views.Comment.EditView::handleSubmit, cms.views.Comment.DeleteView::renderForm, cms.views.Comment.DeleteView::handleSubmit));
        ROUTES.add(new EntityRoute("WebSite", "web-sites", cms.views.WebSite.ListView::render, cms.views.WebSite.DetailView::render, cms.views.WebSite.CreateView::renderForm, cms.views.WebSite.CreateView::handleSubmit, cms.views.WebSite.EditView::renderForm, cms.views.WebSite.EditView::handleSubmit, cms.views.WebSite.DeleteView::renderForm, cms.views.WebSite.DeleteView::handleSubmit));
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "4006"));
        String host = System.getenv().getOrDefault("HOST", "0.0.0.0");
        HttpServer server = create(host, port);
        server.start();
        System.err.println("CMS frontend running at http://" + host + ":" + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(2)));
    }

    public static HttpServer create(String host, int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/", new MainHandler());
        return server;
    }

    private static class MainHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            try {
                if ("GET".equals(method) && "/health".equals(path)) {
                    byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                    return;
                }
                if ("GET".equals(method) && "/style.css".equals(path)) {
                    serveStatic(exchange, "public/style.css", "text/css; charset=utf-8");
                    return;
                }
                if ("GET".equals(method) && "/".equals(path)) {
                    sendHtml(exchange, 200, indexPage());
                    return;
                }

                Match m = matchEntityRoute(path);
                if (m == null) {
                    sendHtml(exchange, 404, Layout.layout(Map.of("title", "Not Found", "body", "<p role=\"alert\">Page not found.</p>")));
                    return;
                }
                EntityRoute r = m.route;
                String kind = m.kind;
                String id = m.id;
                boolean idValid = id == null || UUID_PATTERN.matcher(id).matches();

                if ("list".equals(kind) && "GET".equals(method)) {
                    sendResponse(exchange, r.list.apply(Map.of("url", exchange.getRequestURI().toString())));
                    return;
                }
                if ("new".equals(kind)) {
                    if ("GET".equals(method)) { sendResponse(exchange, r.createForm.apply(Map.of())); return; }
                    if ("POST".equals(method)) {
                        String form = readFormBody(exchange);
                        Map<String, Object> result = r.createSubmit.apply(Map.of("form", form));
                        if (result.containsKey("redirect")) { sendRedirect(exchange, (String) result.get("redirect"), (int) result.getOrDefault("status", 303)); return; }
                        if (result.containsKey("html")) { sendHtml(exchange, (int) result.getOrDefault("status", 400), (String) result.get("html")); return; }
                        sendResponse(exchange, r.createForm.apply(Map.of("errors", result.getOrDefault("errors", List.of()), "values", result.getOrDefault("values", Map.of()))));
                        return;
                    }
                }
                if (!idValid) {
                    sendHtml(exchange, 400, Layout.layout(Map.of("title", "Invalid ID", "body", "<p role=\"alert\">ID must be a valid UUID.</p>")));
                    return;
                }
                if ("detail".equals(kind) && "GET".equals(method)) {
                    sendResponse(exchange, r.detail.apply(Map.of("id", id)));
                    return;
                }
                if ("edit".equals(kind)) {
                    if ("GET".equals(method)) { sendResponse(exchange, r.editForm.apply(Map.of("id", id))); return; }
                    if ("POST".equals(method)) {
                        String form = readFormBody(exchange);
                        Map<String, Object> result = r.editSubmit.apply(Map.of("id", id, "form", form));
                        if (result.containsKey("redirect")) { sendRedirect(exchange, (String) result.get("redirect"), (int) result.getOrDefault("status", 303)); return; }
                        if (result.containsKey("html")) { sendHtml(exchange, (int) result.getOrDefault("status", 400), (String) result.get("html")); return; }
                        Map<String, Object> retryOpts = new LinkedHashMap<>();
                        retryOpts.put("id", id);
                        retryOpts.put("errors", result.getOrDefault("errors", List.of()));
                        retryOpts.put("values", result.getOrDefault("values", Map.of()));
                        sendResponse(exchange, r.editForm.apply(retryOpts));
                        return;
                    }
                }
                if ("delete".equals(kind)) {
                    if ("GET".equals(method)) { sendResponse(exchange, r.deleteForm.apply(Map.of("id", id))); return; }
                    if ("POST".equals(method)) { sendResponse(exchange, r.deleteSubmit.apply(Map.of("id", id))); return; }
                }
                sendHtml(exchange, 405, Layout.layout(Map.of("title", "Method not allowed", "body", "<p role=\"alert\">Method not allowed.</p>")));
            } catch (Exception e) {
                System.err.println("[" + method + " " + path + "] " + e.getMessage());
                e.printStackTrace(System.err);
                try {
                    sendHtml(exchange, 500, Layout.layout(Map.of("title", "Error", "body", "<p role=\"alert\">Internal server error.</p>")));
                } catch (IOException ignored) {}
            } finally {
                exchange.close();
            }
        }
    }

    private static String indexPage() {
        StringBuilder items = new StringBuilder();
        for (EntityRoute r : ROUTES) {
            items.append("<li><a href=\"/").append(r.plural).append("\">").append(Layout.escapeHtml(r.entity)).append("</a></li>");
        }
        return Layout.layout(Map.of("title", "CMS", "body", "<p>Schema.org-aligned CMS frontend.</p><ul>" + items + "</ul>"));
    }

    private static final class Match {
        EntityRoute route; String kind; String id;
        Match(EntityRoute route, String kind, String id) { this.route = route; this.kind = kind; this.id = id; }
    }

    private static Match matchEntityRoute(String path) {
        for (EntityRoute r : ROUTES) {
            String base = "/" + r.plural;
            if (path.equals(base)) return new Match(r, "list", null);
            if (path.equals(base + "/new")) return new Match(r, "new", null);
            if (path.startsWith(base + "/")) {
                String rest = path.substring(base.length() + 1);
                int slash = rest.indexOf('/');
                if (slash < 0) return new Match(r, "detail", rest);
                String head = rest.substring(0, slash);
                String action = rest.substring(slash + 1);
                if (!action.equals("edit") && !action.equals("delete")) continue;
                return new Match(r, action, head);
            }
        }
        return null;
    }

    private static void sendHtml(HttpExchange exchange, int status, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
    }

    private static void sendRedirect(HttpExchange exchange, String location, int status) throws IOException {
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(status, -1);
    }

    @SuppressWarnings("unchecked")
    private static void sendResponse(HttpExchange exchange, Map<String, Object> response) throws IOException {
        if (response.containsKey("redirect")) {
            int status = (int) response.getOrDefault("status", 303);
            sendRedirect(exchange, (String) response.get("redirect"), status);
            return;
        }
        int status = (int) response.getOrDefault("status", 200);
        sendHtml(exchange, status, (String) response.get("html"));
    }

    private static void serveStatic(HttpExchange exchange, String relPath, String contentType) throws IOException {
        Path path = Path.of(relPath);
        if (!Files.isRegularFile(path)) {
            sendHtml(exchange, 404, Layout.layout(Map.of("title", "Not Found", "body", "<p role=\"alert\">Page not found.</p>")));
            return;
        }
        byte[] body = Files.readAllBytes(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=300");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
    }

    private static String readFormBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        int total = 0;
        while ((n = is.read(chunk)) > 0) {
            total += n;
            if (total > MAX_BODY_SIZE) return "";
            buf.write(chunk, 0, n);
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
