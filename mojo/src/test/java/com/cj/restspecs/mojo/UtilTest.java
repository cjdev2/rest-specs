package com.cj.restspecs.mojo;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest {

    @Test
    public void testPackageToPath() {
        assertEquals("com/foo/bar", Util.packageToPath("com.foo.bar"));
    }

    @Test(expected = NullPointerException.class) public void testPackageToPathDivergesOnNull() {
        Util.packageToPath(null);
    }

}