package cms;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import cms.views.Layout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class Server {

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE);

    @FunctionalInterface
    public interface RenderFn { Map<String, Object> apply(Map<String, Object> opts); }

    public static final class EntityRoute {
        public final String entity;
        public final String plural;
        public final RenderFn list;
        public final RenderFn detail;

        public EntityRoute(String entity, String plural, RenderFn list, RenderFn detail) {
            this.entity = entity; this.plural = plural;
            this.list = list; this.detail = detail;
        }
    }

    public static final List<EntityRoute> ROUTES = new ArrayList<>();
    static {
        ROUTES.add(new EntityRoute("BlogPosting", "blog-postings", cms.views.BlogPosting.ListView::render, cms.views.BlogPosting.DetailView::render));
        ROUTES.add(new EntityRoute("Person", "persons", cms.views.Person.ListView::render, cms.views.Person.DetailView::render));
        ROUTES.add(new EntityRoute("Organization", "organizations", cms.views.Organization.ListView::render, cms.views.Organization.DetailView::render));
        ROUTES.add(new EntityRoute("WebPage", "web-pages", cms.views.WebPage.ListView::render, cms.views.WebPage.DetailView::render));
        ROUTES.add(new EntityRoute("ImageObject", "image-objects", cms.views.ImageObject.ListView::render, cms.views.ImageObject.DetailView::render));
        ROUTES.add(new EntityRoute("VideoObject", "video-objects", cms.views.VideoObject.ListView::render, cms.views.VideoObject.DetailView::render));
        ROUTES.add(new EntityRoute("AudioObject", "audio-objects", cms.views.AudioObject.ListView::render, cms.views.AudioObject.DetailView::render));
        ROUTES.add(new EntityRoute("CategoryCode", "category-codes", cms.views.CategoryCode.ListView::render, cms.views.CategoryCode.DetailView::render));
        ROUTES.add(new EntityRoute("CategoryCodeSet", "category-code-sets", cms.views.CategoryCodeSet.ListView::render, cms.views.CategoryCodeSet.DetailView::render));
        ROUTES.add(new EntityRoute("DefinedTerm", "defined-terms", cms.views.DefinedTerm.ListView::render, cms.views.DefinedTerm.DetailView::render));
        ROUTES.add(new EntityRoute("DefinedTermSet", "defined-term-sets", cms.views.DefinedTermSet.ListView::render, cms.views.DefinedTermSet.DetailView::render));
        ROUTES.add(new EntityRoute("Comment", "comments", cms.views.Comment.ListView::render, cms.views.Comment.DetailView::render));
        ROUTES.add(new EntityRoute("WebSite", "web-sites", cms.views.WebSite.ListView::render, cms.views.WebSite.DetailView::render));
        ROUTES.add(new EntityRoute("SiteNavigationElement", "site-navigation-elements", cms.views.SiteNavigationElement.ListView::render, cms.views.SiteNavigationElement.DetailView::render));
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
                if (!"GET".equals(method) && !"HEAD".equals(method)) {
                    sendHtml(exchange, 404, Layout.layout(Map.of("title", "Not Found", "body", "<p role=\"alert\">Page not found.</p>")));
                    return;
                }
                if ("/health".equals(path)) {
                    byte[] body = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                    return;
                }
                if ("/style.css".equals(path)) {
                    serveStatic(exchange, "public/style.css", "text/css; charset=utf-8");
                    return;
                }
                if ("/".equals(path)) {
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

                if ("list".equals(kind)) {
                    sendResponse(exchange, r.list.apply(Map.of("url", exchange.getRequestURI().toString())));
                    return;
                }
                if ("detail".equals(kind)) {
                    if (!UUID_PATTERN.matcher(id).matches()) {
                        sendHtml(exchange, 400, Layout.layout(Map.of("title", "Invalid ID", "body", "<p role=\"alert\">ID must be a valid UUID.</p>")));
                        return;
                    }
                    sendResponse(exchange, r.detail.apply(Map.of("id", id)));
                    return;
                }
                sendHtml(exchange, 404, Layout.layout(Map.of("title", "Not Found", "body", "<p role=\"alert\">Page not found.</p>")));
            } catch (Exception e) {
                System.err.println("[" + method + " " + path + "] " + e.getMessage());
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
            if (path.startsWith(base + "/")) {
                String rest = path.substring(base.length() + 1);
                // Read-only frontend: only a single id segment is a route. Any
                // deeper path (a would-be write action) is not matched and 404s.
                if (rest.isEmpty() || rest.indexOf('/') >= 0) continue;
                return new Match(r, "detail", rest);
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

    private static void sendResponse(HttpExchange exchange, Map<String, Object> response) throws IOException {
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
}
