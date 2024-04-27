/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.markdown.migration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * Main entry point for the converter. It's basically a void main that calls a
 * unit test function that performs the conversion. Use -help to get syntax
 * info.
 *
 * @author AO
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("srcDir", true, "Source directory");
        options.addOption("destDir", true, "Destination directory");
        options.addOption("srcFormat", true, "Source format (jspwiki,markdown)");
        options.addOption("destFormat", true, "Destination format (jspwiki,markdown)");
        options.addOption("r", false, "Recursive (process history too)");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        int pts = 0;
        if (cmd.hasOption("srcDir")) {
            pts++;
        }
        if (cmd.hasOption("destDir")) {
            pts++;
        }
        if (cmd.hasOption("srcFormat")) {
            pts++;
        }
        if (cmd.hasOption("destFormat")) {
            pts++;
        }
        if (pts != 4) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("jspwiki-syntax-converter", options);
            return;
        }
        //confirm both direcotires exist
        File src = new File(cmd.getOptionValue("srcDir"));
        if (!src.exists()) {
            System.err.println("srcDir does not exist");
            return;
        } else {
            if (!src.isDirectory()) {
                System.err.println("srcDir was not a directory");
                return;
            }
        }
        File dest = new File(cmd.getOptionValue("destDir"));
        if (!dest.exists()) {
            if (!dest.mkdirs()) {
                System.err.println("destDir does not exist and we failed to mkdirs at that location. Try a different path");
                return;
            }
        } else {
            System.out.println("destDir exists. Any existing files may be overwritten");
        }
        long start = System.currentTimeMillis();
        WikiSyntaxConverter converter = new WikiSyntaxConverter();
        List<String> errors = convert(converter, src,
                cmd.getOptionValue("srcFormat"),
                dest, cmd.getOptionValue("destFormat"),
                cmd.hasOption("r")
        );
        System.out.println("processed in " + (System.currentTimeMillis() - start) + "ms with " + errors.size() + " errors");
        for (String s : errors) {
            System.out.println(s);
        }
        if (errors.isEmpty()) {
            return;
        } else {
            System.exit(1);
        }
    }

    private static List<String> convert(WikiSyntaxConverter converter, File src, String srcF, File dest, String destF, boolean recursive) throws Exception {
        System.out.println("processing " + src.getAbsolutePath());
        List<String> r = new ArrayList<>();
        r.addAll(converter.convert(src.getAbsolutePath(), srcF, dest.getAbsolutePath(), destF));
        if (!recursive) {
            return Collections.EMPTY_LIST;
        }
        File[] files = src.listFiles();
        if (files == null) {
            return Collections.EMPTY_LIST;
        }

        for (File f : files) {
            if (!f.isDirectory()) {
                continue;
            }
            File newDest = new File(destF, f.getName());
            if (!newDest.mkdirs()) {
                System.err.println("failed to mkdirs at " + newDest.getAbsolutePath());
            }
            r.addAll(convert(converter, f, srcF, newDest, destF, recursive));
        }
        return r;
    }

}
