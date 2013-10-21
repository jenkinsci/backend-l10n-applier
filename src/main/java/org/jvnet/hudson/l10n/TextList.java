package org.jvnet.hudson.l10n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class TextList {
    private final Set<String> set = new HashSet<String>();

    public TextList(Reader reader) throws IOException {
        BufferedReader r = new BufferedReader(reader);
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
