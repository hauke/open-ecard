/****************************************************************************
 * Copyright (C) 2012 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the Open eCard App.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package org.openecard.maven.classlist;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.bind.annotation.XmlRegistry;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * Implementation of the {@code class-list} goal.
 * The plugin creates a file named in the {@code fileName} parameter and places it into the the directory named by the
 * parameter {@code outputDirectory}. The contents of the file are entries of fully qualified class names with all JAXB
 * ObjectFactory classes found below the directory named by the {@code classDirectory} parameter. The {@code excludes}
 * list can contain fully qualified class names which should not occur in the list.
 * <p>The plugin is executed in the {@code process-classes} phase. The goal is named {@code class-list}.</p>
 *
 * @author Tobias Wich
 */
@Mojo(name = "class-list", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
public class ClassList extends AbstractMojo {

    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File outputDirectory;
    /**
     * Name of the file.
     */
    @Parameter(defaultValue = "classes.lst")
    private String fileName;
    /**
     * List of excluded classes.
     */
    @Parameter
    private List<String> excludes;

    /**
     * List of directories to look at.
     */
    @Parameter(defaultValue = "${project.build.directory}/classes")
    private File classDirectory;

    @Override
    public void execute() throws MojoExecutionException {
	// correct input parameters
	if (excludes == null) {
	    excludes = Collections.emptyList();
	}

	File f = outputDirectory;

	if (! f.exists()) {
	    boolean created = f.mkdirs();
	    if (! created) {
		throw new MojoExecutionException("Directory could not be created: " + outputDirectory);
	    }
	}
	File touch = new File(f, fileName);

	try (PrintWriter pw = new PrintWriter(touch, "UTF-8")) {
	    // get list of all class files
	    for (String next : getClassFiles()) {
		pw.println(next);
	    }
	} catch (IOException e) {
	    throw new MojoExecutionException("Error creating file " + touch, e);
	}
    }

    private Set<String> getClassFiles() {
	Set<String> result = new TreeSet<>();

	getLog().info("Checking dir: " + classDirectory.getPath());
	result.addAll(getClassFiles(classDirectory));

	return result;
    }

    private Set<String> getClassFiles(File dir) {
	Set<String> result = new TreeSet<>();

	File[] files = dir.listFiles();

	for (File f : files) {
	    if (f.isDirectory()) {
		result.addAll(getClassFiles(f));
	    } else { // is a file
		String className = getClassName(f);
		if (className != null) {
		    result.add(className);
		}
	    }
	}

	return result;
    }

    private String getClassName(File classFile) {
	String name = null;
	try {
	    String classFilePath = classFile.getCanonicalPath();
	    String next = classFilePath;
	    if (next.endsWith(".class")) {
		next = next.substring(0, next.length() - 6);
		next = next.substring(classDirectory.getCanonicalPath().length() + 1);
		next = next.replace(File.separator, ".");
		// consult excludes list
		if (excludes.contains(next)) {
		    getLog().info("Excluding class because of exclude list: " + next);
		} else {
		    URL url = classDirectory.toURI().toURL();
		    URLClassLoader cl = new URLClassLoader(new URL[] {url});
		    Class<?> c = cl.loadClass(next);
		    if (c.isAnnotationPresent(XmlRegistry.class)) {
			getLog().debug("Adding class to list: " + next);
			name = next;
		    } else {
			getLog().info("Excluding class because of annotation mismatch: " + next);
		    }
		}
	    }
	} catch (IOException | ClassNotFoundException ex) {
	    getLog().warn(ex);
	}
	return name;
    }

}
