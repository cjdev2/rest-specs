package com.cj.restspecs.mojo;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.*;

public class CatalogUtilityTest {

    private static final Random RNG = new SecureRandom();

    private static final File SOURCES, CLASSES;

    private static final Set<File> SPECIFICATIONS;

    static {
        {
            // generate base paths for sources and classes (does not write to file system)
            SOURCES = new File(FileUtils.getTempDirectory(), generateRandomPath());
            CLASSES = new File(FileUtils.getTempDirectory(), generateRandomPath());
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


    @After
    public void cleanupClasses() throws IOException{
        FileUtils.deleteDirectory(CLASSES);
    }

    @Test
    public void testGenerateCatalogCreatesFile() throws IOException {

        // GIVEN
        String packageName = "com.foo.bar";

        // WHEN
        CatalogUtility.generateCatalog(SOURCES, CLASSES, packageName);

        // THEN
        final File expectedFile = new File(CLASSES, "com/foo/bar/restspecs.rs");

        assertTrue("file should have been created", expectedFile.exists());
    }
}