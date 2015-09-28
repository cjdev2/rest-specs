package com.cj.restspecs.mojo;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CatalogUtilityTest {

    private static final Random RNG = new SecureRandom();

    private static final File SOURCE_ROOT, DESTINATION_ROOT;

    private static final Set<File> SPECIFICATIONS;

    static {
        {
            // generate base paths for sources and classes (does not write to file system)
            SOURCE_ROOT = new File(FileUtils.getTempDirectory(), generateRandomPath());
            DESTINATION_ROOT = new File(FileUtils.getTempDirectory(), generateRandomPath());
        }
        {
            // generate some specification paths (does not write to file system)
            final ArrayList<File> specs = new ArrayList<>();
            specs.add(new File(SOURCE_ROOT, "com/package/one/service-twenty-one.spec.json"));
            specs.add(new File(SOURCE_ROOT, "customers.spec.json"));
            specs.add(new File(SOURCE_ROOT, "com/facebook/frienemies.spec.json"));
            SPECIFICATIONS = Collections.unmodifiableSet(new HashSet<>(specs));
        }
    }

    private static final String generateRandomPath() {
        return String.valueOf(Math.abs(RNG.nextLong()));
    }


    @BeforeClass
    public static void writeSources() throws IOException {

        if (SOURCE_ROOT.exists())
            throw new RuntimeException("sources directory already exists");

        if (!SOURCE_ROOT.mkdir())
            throw new RuntimeException("cannot create sources directory");


        for(File spec : SPECIFICATIONS) {
            spec.getParentFile().mkdirs();
            FileUtils.touch(spec);
        }

        /*
        add some non-spec files, too.
         */
        final File notSpec1 = new File(SOURCE_ROOT, "com/package/this.here.file.json");
        final File notSpec2 = new File(SOURCE_ROOT, "not-a-spec-at-all.txt");

    }


    @AfterClass
    public static void cleanupSources() throws IOException {
        FileUtils.deleteDirectory(SOURCE_ROOT);
    }


    @Before
    public void ensureDestinationRoot() {

        if (DESTINATION_ROOT.exists())
            throw new RuntimeException("destination directory already exists");

        if (!DESTINATION_ROOT.mkdir())
            throw new RuntimeException("cannot create destination directory");

    }

    @After
    public void cleanupClasses() throws IOException{
        FileUtils.deleteDirectory(DESTINATION_ROOT);
    }

    @Test
    public void testGenerateCatalogCreatesFile() throws IOException {

        // GIVEN
        String packageName = "com.foo.bar";

        // WHEN
        CatalogUtility.generateCatalog(SOURCE_ROOT, DESTINATION_ROOT, packageName);

        // THEN
        final File expectedFile = new File(DESTINATION_ROOT, "com/foo/bar/restspecs.rs");

        assertTrue("file should have been created", expectedFile.exists());
    }

    @Test
    public void testGenerateCatalogContents() throws IOException {

        // GIVEN
        String packageName = "com.foo.bar";

        // WHEN
        CatalogUtility.generateCatalog(SOURCE_ROOT, DESTINATION_ROOT, packageName);

        // THEN
        final File sourceFile = new File(DESTINATION_ROOT, "com/foo/bar/restspecs.rs");

        final List<String> lines = FileUtils.readLines(sourceFile);


        final Set<String> actual =
                lines.stream()
                        .map(File::new)
                        .map(File::getAbsolutePath)
                    .collect(Collectors.toSet());

        final Set<String> expected =
                SPECIFICATIONS.stream()
                        .map(File::getAbsolutePath)
                    .collect(Collectors.toSet());

        assertEquals(expected, actual);


    }


}