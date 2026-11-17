package io.smallrye.ffm.maven;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The transform goals skip on request and do nothing for a missing classes directory.
 */
public final class TransformMojoSkipTest {

    @TempDir
    Path dir;

    public TransformMojoSkipTest() {
    }

    @Test
    public void testMissingClassesDirectoryIsNotAnError() throws Exception {
        CapturingLog log = new CapturingLog();
        TransformMojo mojo = mojo(new TransformMojo(), dir.resolve("classes").toFile(), log);
        mojo.execute();
        assertTrue(log.contains("does not exist"), log.lines.toString());
    }

    @Test
    public void testMissingTestClassesDirectoryIsNotAnError() throws Exception {
        CapturingLog log = new CapturingLog();
        TestTransformMojo mojo = mojo(new TestTransformMojo(), dir.resolve("test-classes").toFile(), log);
        mojo.execute();
        assertTrue(log.contains("does not exist"), log.lines.toString());
    }

    @Test
    public void testSkipParameter() throws Exception {
        CapturingLog log = new CapturingLog();
        TransformMojo mojo = mojo(new TransformMojo(), dir.toFile(), log);
        set(mojo, AbstractTransformMojo.class, "skip", true);
        mojo.execute();
        assertTrue(log.contains("Skipping"), log.lines.toString());
    }

    @Test
    public void testTestGoalHonoursMavenTestSkip() throws Exception {
        CapturingLog log = new CapturingLog();
        TestTransformMojo mojo = mojo(new TestTransformMojo(), dir.toFile(), log);
        set(mojo, TestTransformMojo.class, "mavenTestSkip", true);
        mojo.execute();
        assertTrue(log.contains("Skipping"), log.lines.toString());
    }

    @Test
    public void testEmptyDirectoryIsTransformed() throws Exception {
        CapturingLog log = new CapturingLog();
        TransformMojo mojo = mojo(new TransformMojo(), dir.toFile(), log);
        mojo.execute();
        assertTrue(log.contains("No classes transformed"), log.lines.toString());
    }

    private static <M extends AbstractTransformMojo> M mojo(M mojo, File classes, Log log) throws Exception {
        set(mojo, mojo.getClass(), "classesDirectory", classes);
        mojo.setLog(log);
        return mojo;
    }

    private static void set(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * A log that keeps every message.
     */
    static final class CapturingLog implements Log {
        final List<String> lines = new ArrayList<>();

        boolean contains(String text) {
            return lines.stream().anyMatch(line -> line.contains(text));
        }

        private void add(CharSequence content) {
            lines.add(String.valueOf(content));
        }

        public boolean isDebugEnabled() {
            return true;
        }

        public void debug(CharSequence content) {
            add(content);
        }

        public void debug(CharSequence content, Throwable error) {
            add(content);
        }

        public void debug(Throwable error) {
            add(String.valueOf(error));
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public void info(CharSequence content) {
            add(content);
        }

        public void info(CharSequence content, Throwable error) {
            add(content);
        }

        public void info(Throwable error) {
            add(String.valueOf(error));
        }

        public boolean isWarnEnabled() {
            return true;
        }

        public void warn(CharSequence content) {
            add(content);
        }

        public void warn(CharSequence content, Throwable error) {
            add(content);
        }

        public void warn(Throwable error) {
            add(String.valueOf(error));
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public void error(CharSequence content) {
            add(content);
        }

        public void error(CharSequence content, Throwable error) {
            add(content);
        }

        public void error(Throwable error) {
            add(String.valueOf(error));
        }
    }
}
