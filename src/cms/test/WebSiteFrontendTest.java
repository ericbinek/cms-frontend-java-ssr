package cms.test;

import java.util.regex.Pattern;

public final class WebSiteFrontendTest {

    public static final String ENTITY = "WebSite";
    public static final String BASE = "/web-sites";

    private WebSiteFrontendTest() {}

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
    }
}
