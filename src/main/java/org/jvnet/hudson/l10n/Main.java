package org.jvnet.hudson.l10n;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Main {
    @Argument
    public File sourceRoot;

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
