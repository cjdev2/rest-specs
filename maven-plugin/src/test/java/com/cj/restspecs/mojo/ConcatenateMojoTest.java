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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

public class ConcatenateMojoTest {

    @After
    public void removeTheFiles() throws Exception {
        for(FilesystemScenario next : FilesystemScenario.instances){
            next.delete();
        }
    }

    @Test
    public void theDestinationFileIsConfigurable() throws Exception {
        FilesystemScenario mavenProject = new FilesystemScenario();
        
        File destinationFile = new File(mavenProject.targetDir, "MyRestSpec.js");
        
        ConcatenateMojo mojo = new ConcatenateMojo();
        mojo.directories.add(Util.relativePath(mavenProject.root, mavenProject.srcDir));
        mojo.basedir = mavenProject.root;
        mojo.destinationFile = destinationFile;
        mojo.execute();
        
        String fileContents = FileUtils.readFileToString(destinationFile);
        //target should contain strings apple, root, banana, but not cat.
        assertTrue(fileContents.contains("apple"));
        assertTrue(fileContents.contains("banana"));
        assertTrue(fileContents.contains("root"));
        assertFalse("The matcher accepted something that doesn't end in .spec.js", fileContents.contains("cat"));
        assertEquals(1, mavenProject.targetDir.list().length);

    }
    
    @Test
    public void testConcatenator() throws Exception {
        FilesystemScenario mavenProject = new FilesystemScenario();
        
        File destinationFile = new File(mavenProject.targetDir, "RestSpec.js");
        
        ConcatenateMojo mojo = new ConcatenateMojo();
        mojo.directories.add(Util.relativePath(mavenProject.root, mavenProject.srcDir));
        mojo.basedir = mavenProject.root;
        mojo.destinationFile = destinationFile;
        mojo.execute();
        
        String fileContents = FileUtils.readFileToString(destinationFile);
        //target should contain strings apple, root, banana, but not cat.
        assertTrue(fileContents.contains("apple"));
        assertTrue(fileContents.contains("banana"));
        assertTrue(fileContents.contains("root"));
        assertFalse("The matcher accepted something that doesn't end in .spec.js", fileContents.contains("cat"));

    }

    @Test
    public void testHeaderAndFooter() throws Exception {
        // GIVEN
        FilesystemScenario mavenProject = new FilesystemScenario();

        File destinationFile = new File(mavenProject.targetDir, "RestSpec.js");
        
        ConcatenateMojo mojo = new ConcatenateMojo();
        mojo.directories.add(Util.relativePath(mavenProject.root, mavenProject.srcDir));
        mojo.basedir = mavenProject.root;
        mojo.destinationFile = destinationFile;

        // WHEN
        mojo.execute();

        // THEN
        String[] actualLines = FileUtils.readLines(destinationFile).toArray(new String[]{});
        String[] expectedHeaderLines = {
                "/*THIS FILE HAS BEEN AUTOMATICALLY GENERATED*/",
                "",
                "(function(){",
                "var defineShim = (typeof define !== 'undefined') ? define : function(func){window.RestSpec = func();};",
                "",
                "   defineShim(function() { return ["
        };

        String[] expectedFooterLines = {
                "   ];});",
                "})();"
        };

        for (int i = 0; i < expectedHeaderLines.length; i++) {
            assertEquals(expectedHeaderLines[i], actualLines[i]);
        }

        //check there are 2 lines which are just commas
        int commaCount = 0;
        for (String line : actualLines) {
            if (line.equals(",")) {
                commaCount++;
            }
        }
        assertEquals("There should be two commas between the rest specs", 2, commaCount);

        for (int i = 0; i < expectedFooterLines.length; i++) {
            String expected = expectedFooterLines[i];
            String actual = actualLines[actualLines.length - expectedFooterLines.length + i];
            assertEquals(expected, actual);
        }
    }
}

class FilesystemScenario {
    static List<FilesystemScenario> instances = new ArrayList<FilesystemScenario>();
    
    public final File root, srcDir, targetDir;

    public FilesystemScenario() {
        try{
            File tempFile = new File(System.getProperty("java.io.tmpdir"));
            root = File.createTempFile("fakemavenproject", ".dir", tempFile);
            if(!root.delete() && !root.mkdirs()){
                throw new RuntimeException("Unable to create directory");
            }
            srcDir = mkdirs(new File(root, "src/main/resources"));
            File fileInRoot = new File(srcDir, "fileInRoot.spec.json");
            File dirA = new File(srcDir, "dirA");
            dirA.mkdir();
            File fileInA = new File(dirA, "fileInA.spec.json");
            File dirB = new File(srcDir, "dirB");
            dirB.mkdir();
            File fileInB = new File(dirB, "fileInB.spec.json");
            File fileInBNotSpec = new File(dirB,"notaspec.json");

            writeToFile(fileInRoot,"root");
            writeToFile(fileInA,"apple");
            writeToFile(fileInB,"banana");
            writeToFile(fileInBNotSpec,"cat");

            targetDir = mkdirs(new File(root,"target"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        instances.add(this);
    }

    private static File mkdirs(File path){
        if(!path.exists() && !path.mkdirs()){
            throw new RuntimeException("Could not create directory " + path.getAbsolutePath());
        }
        return path;
    }
    
    private void writeToFile(File file, String string) {
        try {
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            PrintWriter out = new PrintWriter(file);
            out.println(string);
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public void delete(){
        try {
            FileUtils.deleteDirectory(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}