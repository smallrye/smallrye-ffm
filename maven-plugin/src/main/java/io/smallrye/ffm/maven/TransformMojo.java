package io.smallrye.ffm.maven;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * The transform Mojo.
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public final class TransformMojo extends AbstractTransformMojo {
    /**
     * The directory holding the project classes.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true, property = "smallrye-ffm.classesDirectory")
    private File classesDirectory;

    File classesDirectory() {
        return classesDirectory;
    }
}
