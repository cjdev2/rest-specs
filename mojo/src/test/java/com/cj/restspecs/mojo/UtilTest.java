package com.cj.restspecs.mojo;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UtilTest {

    private static final Random RNG = new SecureRandom();

    private static final File SOURCES;

    private static final Set<File> SPECIFICATIONS;

    static {
        {
            // generate a base path for sources (does not write to file system)
            SOURCES = new File(FileUtils.getTempDirectory(), generateRandomPath());
        }
        {
            // generate some specification paths (does not write to file system)
            final ArrayList<File> specs = new ArrayList<>();
            specs.add(new File(SOURCES, "com/package/one/service-twenty-one.spec.json"));
            specs.add(new File(SOURCES, "customers.spec.json"));
            specs.add(new File(SOURCES, "com/facebook/frienemies.spec.json"));
            SPECIFICATIONS = Collections.unmodifiableSet(new HashSet<>(specs));
        }
    }

    private static final String generateRandomPath() {
        return String.valueOf(Math.abs(RNG.nextLong()));
    }

    @Test
    public void testPackageToPath() {
        assertEquals("com/foo/bar", Util.packageToPath("com.foo.bar"));
    }

    @Test(expected = NullPointerException.class)
    public void testPackageToPathDivergesOnNull() {
        Util.packageToPath(null);
    }

    @BeforeClass
    public static void generateSources() throws IOException {

        if (SOURCES.exists())
            throw new RuntimeException("sources directory already exists");

        if (!SOURCES.mkdir())
            throw new RuntimeException("cannot create sources directory");


        for(File spec : SPECIFICATIONS) {
            spec.getParentFile().mkdirs();
            FileUtils.touch(spec);
        }

        /*
        add some non-spec files, too.
         */
        final File notSpec1 = new File(SOURCES, "com/package/this.here.file.json");
        final File notSpec2 = new File(SOURCES, "not-a-spec-at-all.txt");

    }

    @AfterClass
    public static void removeSources() throws IOException {
        FileUtils.deleteDirectory(SOURCES);
    }


    @Test
    public void testFindRestSpecFiles() throws IOException {

        // WHEN
        final Set<File> actual = Util.findRestSpecFiles(SOURCES).collect(Collectors.toSet());

        // THEN
        final Set<File> expected = SPECIFICATIONS;
        assertEquals(expected, actual);

    }

}