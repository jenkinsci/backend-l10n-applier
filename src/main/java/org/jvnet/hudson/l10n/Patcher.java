package org.jvnet.hudson.l10n;

import net.sf.json.JSONObject;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.*;

/**
 * Patches the message resources.
 *
 */
public class Patcher
{
    /**
     * Possible source roots to find the match from.
     */
    public Collection<File> sourceRoots;

    /**
     * Cache from baseName to {@link File}
     */
    private Map<String,File> baseNameCache = new HashMap<String, File>();

    private final Blacklist blacklist;

    private final TextList addOnlyLanguages;

    public Patcher(Collection<File> sourceRoots) throws IOException {
        this.sourceRoots = sourceRoots;
        blacklist = new Blacklist();
        addOnlyLanguages = new TextList(getClass().getResourceAsStream("add-only-languages.txt"));
    }

    public void patch(File json) throws IOException {
        System.out.println("Patching from "+json);
        JSONObject o = JSONObject.fromObject(FileUtils.readFileToString(json, "UTF-8"));

        String installation = o.getString("installation");
        if (blacklist.contains(installation)) {
            System.out.println("  "+installation+" blacklisted");
            return;
        }

        String locale = o.getString("locale");

        boolean noEdit = addOnlyLanguages.contains(locale);

        for (JSONObject e : (List<JSONObject>)(List)o.getJSONArray("entry")) {
            String baseName = e.getString("baseName");
            File match = findMatch(baseName);
            if (match==null)
                throw new IOException("Failed to find the matching Jelly script for "+baseName);

            String key = e.getString("key");
            String text = e.getString("text");
            String original = e.getString("original");

            if (!argumentsMatch(text,original)) {
                System.out.println("  Number of arguments not matching: "+text+" vs "+original);
                continue;
            }

            if (blacklistText(text)) {
                System.out.println("  Not applying text:"+text);
                continue;
            }

            File l10n = new File(FilenameUtils.removeExtension(match.getPath())+"_"+locale+".properties");
            if (l10n.exists()) {
                insert(key, text, l10n, noEdit);
            } else {
                if (key.equals(text))
                    continue;   // no need to list this entry explicitly.

                // create a brand new file
                FileOutputStream s = new FileOutputStream(l10n);
                PrintWriter w = new PrintWriter(new OutputStreamWriter(s,"iso-8859-1"));
                try {
                    IOUtils.copy(getClass().getResourceAsStream("header.txt"),s);
                    w.println();
                    writeEntry(key, text, w);
                } finally {
                    w.close();
                }
            }
            System.out.println("  "+l10n);
        }
    }

    /**
     * Number of arguments must match.
     */
    private boolean argumentsMatch(String text, String original) {
        try {
            if (original.length()>0)
                return countArgs(text)==countArgs(original);
            return true;    // can't really tell unless there's an existing text
        } catch (IllegalArgumentException e) {
            // this happens when the message fails to parse with MessageFormat
            // usually someone foget to escape '
            return false;
        }
    }

    private int countArgs(String text) {
        return new MessageFormat(text).getFormatsByArgumentIndex().length;
    }

    private boolean blacklistText(String text) {
        text = text.toLowerCase(Locale.ENGLISH);
        if (text.contains("<script") || text.contains("<iframe") || text.contains("<style") || text.contains("<img"))   // induces formatting changes. can't be right.
            return true;

        if (text.contains("javascript:"))
            return true;

        return false;
    }

