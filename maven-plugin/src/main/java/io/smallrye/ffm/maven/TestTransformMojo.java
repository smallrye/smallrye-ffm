package io.smallrye.ffm.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The test transform Mojo.
 */
@Mojo(name = "transform-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES)
public final class TestTransformMojo extends AbstractTransformMojo {
    /**
     * The directory holding the project test classes.
     */
    @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true, property = "smallrye-ffm.testClassesDirectory")
    private File classesDirectory;

    File classesDirectory() {
        return classesDirectory;
    }
}
