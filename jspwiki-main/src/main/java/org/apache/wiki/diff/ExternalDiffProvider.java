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

package org.apache.wiki.diff;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * This DiffProvider allows external command line tools to be used to generate the diff.
 */
public class ExternalDiffProvider implements DiffProvider {

    private static final Logger log = Logger.getLogger(ExternalDiffProvider.class);

    /**
     * Determines the command to be used for 'diff'. This program must be able
     * to output diffs in the unified format. For example 'diff -u %s1 %s2'.
     */
    public static final String PROP_DIFFCOMMAND    = "jspwiki.diffCommand";

    private String m_diffCommand = null;
    private Charset m_encoding;

    private static final char DIFF_ADDED_SYMBOL    = '+';
    private static final char DIFF_REMOVED_SYMBOL  = '-';

    private static final String CSS_DIFF_ADDED     = "<tr><td bgcolor=\"#99FF99\" class=\"diffadd\">";
    private static final String CSS_DIFF_REMOVED   = "<tr><td bgcolor=\"#FF9933\" class=\"diffrem\">";
    private static final String CSS_DIFF_UNCHANGED = "<tr><td class=\"diff\">";
    private static final String CSS_DIFF_CLOSE     = "</td></tr>";

    //FIXME This could/should be a property for this provider, there is not guarentee that
    //the external program generates a format suitible for the colorization code of the
    //TraditionalDiffProvider, currently set to true for legacy compatibility.
    //I don't think this 'feature' ever worked right, did it?...
    private boolean m_traditionalColorization = true;

    /**
     *  Creates a new ExternalDiffProvider.
     */
    public ExternalDiffProvider()
    {
    }

    /**
     * @see org.apache.wiki.WikiProvider#getProviderInfo()
     * {@inheritDoc}
     */
    public String getProviderInfo()
    {
        return "ExternalDiffProvider";
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.WikiProvider#initialize(org.apache.wiki.WikiEngine, java.util.Properties)
     */
    public void initialize( final WikiEngine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        m_diffCommand = properties.getProperty( PROP_DIFFCOMMAND );
        if( m_diffCommand == null || m_diffCommand.trim().equals( "" ) ) {
            throw new NoRequiredPropertyException( "ExternalDiffProvider missing required property", PROP_DIFFCOMMAND );
        }

        m_encoding = engine.getContentEncoding();
    }


    /**
     * Makes the diff by calling "diff" program.
     * {@inheritDoc}
     */
    public String makeDiffHtml( final WikiContext ctx, final String p1, final String p2 ) {
        File f1 = null;
        File f2 = null;
        String diff = null;

        try {
            f1 = FileUtil.newTmpFile(p1, m_encoding);
            f2 = FileUtil.newTmpFile(p2, m_encoding);

            String cmd = TextUtil.replaceString(m_diffCommand, "%s1", f1.getPath());
            cmd = TextUtil.replaceString(cmd, "%s2", f2.getPath());

            final String output = FileUtil.runSimpleCommand(cmd, f1.getParent());

            // FIXME: Should this rely on the system default encoding?
            final String rawWikiDiff = new String( output.getBytes( StandardCharsets.ISO_8859_1 ), m_encoding );
            final String htmlWikiDiff = TextUtil.replaceEntities( rawWikiDiff );

            if (m_traditionalColorization) { //FIXME, see comment near declaration...
                diff = colorizeDiff( htmlWikiDiff );
            } else {
                diff = htmlWikiDiff;
            }
        } catch( final IOException e ) {
            log.error("Failed to do file diff", e );
        } catch( final InterruptedException e ) {
            log.error("Interrupted", e );
        } finally {
            if( f1 != null ) {
                f1.delete();
            }
            if( f2 != null ) {
                f2.delete();
            }
        }

        return diff;
    }


    /**
     * Goes through output provided by a diff command and inserts HTML tags to
     * make the result more legible. Currently colors lines starting with a +
     * green, those starting with - reddish (hm, got to think of color blindness here...).
     */
    static String colorizeDiff( final String diffText ) throws IOException {
        if( diffText == null ) {
            return "Invalid diff - probably something wrong with server setup.";
        }

        String line;
        String start;
        String stop;

        final BufferedReader in = new BufferedReader( new StringReader( diffText ) );
        final StringBuilder out = new StringBuilder();

        out.append("<table class=\"diff\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
        while( (line = in.readLine()) != null ) {
            stop = CSS_DIFF_CLOSE;

            if( line.length() > 0 ) {
                switch( line.charAt( 0 ) ) {
                    case DIFF_ADDED_SYMBOL:
                        start = CSS_DIFF_ADDED;
                        break;
                    case DIFF_REMOVED_SYMBOL:
                        start = CSS_DIFF_REMOVED;
                        break;
                    default:
                        start = CSS_DIFF_UNCHANGED;
                }
            } else {
                start = CSS_DIFF_UNCHANGED;
            }

            out.append( start )
               .append( line.trim() )
               .append( stop )
               .append( "\n" );
        }

        out.append( "</table>\n" );
        return out.toString();
    }

}
