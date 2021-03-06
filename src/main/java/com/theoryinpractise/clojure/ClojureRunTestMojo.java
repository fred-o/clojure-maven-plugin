package com.theoryinpractise.clojure;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plugin for Clojure source compiling.
 * <p/>
 * (C) Copyright Tim Dysinger   (tim -on- dysinger.net)
 * Mark Derricutt (mark -on- talios.com)
 * Dimitry Gashinsky (dimitry -on- gashinsky.com)
 * <p/>
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * @goal test
 * @phase test
 * @requiresDependencyResolution test
 */
public class ClojureRunTestMojo extends AbstractClojureCompilerMojo {
    /**
     * Location of the file.
     *
     * @parameter default-value="${project.build.testOutputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Flag to allow test compiliation to be skipped.
     *
     * @parameter expression="${maven.test.skip}" default-value="false"
     * @noinspection UnusedDeclaration
     */
    private boolean skip;

    /**
     * Location of the source files.
     *
     * @parameter default-value="${project.build.testSourceDirectory}"
     * @required
     */
    private File baseTestSourceDirectory;

    /**
     * Location of the source files.
     *
     * @parameter
     */
    private File[] sourceDirectories = new File[] {new File("src/main/clojure")};

    /**
     * Location of the source files.
     *
     * @parameter
     */
    private File[] testSourceDirectories = new File[] {new File("src/test/clojure")};

    /**
     * Project classpath.
     *
     * @parameter default-value="${project.testClasspathElements}"
     * @required
     * @readonly
     */
    private List<String> classpathElements;

    /**
     * The main clojure script to run
     *
     * @parameter
     */
    private String testScript;

    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Test execution is skipped");
        } else {
            if (testScript == null || "".equals(testScript) || !(new File(testScript).exists())) {
                throw new MojoExecutionException("testScript is empty or does not exist!");
            } else {
                List<File> dirs = new ArrayList<File>();
                if (baseTestSourceDirectory != null) {
                    dirs.add(baseTestSourceDirectory);
                }
                if (testSourceDirectories != null) {
                    dirs.addAll(Arrays.asList(testSourceDirectories));
                }
                if (sourceDirectories != null) {
                    dirs.addAll(Arrays.asList(sourceDirectories));
                }

                callClojureWith(dirs.toArray(new File[]{}), outputDirectory, classpathElements, "clojure.main", new String[]{testScript});
            }
        }
    }

}
