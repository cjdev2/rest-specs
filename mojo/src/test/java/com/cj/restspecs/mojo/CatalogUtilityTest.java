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

import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CatalogUtilityTest {

    private static final Random RNG = new SecureRandom();

    private static final Path SOURCE_ROOT, DESTINATION_ROOT;

    private static final Set<Path> SPECIFICATIONS;

    static {
        {
            // generate base paths for sources and classes (does not write to file system)
            SOURCE_ROOT = new File(FileUtils.getTempDirectory(), "source" + generateRandomPath()).toPath();
            DESTINATION_ROOT = new File(FileUtils.getTempDirectory(), "dest" + generateRandomPath()).toPath();
        }
        {
            // generate some specification paths (does not write to file system)
            final ArrayList<Path> specs = new ArrayList<>();
            specs.add(new File("com/package/one/service-twenty-one.spec.json").toPath());
            specs.add(new File("customers.spec.json").toPath());
            specs.add(new File("com/facebook/frienemies.spec.json").toPath());
            SPECIFICATIONS = Collections.unmodifiableSet(new HashSet<>(specs));
        }
    }

    private static final String generateRandomPath() {
        return String.valueOf(Math.abs(RNG.nextLong()));
    }


    @BeforeClass
    public static void writeSources() throws IOException {

        if (SOURCE_ROOT.toFile().exists())
            throw new RuntimeException("sources directory already exists");

        if (!SOURCE_ROOT.toFile().mkdir())
            throw new RuntimeException("cannot create sources directory");


        for(Path specPath : SPECIFICATIONS) {

            File specFile = SOURCE_ROOT.resolve(specPath).toFile();

            specFile.getParentFile().mkdirs();
            FileUtils.touch(specFile);
        }

        /*
        add some non-spec files, too.
         */
        final Path notSpec1 = SOURCE_ROOT.resolve( "com/package/this.here.file.json");
        final Path notSpec2 = SOURCE_ROOT.resolve( "not-a-spec-at-all.txt");

        for(Path p : new Path[] {notSpec1,notSpec2}) {
            File f = p.toFile();
            f.mkdirs();
            FileUtils.touch(f);
        }

    }


    @AfterClass
    public static void cleanupSources() throws IOException {
        FileUtils.deleteDirectory(SOURCE_ROOT.toFile());
    }


    @Before
    public void ensureDestinationRoot() {

        if (DESTINATION_ROOT.toFile().exists())
            throw new RuntimeException("destination directory already exists");

        if (!DESTINATION_ROOT.toFile().mkdir())
            throw new RuntimeException("cannot create destination directory");

    }

    @After
    public void cleanupClasses() throws IOException{
        FileUtils.deleteDirectory(DESTINATION_ROOT.toFile());
    }

    @Test
    public void testGenerateCatalogCreatesFile() throws IOException {

        // GIVEN
        String packageName = "com.foo.bar";

        // WHEN
        CatalogUtility.generateCatalog(SOURCE_ROOT, DESTINATION_ROOT, packageName);

        // THEN
        final Path expectedLocation = DESTINATION_ROOT.resolve("com/foo/bar/restspecs.rs");
        assertTrue("file should have been created", expectedLocation.toFile().exists());
    }

    @Test
    public void testGenerateCatalogContents() throws IOException {

        // GIVEN
        String packageName = "com.foo.bar";

        // WHEN
        CatalogUtility.generateCatalog(SOURCE_ROOT, DESTINATION_ROOT, packageName);

        // THEN
        final Path sourcePath = DESTINATION_ROOT.resolve( "com/foo/bar/restspecs.rs");
        final List<String> actualLines = FileUtils.readLines(sourcePath.toFile());


        final Set<String> actual = new HashSet<>(actualLines);

        final Set<String> expected =
                SPECIFICATIONS.stream()
                        .map(Path::toString)
                    .collect(Collectors.toSet());

        assertEquals(expected, actual);

    }


}