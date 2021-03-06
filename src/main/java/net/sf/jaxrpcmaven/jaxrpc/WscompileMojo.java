package net.sf.jaxrpcmaven.jaxrpc;

import com.sun.xml.rpc.tools.wscompile.CompileTool;
import java.net.MalformedURLException;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import org.apache.commons.lang.SystemUtils;

/**
 * A wscompile mojo for maven 2.
 *
 * See http://download.oracle.com/docs/cd/E17802_01/webservices/webservices/docs/1.5/jaxrpc/jaxrpc-tools.html
  * for detailed manual.
 * @author <a href="ludek.h@gmail.com">Ludek Hlavacek</a>
 * @goal wscompile
 * @requiresDependencyResolution compile
 * @requiresDependencyResolution runtime
 * @requiresProject
 * @phase process-classes
 * @describe wscompile mojo
 */
public class WscompileMojo extends AbstractMojo {

	/**
	 * The maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;


        /**
	 * operation
	 * 
	 * @parameter
	 * @required
	 */
	private String operation;

	/**
	 * enable the given features
	 *
	 * @parameter
	 */
	private String features;

	/**
	 * specify a HTTP proxy server
	 *
	 * @parameter
	 */
	private ProxyConfiguration httpProxy;

    /**
     * output messages about what the compiler is doing
     *
     * @parameter default-value="false"
     */
    private boolean verbose;

    /**
     * keep generated files
     *
     * @parameter default-value="false"
     */
    private boolean keep;

    /**
     * generate debugging info
     *
     * @parameter default-value="false"
     */
    private boolean debug;

    /**
     * optimize generated code
     *
     * @parameter default-value="false"
     */
    private boolean optimize;

	/**
	 * generate a J2EE mapping.xml file
	 * 
	 * @parameter
	 */
	private File mapping;


	/**
	 * write the internal model to the given file
	 * 
	 * @parameter
	 */
	private File model;

	/**
	 * specify where to place non-class generated files
	 * 
	 * @parameter
	 */
	private File nd;

	/**
	 * specify where to place generated source files
	 *
	 * @parameter default-value="${project.build.directory}/generated-sources/jaxrpc"
	 */
	private File s;
	/**
	 * Should be generated source files added to compile path?
	 *
	 * @parameter default-value="true"
	 */
	private boolean addSources;

	/**
	 * specify where to place generated output files
	 *
	 * @parameter default-value="${project.build.outputDirectory}"
	 */
	private File d;

	/**
	 * Generate code for the specified JAX-RPC SI version. 
	 * Supported versions are: 1.0.1, 1.0.3, and 1.1 (default).
	 *
	 * @parameter
	 */
	private String source;


	/**
	 * configuration-file
	 *
	 * @required
	 * @parameter
	 */
	private String config;


        private void addToolsToCL()
        {
            try {
                URL url = new URL("jar:file://" + findToolsJar() + "!/");
                ClassLoader cl = new URLClassLoader(new URL[] { url }, getClass().getClassLoader());
                Thread.currentThread().setContextClassLoader(cl);
            } catch (MalformedURLException e) {
                getLog().warn("Failed to add tools.jar to classpath");
            }
        }

	public void execute() throws MojoExecutionException, MojoFailureException {
		
		List args = new ArrayList();

		args.add("-"+operation);
		
		args.add("-cp");
		args.add(getCp());

		if (features != null) {
		    args.add("-features:"+features);
		}
		
		if (keep) {
		    args.add("-keep");
		}

		if (debug) {
		    args.add("-g");
		}
		if (optimize) {
		    args.add("-O");
		}
		if ((httpProxy != null) && (httpProxy.getHost() != null)) {
		    args.add("-httpproxy:"+httpProxy.getHost()+":"+httpProxy.getPort());
		}
		
		if (mapping != null) {
		    args.add("-mapping");
		    args.add(mapping.getAbsolutePath());
		}
		if (model != null) {
		    args.add("-model");
		    args.add(model.getAbsolutePath());
		}
		if (nd != null) {
		    args.add("-nd");
		    args.add(nd.getAbsolutePath());
		    nd.mkdirs();
		}
		if (s != null) {
		    args.add("-s");
		    args.add(s.getAbsolutePath());
		    s.mkdirs();
                    if (addSources) {
                        project.addCompileSourceRoot(s.getAbsolutePath());
                    }
		}
		if (d != null) {
		    args.add("-d");
		    args.add(d.getAbsolutePath());
		    d.mkdirs();
		}
		if (source != null) {
		    args.add("-source");
		    args.add(source);
		}
		if (verbose) {
		    args.add("-verbose");
		}
		
		args.add(config);

		String[] strArgs = (String[]) args.toArray(new String[args.size()]);
		getLog().info(Arrays.toString(strArgs));

                addToolsToCL();
                
		CompileTool tool = new CompileTool(System.out, "wscompile");
                boolean result = tool.run(strArgs);
                if (!result) {
                    throw new MojoFailureException("Wscompile failed");
                }
		getLog().info("Wscompile succeeded");
	}

	
        
    /**
    * Returns the an isolated classloader.
    *
    * @return cpasspath list
    * @noinspection unchecked
    */
    private String getCp()
    {

        Set cp = new HashSet();

        try {
	    List classpathElements = project.getCompileClasspathElements();
            for (int i = 0; i < classpathElements.size(); i++)
            {
                //getLog().info("CP: "+classpathElements.get(i));
                cp.add(classpathElements.get(i));
            }
        } catch (DependencyResolutionRequiredException ex) {
            getLog().error("setup classpath: ", ex);
            return "";
        }

        cp.add(project.getBuild().getOutputDirectory());
        
        cp.add(findToolsJar());
    
        Iterator cpIter = cp.iterator();
        StringBuffer oBuilder = new StringBuffer(2000);
        oBuilder.append(String.valueOf(cpIter.next()));
        while ( cpIter.hasNext() )
            oBuilder.append( File.pathSeparator ).append( cpIter.next() );
     
        return oBuilder.toString();
    }


    /**
     * Figure out where the tools.jar file lives.
     */
    private File findToolsJar() {
        File javaHome = new File(System.getProperty("java.home"));
       

        File file = null;
	try {
        	if (SystemUtils.IS_OS_MAC_OSX) {
        	    file = new File(javaHome, "../Classes/classes.jar").getCanonicalFile();
        	}
        	else {
        	    file = new File(javaHome, "../lib/tools.jar").getCanonicalFile();
        	}
        } catch (IOException ex) {
		getLog().error("Couldn't find tools.jar.", ex);
	}
	
        if ((file == null) || !file.exists()) {
            getLog().error("Missing tools.jar at: $file");
        }
        
        getLog().debug("Using tools.jar: " + file);
        
        return file;
    }

}
