package com.cj.restspecs.mojo;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

public class CatalogUtility {

    private static final String CATALOG_FILENAME = "restspecs.rs";

    static  void generateCatalog(File sourceRoot, File destRoot,  String destPackage) throws IOException{

        final File destination = new File(new File(destRoot, Util.packageToPath(destPackage)),CATALOG_FILENAME);

        FileUtils.touch(destination);


//        final List<File> restSpecs = getRestSpecs();
//
//        final List<String> catalogItems = restSpecs.stream().map(CatalogUtility::toResourceName).collect(Collectors.toList());
//
//        final File catalog = new File(new File(pathRoot, Util.packageToPath(catalogPackage)), DEFAULT_CATALOG_FILENAME);
//
//        try(final OutputStream outs = new FileOutputStream(catalog);
//            final PrintWriter outw = new PrintWriter(new OutputStreamWriter(outs, Charset.forName("UTF-8")))) {
//            catalogItems.forEach(outw::println);
//        }

    }


//    private static    String toResourceName(File specFile) {
//        throw new UnsupportedOperationException("NYI");
//    }

//    private static List<File> getRestSpecs() {
//        throw new UnsupportedOperationException("NYI");
//    }

}
