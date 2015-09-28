package com.cj.restspecs.mojo;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CatalogUtility {

    private static final String CATALOG_FILENAME = "restspecs.rs";

    static  void generateCatalog(Path sourceRoot, Path destRoot,  String packageName) throws IOException{

        final Path catalogLocation = destRoot.resolve(Util.packageToPath(packageName) ).resolve(CATALOG_FILENAME);

        final List<String> specs  =
            Util.findRestSpecPaths(sourceRoot)
                    .map(sourceRoot::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        FileUtils.writeLines(catalogLocation.toFile(), specs);

    }

}
