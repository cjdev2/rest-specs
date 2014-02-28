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

import static com.cj.restspecs.mojo.Util.relativePath;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class FingerprintMojoTest {

    @Test
    public void happyPath() throws Exception {
        // given
        final Scenario scenario = new Scenario();
        
        final File destinationFile = new File(scenario.targetDir, "foobar.fingerprint");
        
        final FingerprintMojo mojo = new FingerprintMojo();
        mojo.directories.add(relativePath(scenario.root, scenario.srcMainResourcesDir));
        mojo.basedir = scenario.root;
        mojo.destinationFile = destinationFile;
        
        
        final File someDir = mkdirs(new File(scenario.srcMainResourcesDir, "com/cj/restspecs/mojo"));

        copyInputStreamToFile(
                getClass().getResourceAsStream("SampleA.spec.json"), 
                new File(someDir, "SampleA.spec.json"));
        
        FileUtils.write(new File(someDir, "SampleA.response.txt"), "contents\n");
        
        // when
        mojo.execute();

        // then
        String fileContents = FileUtils.readFileToString(destinationFile);
        Assert.assertEquals("7a6e8344293a3ff8f8ff87092846e6f3", fileContents);
    }
    
    @Test
    public void allSpecFilesAreIncluded() throws Exception {
        // given
        final Scenario scenario = new Scenario();
        
        final File destinationFile = new File(scenario.targetDir, "foobar.fingerprint");
        
        final FingerprintMojo mojo = new FingerprintMojo();
        mojo.directories.add(relativePath(scenario.root, scenario.srcMainResourcesDir));
        mojo.basedir = scenario.root;
        mojo.destinationFile = destinationFile;
        
        
        final File someDir = mkdirs(new File(scenario.srcMainResourcesDir, "com/cj/restspecs/mojo"));

        copyInputStreamToFile(
                getClass().getResourceAsStream("SampleA.spec.json"), 
                new File(someDir, "SampleA.spec.json"));
        
        FileUtils.write(new File(someDir, "SampleA.response.txt"), "contents\n");
        mojo.execute();
        String fingerprintBefore= FileUtils.readFileToString(destinationFile);
        copyInputStreamToFile(
                getClass().getResourceAsStream("SampleB.spec.json"), 
                new File(someDir, "SampleB.spec.json"));
        
        // when
        mojo.execute();
        
        // then
        String fingerprintAfter= FileUtils.readFileToString(destinationFile);
        Assert.assertFalse("The md5 sums should have changed", fingerprintBefore.equals(fingerprintAfter));
    }
    
    @Test
    public void changesToFilesCauseTheFingerprintToChange() throws Exception {
        // given
        final Scenario scenario = new Scenario();
        
        final File destinationFile = new File(scenario.targetDir, "foobar.fingerprint");
        
        final FingerprintMojo mojo = new FingerprintMojo();
        mojo.directories.add(relativePath(scenario.root, scenario.srcMainResourcesDir));
        mojo.basedir = scenario.root;
        mojo.destinationFile = destinationFile;
        
        
        final File someDir = mkdirs(new File(scenario.srcMainResourcesDir, "com/cj/restspecs/mojo"));

        copyInputStreamToFile(
                getClass().getResourceAsStream("SampleA.spec.json"), 
                new File(someDir, "SampleA.spec.json"));
        
        FileUtils.write(new File(someDir, "SampleA.response.txt"), "contents\n");
        mojo.execute();
        String fingerprintBefore= FileUtils.readFileToString(destinationFile);
        
        FileUtils.write(new File(someDir, "SampleA.response.txt"), "newContents\n");
        
        // when
        mojo.execute();
        
        // then
        String fingerprintAfter= FileUtils.readFileToString(destinationFile);
        Assert.assertFalse("The md5 sums should have changed", fingerprintBefore.equals(fingerprintAfter));
    }
    
    
    static class Scenario {
        final File targetDir, root, srcMainResourcesDir;
        
        public Scenario() {
            try {
                File tempFile = new File(System.getProperty("java.io.tmpdir"));
                root = File.createTempFile("fakemavenproject", ".dir", tempFile);
                if(!root.delete() && !root.mkdirs()){
                    throw new RuntimeException("Unable to create directory");
                }
                targetDir = mkdirs(new File(root, "target"));
                srcMainResourcesDir = mkdirs(new File(root, "src/main/resources"));

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static File mkdirs(File path){
        if(!path.exists() && !path.mkdirs()){
            throw new RuntimeException("Could not create directory " + path.getAbsolutePath());
        }
        return path;
    }



}
