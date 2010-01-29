package org.jvnet.hudson.l10n;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies contributed translations (in the form of JSON files) as patches to the source tree.
 *
 * @author Kohsuke Kawaguchi
 */
public class Main {
    /**
     * Should point to the checked out copy of <tt>https://svn.dev.java.net/svn/hudson/trunk/hudson/</tt>
     */
    @Argument
    public File sourceRoot;

    /**
     * JSON translation files to apply as patches
     */
    @Argument(index=1)
    public List<File> jsonFiles = new ArrayList<File>();

    public static void main(String[] args) throws IOException, CmdLineException {
        Main m = new Main();
        CmdLineParser p = new CmdLineParser(m);
        p.parseArgument(args);

        System.err.println("Listing up source roots");
        Patcher patcher = Patcher.forHudsonSourceTree(m.sourceRoot);
        for (File j : m.jsonFiles) {
            try {
                patcher.patch(j);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
