/**
 * Copyright (C) Commission Junction Inc.
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

import java.util.*;
import java.io.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal combine
 * @phase compile
 */
public class ConcatenateMojo extends AbstractRestSpecMojo {
    /** 
     * @parameter expression="${project.build.outputDirectory}/RestSpec.js" 
     * @required 
     */ 
    protected File destinationFile; 

    public void execute() throws MojoExecutionException, MojoFailureException {
                
        List<File> jsons = new ArrayList<File>();
        for(final File sourceDir : findSourceDirectories()){
            jsons.addAll(getJsonFiles(sourceDir));
        }
        getLog().info("Concatenating " + jsons.size() + " files to " + destinationFile.getPath());
        output(jsons, destinationFile);
    }

    private static List<File> getJsonFiles(File dir) {
        List<File> allJs = new ArrayList<File>();
        File[] files = dir.listFiles();
        for (File file : files) {
            if(file.isDirectory()) {
                allJs.addAll(getJsonFiles(file));
            } else {
                if (file.getName().endsWith(".spec.json")) {
                    allJs.add(file);
                }
            }
        }
        return allJs;
    }

    private static void writeHeader(PrintWriter out) {
        out.println("/*THIS FILE HAS BEEN AUTOMATICALLY GENERATED*/");
        out.println();
        out.println("define(function() { return [");
        out.println();
    }

    private static void writeFooter(PrintWriter out) {
        out.println("];});");
    }

    private static void append(File file, PrintWriter out) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        while(line != null) {
            out.println(line);
            line = br.readLine();
        }
        br.close();
    }

    private static void output(List<File> files, File target) {
        try {
            PrintWriter p = new PrintWriter(target);
            writeHeader(p);

            Iterator<File> it = files.iterator();
            while(it.hasNext()) {
                File file = it.next();

                append(file, p);
                if (it.hasNext()) {
                    p.println(",");
                }
            }
            writeFooter(p);
            p.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
