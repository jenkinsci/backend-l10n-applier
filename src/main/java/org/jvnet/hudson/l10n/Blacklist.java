package org.jvnet.hudson.l10n;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Blacklisting instance IDs for known unreliable sources.
 *
 * @author Kohsuke Kawaguchi
 */
public class Blacklist extends TextList {
    public Blacklist() throws IOException {
        super(new InputStreamReader(Blacklist.class.getResourceAsStream("blacklist.txt")));
    }
}
