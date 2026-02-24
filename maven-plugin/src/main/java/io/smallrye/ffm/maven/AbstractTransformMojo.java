package io.smallrye.ffm.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import io.smallrye.classfile.ClassFile;
import io.smallrye.classfile.ClassModel;

public abstract class AbstractTransformMojo extends AbstractMojo {
    private static final ClassFile cf = ClassFile.of(ClassFile.StackMapsOption.STACK_MAPS_WHEN_REQUIRED);

    public void execute() throws MojoExecutionException, MojoFailureException {
        Path classes = classesDirectory().toPath();
        getLog().info("Transforming classes in path: " + classes);
        int cnt = processPath(classes);
        switch (cnt) {
            case 0 -> getLog().info("No classes transformed");
            case 1 -> getLog().info("Transformed 1 class");
            default -> getLog().info("Transformed " + cnt + " classes");
        }
    }

    private int processPath(final Path path) throws MojoFailureException {
        int cnt = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
            for (Path file : ds) {
                if (Files.isDirectory(file)) {
                    cnt += processPath(file);
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
                        if (Generator.processElement(zb, ce)) {
                            resHolder.res = true;
                        }
                    });
                    if (resHolder.res) {
                        cnt++;
                        try {
                            Files.write(file, result);
                        } catch (IOException e) {
                            throw new MojoFailureException("Failed to write " + file, e);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new MojoFailureException("Failed to read directory " + path, e);
        }
        return cnt;
    }

    abstract File classesDirectory();
}
