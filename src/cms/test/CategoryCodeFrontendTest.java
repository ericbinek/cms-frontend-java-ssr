package cms.test;

import java.util.Map;
import java.util.regex.Pattern;

public final class CategoryCodeFrontendTest {

    public static final String ENTITY = "CategoryCode";
    public static final String BASE = "/category-codes";

    private CategoryCodeFrontendTest() {}

    public static void run(TestRunner.TestContext ctx) {
        ctx.test("GET list renders semantic page", () -> {
            Helpers.ensureEntity(ENTITY);
            Helpers.Response r = Helpers.frontendGet(BASE);
            Assert.equal(200, r.status);
            Assert.match(Pattern.compile("<table\\b"), r.body, "table");
            Assert.match(Pattern.compile("<caption>"), r.body, "caption");
            Assert.isTrue(r.body.contains(ENTITY), "body contains entity name");
        });

        ctx.test("GET /new renders a form", () -> {
            Helpers.Response r = Helpers.frontendGet(BASE + "/new");
            Assert.equal(200, r.status);
            Assert.match(Pattern.compile("<form[^>]+method=\"POST\""), r.body, "form");
        });

        ctx.test("POST /new with valid form redirects to detail", () -> {
            String body = Helpers.formBodyFor(ENTITY);
            Helpers.Response r = Helpers.frontendPostForm(BASE + "/new", body);
            Assert.equal(303, r.status);
            String loc = r.headers.get("location");
            Assert.isTrue(loc != null && loc.startsWith(BASE + "/"), "expected redirect to " + BASE + "/<id>, got " + loc);
        });

        ctx.test("POST /new with empty form returns 400 or 303", () -> {
            Helpers.Response r = Helpers.frontendPostForm(BASE + "/new", "");
            if (r.status == 303) return;
            Assert.equal(400, r.status);
            Assert.match(Pattern.compile("role=\"alert\""), r.body, "alert");
        });

        ctx.test("GET detail returns 200 with article markup", () -> {
            String id = Helpers.ensureEntity(ENTITY);
            Helpers.Response r = Helpers.frontendGet(BASE + "/" + id);
            Assert.equal(200, r.status);
            Assert.match(Pattern.compile("<article\\b"), r.body, "article");
            Assert.match(Pattern.compile("<dl>"), r.body, "dl");
            Assert.isTrue(r.body.contains(id), "body contains id");
        });

        ctx.test("GET edit renders pre-filled form", () -> {
            String id = Helpers.ensureEntity(ENTITY);
            Helpers.Response r = Helpers.frontendGet(BASE + "/" + id + "/edit");
            Assert.equal(200, r.status);
            Assert.match(Pattern.compile("<form[^>]+method=\"POST\""), r.body, "form");
        });

        ctx.test("POST edit redirects back to detail", () -> {
            String id = Helpers.ensureEntity(ENTITY);
            String body = Helpers.formBodyFor(ENTITY);
            Helpers.Response r = Helpers.frontendPostForm(BASE + "/" + id + "/edit", body);
            Assert.equal(303, r.status);
            Assert.equal(BASE + "/" + id, r.headers.get("location"));
        });

        ctx.test("GET delete renders confirmation form", () -> {
            String id = Helpers.ensureEntity(ENTITY);
            Helpers.Response r = Helpers.frontendGet(BASE + "/" + id + "/delete");
            Assert.equal(200, r.status);
            Assert.match(Pattern.compile("<form[^>]+method=\"POST\""), r.body, "form");
            Assert.isTrue(r.body.contains("Confirm Delete"), "body contains Confirm Delete");
        });

        ctx.test("POST delete redirects to list", () -> {
            String id = Helpers.ensureEntity(ENTITY);
            Helpers.Response r = Helpers.frontendPostForm(BASE + "/" + id + "/delete", "");
            Assert.equal(303, r.status);
            Assert.equal(BASE, r.headers.get("location"));
        });

        ctx.test("GET detail with non-UUID id returns 400 with alert", () -> {
            Helpers.Response r = Helpers.frontendGet(BASE + "/not-a-uuid");
            Assert.equal(400, r.status);
            Assert.match(Pattern.compile("role=\"alert\""), r.body, "alert");
        });

        ctx.test("GET detail of missing id renders 404 page", () -> {
            Helpers.Response r = Helpers.frontendGet(BASE + "/00000000-0000-0000-0000-000000000000");
            Assert.equal(404, r.status);
            Assert.match(Pattern.compile("role=\"alert\""), r.body, "alert");
        });

        ctx.test("navigation includes self link with aria-current", () -> {
            Helpers.ensureEntity(ENTITY);
            Helpers.Response r = Helpers.frontendGet(BASE);
            Assert.match(Pattern.compile("aria-current=\"page\""), r.body, "aria-current");
        });

        ctx.test("list view paginates with previous and next navigation", () -> {
            Helpers.seedWith(ENTITY, Map.of());
            Helpers.seedWith(ENTITY, Map.of());
            Helpers.seedWith(ENTITY, Map.of());

            Helpers.Response first = Helpers.frontendGet(BASE + "?limit=2&offset=0");
            Assert.equal(200, first.status);
            Assert.isTrue(first.body.contains("rel=\"next\""), "first page has a next link");
            Assert.isTrue(first.body.contains("offset=2"), "next link advances the offset by one page");
            Assert.isTrue(!first.body.contains("rel=\"prev\""), "first page has no previous link");

            Helpers.Response second = Helpers.frontendGet(BASE + "?limit=2&offset=2");
            Assert.equal(200, second.status);
            Assert.isTrue(second.body.contains("rel=\"prev\""), "second page has a previous link");
        });

        ctx.test("stored javascript: and data: URLs render as inert text, never as links", () -> {
            String jsId = Helpers.seedWith(ENTITY, Map.of("url", "javascript:alert(1)"));
            Helpers.Response js = Helpers.frontendGet(BASE + "/" + jsId);
            Assert.equal(200, js.status);
            Assert.isTrue(js.body.contains("javascript:alert(1)"), "body contains the inert value");
            Assert.isTrue(!js.body.contains("href=\"javascript:"), "javascript: value is not a clickable link");

            String dataId = Helpers.seedWith(ENTITY, Map.of("url", "data:text/html,x"));
            Helpers.Response data = Helpers.frontendGet(BASE + "/" + dataId);
            Assert.equal(200, data.status);
            Assert.isTrue(!data.body.contains("href=\"data:"), "data: value is not a clickable link");
        });

        ctx.test("stored http(s) URL renders as a clickable link", () -> {
            String id = Helpers.seedWith(ENTITY, Map.of("url", "https://example.com/profile"));
            Helpers.Response r = Helpers.frontendGet(BASE + "/" + id);
            Assert.equal(200, r.status);
            Assert.isTrue(r.body.contains("href=\"https://example.com/profile\""), "http(s) URL is a clickable link");
        });
    }
}