    /**
     * We try to stitch the text into the existing text.
     * If there's no existing text, we insert it to the nearest match.
     *
     * @param noEdit
     *      If this is true, the text cannot replace an existing translation. Only new addtion
     *      where there is no current text is allowed.
     */
    private void insert(String key, String text, File l10n, boolean noEdit) throws IOException {
        File tmp = new File(l10n.getPath()+".tmp");

        // figure out where we insert the new text
        Properties props = load(l10n);
        TreeSet<String> existingKeys = new TreeSet<String>((Set)props.keySet());
        String insertionPosition = existingKeys.ceiling(key);

        if (key.equals(insertionPosition) && noEdit)
            return; // existing text found. not taking the new edit.

        // property files are ISO-8859-1
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(l10n),"iso-8859-1"));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(tmp),"iso-8859-1"));
        try {
            String line;
            while ((line=in.readLine())!=null) {
                // look for the insertion key and insert it there
                int eq = line.indexOf('=');
                if (eq>0) {
                    String k = line.substring(0, eq).trim();
                    if (k.equals(escapeKey(insertionPosition)))
                        writeEntry(key, text, out);

                    if (k.equals(escapeKey(key))) {
                        // remove existing value
                        while (line!=null && line.endsWith("\\")) {
                            line = in.readLine();
                        }
                        continue;
                    }
                }
                out.println(line);
            }

            if (insertionPosition==null) {// insert it at the end
                writeEntry(key, text, out);
            }
        } finally {
            in.close();
            out.close();
        }

        // override the properties file.
        tmp.renameTo(l10n);
    }

    private Properties load(File l10n) throws IOException {
        FileInputStream in = new FileInputStream(l10n);
        try {
            Properties props = new Properties();
            props.load(in);
            return props;
        } finally {
            in.close();
        }
    }

    /**
     * Write a property file entry.
     */
    private void writeEntry(String key, String text, PrintWriter out) {
        if (key.equals(text))
            return; // there's no need to list such an entry explicitly.
        out.println(escapeKey(key)+"="+escape(text));
    }

    /**
     * Escapes the property key.
     */
    private String escapeKey(String s) {
        if (s==null)    return null;
        StringBuilder buf = new StringBuilder(s.length());
        for (int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
            case '\'':
                buf.append("''");
                break;
            case ' ':
                buf.append("\\ ");
                break;
            default:
                buf.append(ch);
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Escapes the property value in the properties file format.
     */
    private String escape(String s) {
        StringBuilder buf = new StringBuilder(s.length());
        for (int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
            case '\'':
                buf.append("''");
                break;
            default:
                if(ch>=0x80) // non-ASCII
                    buf.append(String.format("\\u%04X",(int)ch));
                else
                    buf.append(ch);
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Given the URL of the resource, like "file:/.../glassfish/domains/domain1/generated/jsp/j2ee-modules/hudson/loader/lib/hudson/buildHealth",
     * find the local Jelly file that corresponds to it.
     */
    private File findMatch(String baseName) {
        // check the cache
        File f = baseNameCache.get(baseName);
        if(f==null)
            baseNameCache.put(baseName,f = _findMatch(baseName));
        return f;
    }


    private File _findMatch(String baseName) {
        if (baseName.startsWith("file:")) {
            for (String suffix : asList("WEB-INF/classes","WEB-INF/classes.jar!")) {
                int idx = baseName.lastIndexOf(suffix);
                if(idx>=0)
                    return locateInSourceTree(baseName.substring(idx+suffix.length()));
            }

            // glassfish uses URLs like file:/.../glassfish/domains/domain1/generated/jsp/j2ee-modules/hudson/loader/lib/hudson/buildHealth
            // or                       file:/.../glassfish/domains/domain1/generated/jsp/hudson/loader/hudson/model/View/builds
            //                          file:/.../glassfish/domains/domain1/generated/jsp/jenkins/loader_1182831483/lib/layout/layout
            Matcher m = Pattern.compile("generated/jsp/(.+)/loader(_[0-9]+)?/").matcher(baseName);
            if (m.find())
                return locateInSourceTree(baseName.substring(m.end()));
        }
        if (baseName.startsWith("jar:")) {
            int idx = baseName.lastIndexOf("!");
            return locateInSourceTree(baseName.substring(idx+1));
        }

        // JBoss produces URLs like this
        // vfszip:/home/kohsuke/Jboss/jbossAS5/jboss-5.1.0.GA/server/default/deploy/hudson.war/WEB-INF/lib/hudson-core-1.339.jar/lib/layout/layout
        if (baseName.startsWith("vfszip:") || baseName.startsWith("vfs:")) {
            int idx = baseName.indexOf(".jar/");
            if (idx>0)
                return locateInSourceTree(baseName.substring(idx+4));
        }

        // more desperate checks for things like jar:file:/var/lib/jenkins/plugins/maven-plugin/WEB-INF/lib/maven-plugin.jar!/hudson/maven/MavenModuleSet/global
        for (String suffix : asList(".jar!")) {
            int idx = baseName.lastIndexOf(suffix);
            if(idx>=0)
                return locateInSourceTree(baseName.substring(idx+suffix.length()));
        }

        return null;
    }

    /**
     * Given the path within the resource root, like "/foo/bar/zot", figure out the {@link File}
     * location of foo/bar/zot.jelly
     */
    private File locateInSourceTree(String path) {
        if (path.startsWith("/"))   path=path.substring(1);

        for (File sourceRoot : sourceRoots) {
            File f = new File(sourceRoot,path+".jelly");
            if (f.exists())
                return f;
        }
        return null;
    }

    /**
     * Given the Hudson's source tree, list up all the possible resource roots.
     */
    public static Patcher forHudsonSourceTree(final File baseDir) throws IOException {
        final List<File> roots = new ArrayList<File>();
        DirectoryWalker w = new DirectoryWalker() {
            {
                walk(baseDir,null);
            }
            @Override
            protected boolean handleDirectory(File directory, int depth, Collection results) throws IOException {
                if(directory.getPath().endsWith("src/main/resources")) {
                    roots.add(directory);
                    return false;
                }
                if(directory.getName().equals("java") || directory.getName().equals("target"))
                    return false;
                return true;
            }
        };
        return new Patcher(roots);
    }
}
