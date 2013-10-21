package org.jvnet.hudson.l10n;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Kohsuke Kawaguchi
 */
public class BlacklistTest extends Assert {
    @Test
    public void blacklist() throws Exception {
        Blacklist b = new Blacklist();
        assertTrue(b.contains("5f26ed6ff7a4a56eb9595c3e4cf17a8a"));
        assertFalse(b.contains("---"));
    }
}
