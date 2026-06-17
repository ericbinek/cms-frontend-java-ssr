package cms.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class TestRunner {

    public static final AtomicInteger PASS = new AtomicInteger(0);
    public static final AtomicInteger FAIL = new AtomicInteger(0);

    public static int findFreePort() {
        for (int port = 15000 + (int)(Math.random() * 1000); port < 17999; port++) {
            try (Socket s = new Socket()) {
                s.bind(new InetSocketAddress("127.0.0.1", port));
                return port;
            } catch (IOException ignored) {}
        }
        throw new RuntimeException("No free port found");
    }

    public static void main(String[] args) throws Exception {
        Path mockDataDir = Files.createTempDirectory("cms-mock-java-");

        int mockPort = findFreePort();
        ProcessBuilder mockPb = new ProcessBuilder("java", "-cp", "out", "cms.test.MockApi");
        mockPb.environment().put("PORT", String.valueOf(mockPort));
        mockPb.environment().put("MOCK_DATA_DIR", mockDataDir.toString());
        mockPb.redirectError(ProcessBuilder.Redirect.DISCARD);
        mockPb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process mock = mockPb.start();

        int frontPort = findFreePort();
        while (frontPort == mockPort) frontPort = findFreePort();
        ProcessBuilder frontPb = new ProcessBuilder("java", "-cp", "out", "cms.Server");
        frontPb.environment().put("PORT", String.valueOf(frontPort));
        frontPb.environment().put("API_BASE_URL", "http://127.0.0.1:" + mockPort);
        frontPb.redirectError(ProcessBuilder.Redirect.DISCARD);
        frontPb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process front = frontPb.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            front.destroy();
            mock.destroy();
            try { front.waitFor(); } catch (InterruptedException ignored) {}
            try { mock.waitFor(); } catch (InterruptedException ignored) {}
            deleteRecursive(mockDataDir);
        }));

        Helpers.setApiBase("http://127.0.0.1:" + mockPort);
        Helpers.setFrontendBase("http://127.0.0.1:" + frontPort);
        if (!Helpers.waitForHealth(Helpers.getApiBase(), 10_000)) {
            front.destroy(); mock.destroy();
            System.err.println("Mock API did not become healthy");
            System.exit(2);
        }
        if (!Helpers.waitForHealth(Helpers.getFrontendBase(), 10_000)) {
            front.destroy(); mock.destroy();
            System.err.println("Frontend did not become healthy");
            System.exit(2);
        }

        TestContext ctx = new TestContext();

        run("BlogPosting", cms.test.BlogPostingFrontendTest::run, ctx);
        run("Person", cms.test.PersonFrontendTest::run, ctx);
        run("Organization", cms.test.OrganizationFrontendTest::run, ctx);
        run("WebPage", cms.test.WebPageFrontendTest::run, ctx);
        run("ImageObject", cms.test.ImageObjectFrontendTest::run, ctx);
        run("VideoObject", cms.test.VideoObjectFrontendTest::run, ctx);
        run("AudioObject", cms.test.AudioObjectFrontendTest::run, ctx);
        run("CategoryCode", cms.test.CategoryCodeFrontendTest::run, ctx);
        run("CategoryCodeSet", cms.test.CategoryCodeSetFrontendTest::run, ctx);
        run("DefinedTerm", cms.test.DefinedTermFrontendTest::run, ctx);
        run("DefinedTermSet", cms.test.DefinedTermSetFrontendTest::run, ctx);
        run("Comment", cms.test.CommentFrontendTest::run, ctx);
        run("WebSite", cms.test.WebSiteFrontendTest::run, ctx);
        run("SiteNavigationElement", cms.test.SiteNavigationElementFrontendTest::run, ctx);

        int total = PASS.get() + FAIL.get();
        System.out.println();
        System.out.println("# tests " + total);
        System.out.println("# pass " + PASS.get());
        System.out.println("# fail " + FAIL.get());
        front.destroy();
        mock.destroy();
        front.waitFor();
        mock.waitFor();
        deleteRecursive(mockDataDir);
        System.exit(FAIL.get() > 0 ? 1 : 0);
    }

    private static void run(String entity, Consumer<TestContext> tests, TestContext ctx) {
        ctx.currentEntity = entity;
        Helpers.resetSeedCache();
        tests.accept(ctx);
    }

    public static void recordPass(String name) {
        System.out.println("ok - " + name);
        PASS.incrementAndGet();
    }

    public static void recordFail(String name, Throwable e) {
        System.out.println("not ok - " + name);
        System.out.println("  " + e.getMessage());
        for (StackTraceElement el : e.getStackTrace()) System.out.println("  at " + el);
        FAIL.incrementAndGet();
    }

    private static void deleteRecursive(Path path) {
        try {
            if (!Files.exists(path)) return;
            try (var stream = Files.walk(path)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        } catch (IOException ignored) {}
    }

    public static class TestContext {
        public String currentEntity;

        public void test(String name, ThrowingRunnable fn) {
            String fullName = currentEntity + ": " + name;
            try {
                Helpers.resetSeedCache();
                fn.run();
                recordPass(fullName);
            } catch (Throwable e) {
                recordFail(fullName, e);
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
