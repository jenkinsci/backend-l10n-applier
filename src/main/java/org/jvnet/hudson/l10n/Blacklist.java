package org.jvnet.hudson.l10n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Blacklisting instance IDs for known unreliable sources.
 *
 * @author Kohsuke Kawaguchi
 */
public class Blacklist {
    private final Set<String> set = new HashSet<String>();

    public Blacklist() throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("blacklist.txt")));
        try {
            String l;
            while ((l=r.readLine())!=null) {
                l = l.trim();
                if (l.startsWith("#"))
                    continue;   // comment line
                set.add(l);
            }
        } finally {
            r.close();
        }
    }

    public boolean contains(String id) {
        return set.contains(id);
    }
}
