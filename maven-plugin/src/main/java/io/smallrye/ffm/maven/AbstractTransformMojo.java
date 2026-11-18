package io.smallrye.ffm.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;

/**
 * The base class for transformation mojos.
 */
public abstract class AbstractTransformMojo extends AbstractMojo {
    private static final ClassFile cf = ClassFile.of(ClassFile.StackMapsOption.STACK_MAPS_WHEN_REQUIRED);

    /**
     * Skip the transformation.
     */
    @Parameter(defaultValue = "false", property = "smallrye.ffm.skip")
    private boolean skip;

    /**
     * The project group ID, used in the path of the generated native image metadata.
     */
    @Parameter(defaultValue = "${project.groupId}", readonly = true)
    private String groupId;

    /**
     * The project artifact ID, used in the path of the generated native image metadata.
     */
    @Parameter(defaultValue = "${project.artifactId}", readonly = true)
    private String artifactId;

    /**
     * Write GraalVM native image metadata which registers the downcalls linked by the transformed classes.
     * The file is {@code META-INF/native-image/<groupId>/<artifactId>/smallrye-ffm/reachability-metadata.json}
     * in the classes directory; native-image reads it along with every other metadata file on the class path.
     * This is off by default.
     */
    @Parameter(defaultValue = "false", property = "smallrye.ffm.generateNativeMetadata")
    private boolean generateNativeMetadata;

    /**
     * Construct a new instance.
     */
    AbstractTransformMojo() {
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip || skipped()) {
            getLog().info("Skipping the transformation");
            return;
        }
        Path classes = classesDirectory().toPath();
        if (!Files.isDirectory(classes)) {
            getLog().info("Nothing to transform: " + classes + " does not exist");
            return;
        }
        getLog().info("Transforming classes in path: " + classes);
        NativeMetadata metadata = new NativeMetadata();
        int cnt = processPath(classes, metadata);
        if (generateNativeMetadata) {
            Path file = classes.resolve("META-INF").resolve("native-image").resolve(groupId).resolve(artifactId)
                    .resolve("smallrye-ffm").resolve("reachability-metadata.json");
            try {
                metadata.write(file);
            } catch (NativeMetadata.NotOursException e) {
                throw new MojoFailureException(e.getMessage(), e);
            } catch (IOException e) {
                throw new MojoFailureException("Failed to write " + file, e);
            }
            if (!metadata.downcalls().isEmpty()) {
                getLog().info("Registered " + metadata.downcalls().size() + " downcall shapes for native image in " + file);
            }
        }
        switch (cnt) {
            case 0 -> getLog().info("No classes transformed");
            case 1 -> getLog().info("Transformed 1 class");
            default -> getLog().info("Transformed " + cnt + " classes");
        }
    }

    private int processPath(final Path path, final NativeMetadata metadata) throws MojoFailureException {
        int cnt = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
            for (Path file : ds) {
                if (Files.isDirectory(file)) {
                    cnt += processPath(file, metadata);
                } else if (file.getFileName().toString().endsWith(".class")) {
                    ClassModel cm;
                    try {
                        cm = cf.parse(file);
                    } catch (IOException e) {
                        throw new MojoFailureException("Failed to parse " + file, e);
                    }
                    var resHolder = new Object() {
                        boolean res;
                    };
                    byte[] result = cf.transformClass(cm, (zb, ce) -> {
                        if (Generator.processElement(zb, cm, ce)) {
                            resHolder.res = true;
                        }
                    });
                    if (resHolder.res) {
                        cnt++;
                        metadata.addClass(cf.parse(result));
                        try {
                            Files.write(file, result);
                        } catch (IOException e) {
                            throw new MojoFailureException("Failed to write " + file, e);
                        }
                    } else {
                        metadata.addClass(cm);
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to read directory " + path, e);
        }
        return cnt;
    }

    abstract File classesDirectory();

    /**
     * {@return {@code true} if the goal has its own reason to skip, beyond the {@code skip} parameter}
     */
    boolean skipped() {
        return false;
    }
}
