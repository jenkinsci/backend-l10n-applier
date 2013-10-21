package org.jvnet.hudson.l10n;

import java.io.IOException;

/**
 * Blacklisting instance IDs for known unreliable sources.
 *
 * @author Kohsuke Kawaguchi
 */
public class Blacklist extends TextList {
    public Blacklist() throws IOException {
        super(Blacklist.class.getResourceAsStream("blacklist.txt"));
    }
}
