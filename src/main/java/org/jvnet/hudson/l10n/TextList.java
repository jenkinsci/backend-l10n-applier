package org.jvnet.hudson.l10n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public class TextList {
    private final Set<String> set = new HashSet<String>();

    public TextList(InputStream is) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
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
