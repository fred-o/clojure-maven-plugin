/*
 * Created by IntelliJ IDEA.
 * User: amrk
 * Date: Apr 18, 2009
 * Time: 1:08:16 PM
 */
package com.theoryinpractise.clojure;

import java.lang.reflect.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.*;

import java.util.Map;
import org.apache.commons.exec.CommandLine;

import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

public abstract class AbstractClojureCompilerMojo extends AbstractMojo {
    
    /**
     * Classes to put onto the command line before the main class
     *
     * @parameter
     */
    private List<String> prependClasses;

	/**
	 * @parameter expression="${fork}" default-value="true"
	 */
	private boolean fork;

    protected void callClojureWith(
            File[] sourceDirectory,
            File outputDirectory,
            List<String> compileClasspathElements,
            String mainClass,
            String[] clojureArgs) throws MojoExecutionException {
		if (fork) {
			callClojureForked(sourceDirectory, outputDirectory, compileClasspathElements, mainClass, clojureArgs);
		} else {
			callClojureInProcess(sourceDirectory, outputDirectory, compileClasspathElements, mainClass, clojureArgs);
		}
	}

	/**
	 * Convenience method that adds a new classpath element to a list of URLs.
	 */
	private void addClasspathElement(File f, List<URL> urls) throws MalformedURLException {
		URL u = f.toURL();
		getLog().debug("Adding " + u);
		urls.add(u);
	}

	/**
	 * Like {@link AbstractClojureCompilerMojo#callClojureWith},
	 * except does not fork a new process.
	 */
    protected void callClojureInProcess(
            File[] sourceDirectory,
            File outputDirectory,
            List<String> compileClasspathElements,
            String mainClass,
            String[] clojureArgs) throws MojoExecutionException {
		try {
			List<URL> urls = new ArrayList<URL>();
			try {
				for(File d: sourceDirectory) {
					addClasspathElement(d, urls);
				}

				for(String e: compileClasspathElements) {
					addClasspathElement(new File(e), urls);
				}
			} catch (MalformedURLException e) {
				getLog().error(e);
			}
			
			ClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]));

			Class<?> cl = loader.loadClass(mainClass);
			Method m = cl.getMethod("main", new Class<?>[] { clojureArgs.getClass() });

			getLog().info("Executing " + cl.getName() + "#" + m.getName() + " in-process");

			System.setProperty("clojure.compile.path", outputDirectory.getPath());

			m.invoke(null, new Object[] { clojureArgs });

		} catch (Exception e) {
			getLog().error(e);
		} 		
	}

    protected void callClojureForked(
            File[] sourceDirectory,
            File outputDirectory,
            List<String> compileClasspathElements,
            String mainClass,
            String[] clojureArgs) throws MojoExecutionException {
    
        outputDirectory.mkdirs();
                
        String cp = "";
        for (File directory : sourceDirectory) {
            cp = cp + directory.getPath() + File.pathSeparator;
        }
    
        cp = cp + outputDirectory.getPath() + File.pathSeparator;
    
        for (Object classpathElement : compileClasspathElements) {
            cp = cp + File.pathSeparator + classpathElement;
        }
    
        getLog().debug("Clojure classpath: " + cp);
        CommandLine cl = new CommandLine("java");
    
        cl.addArgument("-cp");
        cl.addArgument(cp);
        cl.addArgument("-Dclojure.compile.path=" + outputDirectory.getPath() + "");
        
        if(prependClasses != null) {
            cl.addArguments(prependClasses.toArray(new String[prependClasses.size()]));          
        }
        
        cl.addArgument(mainClass);
        
        if (clojureArgs != null) {
            cl.addArguments(clojureArgs, false);
        }
        
        Executor exec = new DefaultExecutor();
        Map<String,String> env = new HashMap<String,String>(System.getenv());
        env.put("path", ";");
        env.put("path", System.getProperty("java.home"));
        
        ExecuteStreamHandler handler = new CustomPumpStreamHandler(System.out, System.err, System.in);
        exec.setStreamHandler(handler);
        
        int status;
        try {
            status = exec.execute(cl, env);
        } catch (ExecuteException e) {
            status = e.getExitValue();
        } catch(IOException e) {
            status = 1;
        }
        
        if (status != 0) {
            throw new MojoExecutionException("Clojure failed.");
        }
    }
}
