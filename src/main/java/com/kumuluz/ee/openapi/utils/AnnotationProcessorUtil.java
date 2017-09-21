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

}
