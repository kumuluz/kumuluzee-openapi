package com.kumuluz.ee.openapi.utils;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by zvoneg on 21/09/2017.
 */
public class AnnotationProcessorUtil {

    private static final Logger LOG = Logger.getLogger(AnnotationProcessorUtil.class.getName());

    public static void writeFile(String content, String resourceName, Filer filer) throws IOException {
        Set<String> contents = new HashSet<>();
        contents.add(content);
        writeFile(contents, resourceName, null, filer);
    }

    private static void writeFile(Set<String> content, String resourceName, FileObject overrideFile, Filer filer) throws IOException {
        FileObject file = overrideFile;
        if (file == null) {
            file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceName);
        }
        try (Writer writer = file.openWriter()) {
            for (String serviceClassName : content) {
                writer.write(serviceClassName);
                writer.write(System.lineSeparator());
            }
        }
    }

    private static void readOldFile(Set<String> content, Reader reader) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line = bufferedReader.readLine();
            while (line != null) {
                content.add(line);
                line = bufferedReader.readLine();
            }
        }
    }

    public static void writeFileSet(Set<String> content, String resourceName, Filer filer) throws IOException {
        FileObject file = readOldFile(content, resourceName, filer);
        if (file != null) {
            try {
                writeFileSet(content, resourceName, file, filer);
                return;
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        writeFileSet(content, resourceName, null, filer);
    }

    private static FileObject readOldFile(Set<String> content, String resourceName, Filer filer) throws IOException {
        Reader reader = null;
        try {
            final FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT, "", resourceName);
            reader = resource.openReader(true);
            AnnotationProcessorUtil.readOldFile(content, reader);
            return resource;
        } catch (FileNotFoundException e) {
            // close reader, return null
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    private static void writeFileSet(Set<String> content, String resourceName, FileObject overrideFile, Filer filer) throws IOException {
        FileObject file = overrideFile;
        if (file == null) {
            file = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourceName);
        }
        try (Writer writer = file.openWriter()) {
            for (String serviceClassName : content) {
                writer.write(serviceClassName);
                writer.write(System.lineSeparator());
            }
        }
    }

}
