package com.cj.restspecs.mojo;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class UtilTest {

    private static final Random RNG = new SecureRandom();

    private static final Path SOURCE_ROOT;

    private static final Set<Path> SPECIFICATIONS;

    static {
        {
            // generate a base path for sources (does not write to file system)
            SOURCE_ROOT = new File(FileUtils.getTempDirectory(), generateRandomPath()).toPath();
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

//        if (SOURCE_ROOT.exists())
//            throw new RuntimeException("sources directory already exists");
//
//        if (!SOURCE_ROOT.mkdir())
//            throw new RuntimeException("cannot create sources directory");


        for(Path specPath : SPECIFICATIONS) {
            File specFile = SOURCE_ROOT.resolve(specPath).toFile();
            specFile.getParentFile().mkdirs();
            FileUtils.touch(specFile);
        }

        /*
        add some non-spec files, too.
         */
//        final File notSpec1 = new File(SOURCE_ROOT, "com/package/this.here.file.json");
//        final File notSpec2 = new File(SOURCE_ROOT, "not-a-spec-at-all.txt");

    }

    @AfterClass
    public static void removeSources() throws IOException {
        FileUtils.deleteDirectory(SOURCE_ROOT.toFile());
    }


    @Test
    public void testFindRestSpecPaths() throws IOException {

        // WHEN
        final Set<Path> actual = Util.findRestSpecPaths(SOURCE_ROOT).collect(Collectors.toSet());

        // THEN
        final Set<Path> expected = SPECIFICATIONS.stream().map(rel -> SOURCE_ROOT.resolve(rel))
                .collect(Collectors.toSet());

        assertEquals(expected, actual);

    }

}