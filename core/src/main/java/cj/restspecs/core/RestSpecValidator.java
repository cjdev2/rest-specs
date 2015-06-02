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
package cj.restspecs.core;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import cj.restspecs.core.io.ClasspathLoader;
import cj.restspecs.core.io.Loader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestSpecValidator {
    public static class FileScanningResult {
        public final List<Path> allFiles;
        public final List<Path> specDotJsFiles;

        public FileScanningResult(List<Path> allFiles, List<Path> specDotJsFiles) {
            this.allFiles = allFiles;
            this.specDotJsFiles = specDotJsFiles;
        }
    }

    public static FileScanningResult scan(File resourcesDir) {
        List<Path> allFiles = flatListFiles(new Path(), resourcesDir, new ArrayList<Path>());
        List<Path> specDotJsFiles = minusNonSpecFiles(allFiles);
        return new FileScanningResult(allFiles, specDotJsFiles);
    }

    private final File resourcesDir;
    private final Loader loader;
    private final PrintStream console;

    public static void main(String[] args) {
        new RestSpecValidator(new File(args[0])).validate();
    }

    public RestSpecValidator(File resourcesDir) {
        this(resourcesDir, new ClasspathLoader(), System.out);
    }

    public RestSpecValidator(File resourcesDir, Loader loader, PrintStream console) {
        this.resourcesDir = resourcesDir;
        this.loader = loader;
        this.console = console;
    }

    public void validate() {
        validate(Collections.<String>emptyList());
    }

    public void validate(List<String> ignores) {
        console.println("Scanning files under " + resourcesDir.getAbsolutePath());
        FileScanningResult scan = scan(resourcesDir);
        console.println("Found " + scan.specDotJsFiles.size() + " specs");

        if (scan.specDotJsFiles.isEmpty()) {
            throw new RuntimeException("Something is wrong ... I was expecting to find .spec.json files under " + resourcesDir.getAbsolutePath() + " but found nothing.");
        }

        Set<String> fileNames = new TreeSet<String>();
        for (Path specPath : scan.specDotJsFiles) {
            final String baseMessage = "ERROR VALIDATING " + new File(resourcesDir, specPath.toString());
            try {
                RestSpec spec = new RestSpec("/" + specPath.toString(), loader);

                if (spec.name() == null) {
                    throw new RuntimeException(baseMessage + ": it is missing a \"name\"");
                }

                if (spec.path() == null) {
                    throw new RuntimeException(baseMessage + ": it is missing a \"url\"");
                }

                if (spec.response() == null) {
                    throw new RuntimeException(baseMessage + ": it is missing a \"response\"");
                }

                if (spec.response().representation() != null && spec.response().header().fieldsNamed("Content-Type").isEmpty()) {
                    throw new RuntimeException(baseMessage + ": it is missing a \"Content-Type\" header");
                }

                if (fileNames.contains(spec.name())) {
                    throw new RuntimeException("There is more than one spec named \"" + spec.name() + "\"");
                } else {
                    fileNames.add(spec.name());
                }
            } catch (Exception e) {
                e.printStackTrace(console);
                throw new RuntimeException(baseMessage + ": " + e.getMessage(), e);
            }
        }

        detectOrphansAndMissingReferences(resourcesDir, scan.allFiles, scan.specDotJsFiles, ignores);
    }

    private static List<Path> minusNonSpecFiles(List<Path> allFiles) {
        List<Path> specDotJsFiles = new ArrayList<Path>();

        for (Path next : allFiles) {
            if (next.toString().toLowerCase().endsWith(".spec.json")) {
                specDotJsFiles.add(next);
            }
        }

        return specDotJsFiles;
    }


    interface Fn<A, B> {
        B run(A input);
    }

    private static <A, B> List<B> collect(List<A> input, Fn<A, B> fn) {
        List<B> output = new ArrayList<B>(input.size());

        for (A next : input) {
            output.add(fn.run(next));
        }

        return output;
    }

    private void detectOrphansAndMissingReferences(File resourcesDir, List<Path> files, List<Path> specDotJsFiles, List<String> ignoreStrings) {
        List<Path> ignores = collect(ignoreStrings, new Fn<String, Path>() {
            public Path run(String input) {
                return new Path(input);
            }
        });
        List<Path> referencedFiles = new ArrayList<Path>();

        for (Path next : specDotJsFiles) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readValue(loader.load("/" + next), JsonNode.class);

                JsonNode requestRef = root.path("request").path("representation-ref");
                if (!requestRef.isMissingNode()) {
                    Path file = validateRepresentationReference(next, requestRef);
                    referencedFiles.add(file);
                }

                JsonNode responseRef = root.path("response").path("representation-ref");
                if (!responseRef.isMissingNode()) {
                    Path file = validateRepresentationReference(next, responseRef);
                    referencedFiles.add(file);
                }
            } catch (Throwable t) {
                throw new RuntimeException("There was an error parsing " + next + " :" + t.getMessage(), t);
            }
        }

        TreeSet<Path> filesToVet = new TreeSet<Path>(files);
        filesToVet.removeAll(specDotJsFiles);
        for (Path a : referencedFiles) {
            filesToVet.remove(a);
        }

        TreeSet<Path> filesActuallyIgnored = new TreeSet<Path>();
        for (Path next : ignores) {
            if (filesToVet.contains(next)) {
                filesActuallyIgnored.add(next);
            }
        }

        if (!filesActuallyIgnored.isEmpty()) {
            System.out.println("[WARNING] The following" + filesActuallyIgnored.size() + " files are not referenced by any spec, but I've been configured to ignore them anyway.  If these are intended to be examples of request/response representations, you can fix this by referencing them with a 'representation-ref' in a .spec.json.");
            for (Path next : filesActuallyIgnored) {
                System.out.println("[WARNING]     " + next);
            }
        }
        filesToVet.removeAll(filesActuallyIgnored);

        if (!filesToVet.isEmpty()) {
            StringBuilder text = new StringBuilder("VALIDATION ERROR: The following " + filesToVet.size() + " file(s) are not expected:");
            for (Path next : filesToVet) {
                text.append("\n    " + new File(resourcesDir, next.toString()).getAbsolutePath() + "");
            }

            throw new RuntimeException(text.toString());
        }
    }

    private Path validateRepresentationReference(Path next, JsonNode refNode) {
        File file = new File(resourcesDir.getAbsolutePath() + File.separatorChar + refNode.asText());

        if (!file.exists()) {
            throw new RuntimeException("Spec references nonexistent file: " + file.getAbsolutePath());
        }

        return new Path(refNode.asText());
    }

    private static List<Path> flatListFiles(Path base, File path, List<Path> files) {
        if (path.isDirectory()) {
            for (File child : path.listFiles()) {
                flatListFiles(base.childNamed(child.getName()), child, files);
            }
        } else if (path.isFile()) {
            files.add(base);
        } else {
            // just ignore it
        }

        return files;
    }

    public static class Path implements Comparable<Path> {
        final List<String> segments;

        public Path() {
            segments = Collections.emptyList();
        }

        public Path(String path) {
            this(Arrays.asList(path.split("/")));
        }

        public Path(List<String> segments) {
            super();
            this.segments = segments;
        }

        public Path parent() {
            return segments.isEmpty() ? null : new Path(segments.subList(0, segments.size() - 1));
        }

        public int compareTo(Path o) {
            return o.toString().compareTo(this.toString());
        }

        public Path childNamed(String childName) {
            List<String> path = new ArrayList<String>();
            path.addAll(segments);
            path.add(childName);
            return new Path(path);
        }

        @Override
        public String toString() {
            StringBuilder txt = new StringBuilder();
            for (String next : segments) {
                if (txt.length() > 0) {
                    txt.append("/");
                }
                txt.append(next);
            }

            return txt.toString();
        }
    }
}

