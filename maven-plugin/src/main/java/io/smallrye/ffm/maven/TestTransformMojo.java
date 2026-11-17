package io.smallrye.ffm.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The test transform Mojo.
 */
@Mojo(name = "transform-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true)
public final class TestTransformMojo extends AbstractTransformMojo {
    /**
     * The directory holding the project test classes.
     */
    @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true, property = "smallrye-ffm.testClassesDirectory")
    private File classesDirectory;

    /**
     * Skip the transformation of the test classes when the build skips the tests and their compilation.
     */
    @Parameter(defaultValue = "false", property = "maven.test.skip")
    private boolean mavenTestSkip;

    /**
     * Construct a new instance.
     */
    public TestTransformMojo() {
    }

    File classesDirectory() {
        return classesDirectory;
    }

    @Override
    boolean skipped() {
        return mavenTestSkip;
    }
}
