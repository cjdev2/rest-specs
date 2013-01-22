/**
 * Copyright (C) 2011, 2012, 2013 Commission Junction Inc.
 * 
 * This file is part of rest-specs.
 * 
 * rest-specs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * 
 * rest-specs is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with rest-specs; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 * 
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 * 
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.cj.restspecs.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import cj.restspecs.core.RestSpecValidator;
import cj.restspecs.core.io.Loader;

/**
 * @goal validate
 * @phase compile
 */
public class ValidateMojo extends AbstractRestSpecMojo {
    
    public ValidateMojo() {}

    public ValidateMojo(File basedir, List<String> directories, List<String> excludes) {
	super();
	this.basedir = basedir;
	this.directories.addAll(directories);
	this.excludes.addAll(excludes);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
	
	for(final File targetPath : findSourceDirectories()){
	    
	    final Loader loader = new Loader() {
	        
	        public InputStream load(String name) {
	            try {
	        	File theFile = new File(targetPath, name);
			return new FileInputStream(theFile);
		    } catch (Exception e) {
			throw new RuntimeException(e);
		    }
	        }
	    };
	    
	    try {
		new RestSpecValidator(targetPath, loader, System.out).validate(excludes);
	    } catch (Exception e) {
		throw new MojoFailureException(e.getMessage(), e);
	    }
	}

    }

}
