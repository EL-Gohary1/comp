package com.contractdetector.capture;

/**
 * Thread-local holder for the currently executing test's context.
 *
 * <p>{@link SchemaCaptureFilter} uses this to annotate each captured
 * {@link ApiResponseSample} with the test class and method that issued
 * the HTTP request, so that impact analysis can later trace schema changes
 * back to specific test methods.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>A JUnit extension or {@code @BeforeEach} setup method calls
 *       {@link #set(String, String)} at the start of each test.</li>
 *   <li>{@link SchemaCaptureFilter#filter} reads the context to populate
 *       the sample's {@code testClassName} and {@code testMethodName}.</li>
 *   <li>A {@code @AfterEach} teardown method (or the same extension) calls
 *       {@link #clear()} to prevent thread-local leaks.</li>
 * </ol>
 */
public final class TestContext {

    // ── Internal state ──────────────────────────────────────────────────────

    private static final ThreadLocal<String> TEST_CLASS_NAME  = new ThreadLocal<>();
    private static final ThreadLocal<String> TEST_METHOD_NAME = new ThreadLocal<>();

    // ── Constructor ─────────────────────────────────────────────────────────

    private TestContext() {
        // Utility class — no instances.
    }

    // ── Mutators ────────────────────────────────────────────────────────────

    /**
     * Sets both test identifiers for the current thread.
     *
     * @param className  simple or fully-qualified test class name
     * @param methodName test method name
     */
    public static void set(String className, String methodName) {
        TEST_CLASS_NAME.set(className);
        TEST_METHOD_NAME.set(methodName);
    }

    /** Removes both thread-local values to prevent memory leaks. */
    public static void clear() {
        TEST_CLASS_NAME.remove();
        TEST_METHOD_NAME.remove();
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    /**
     * @return the test class name set for the current thread,
     *         or {@code "unknown"} if not set.
     */
    public static String getClassName() {
        String val = TEST_CLASS_NAME.get();
        return val != null ? val : "unknown";
    }

    /**
     * @return the test method name set for the current thread,
     *         or {@code "unknown"} if not set.
     */
    public static String getMethodName() {
        String val = TEST_METHOD_NAME.get();
        return val != null ? val : "unknown";
    }
}
