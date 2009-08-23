/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.plugin;

import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;

/**
 * Provides a list or table of InterWiki links.
 * <p>
 * Parameters:
 * </p>
 * <ul>
 * <li><b>type</b> - The type of output: acceptable values are 'text', 'ulist'
 * (unordered list), 'olist' or 'table'. Default = 'text'.</li>
 * <li><b>tabletitle</b> - The title of the table, when specified.</li>
 * <li><b>separator</b> - The separator string to be used between two entries in
 * the output of type 'text'. Default = ", ".</li>
 * </ul>
 * <p>
 * Note that the output of this plugin is placed in an HTML DIV with a class
 * 'interwikilinks'; you can use that in your CSS to give the output a distinct
 * look.
 * </p>
 * 
 */
public class InterWikiLinksPlugin implements WikiPlugin
{
    private static enum OutputTypes
    {
        TEXT_OUTPUT, U_LIST_OUTPUT, O_LIST_OUTPUT, TABLE_OUTPUT
    };

    private static Logger log = Logger.getLogger( InterWikiLinksPlugin.class );

    /** Parameter name for setting the type. */
    public static final String PARAM_TYPE = "type";

    /** Parameter name for setting the title. */
    public static final String PARAM_TABLE_TITLE = "tabletitle";

    /** Parameter name for setting the separator. */
    public static final String PARAM_SEPARATOR = "separator";

    private OutputTypes m_defaultOutputType = OutputTypes.TEXT_OUTPUT;

    private String m_defaultTableTitle = "Interwiki Links Table";

    private String m_defaultSeparator = ", ";

    private String m_crlf = System.getProperty( "line.separator" );

    private Collection<String> m_links = null;

    /**
     * {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String, Object> params ) throws PluginException
    {
        ResourceBundle rb = context.getBundle( WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE );
        try
        {
            StringBuffer sb = new StringBuffer();
            OutputTypes outputType = m_defaultOutputType;

            m_links = context.getEngine().getAllInterWikiLinks();
            
            // Get parameters, if any
            String typeParam = (String) params.get( PARAM_TYPE );
            String tableTitleParam = (String) params.get( PARAM_TABLE_TITLE );
            String separatorParam = (String) params.get( PARAM_SEPARATOR );

            // Handle default cases
            if( typeParam == null )
            {
                outputType = OutputTypes.TEXT_OUTPUT;
            }
            else if( "TEXT".equals( typeParam.toUpperCase() ) )
            {
                outputType = OutputTypes.TEXT_OUTPUT;
            }
            else if( "ULIST".equals( typeParam.toUpperCase() ) )
            {
                outputType = OutputTypes.U_LIST_OUTPUT;
            }
            else if( "OLIST".equals( typeParam.toUpperCase() ) )
            {
                outputType = OutputTypes.O_LIST_OUTPUT;
            }
            else if( "TABLE".equals( typeParam.toUpperCase() ) )
            {
                outputType = OutputTypes.TABLE_OUTPUT;
            }

            if( (tableTitleParam == null) || "".equals( tableTitleParam ) )
            {
                tableTitleParam = m_defaultTableTitle;
            }

            if( (separatorParam == null) || "".equals( separatorParam ) )
            {
                separatorParam = m_defaultSeparator;
            }

            // Create output buffer
            log.debug( "TITLE: " + tableTitleParam + ", SEPARATOR: " + separatorParam + ", TYPE: " + outputType.name() );

            sb.append( "<div class=\"interwikilinks\">" + m_crlf );

            if( outputType.equals( OutputTypes.TEXT_OUTPUT ) )
            {
                sb.append( m_crlf + "<span id=\"interwikilisttext\">" );

                int linkCount = 0;
                for( String link : m_links )
                {
                    sb.append( link );
                    sb.append( " == " );
                    sb.append( context.getEngine().getInterWikiURL( link ) );

                    // we don't want a separator after the last link :
                    if( linkCount++ != m_links.size() )
                    {
                        sb.append( separatorParam );
                    }
                }
                sb.append( "</span>" + m_crlf );
            }
            else if( outputType.equals( OutputTypes.U_LIST_OUTPUT ) )
            {
                sb.append( generateListOutput( context, "<ul>", "</ul>" ) );
            }
            else if( outputType.equals( OutputTypes.O_LIST_OUTPUT ) )
            {
                sb.append( generateListOutput( context, "<ol>", "</ol>" ) );
            }
            else if( outputType.equals( OutputTypes.TABLE_OUTPUT ) )
            {
                sb.append( "<table><caption>" + tableTitleParam + "</caption>" + m_crlf );
                sb.append( "<tbody>" + m_crlf );

                for( String link : m_links )
                {
                    sb.append( "<tr><td>" );
                    sb.append( link );
                    sb.append( "</td><td>" );
                    sb.append( context.getEngine().getInterWikiURL( link ) );
                    sb.append( "</td></tr>" + m_crlf );
                }
                sb.append( "</tbody>" + m_crlf );
                sb.append( "</table>" + m_crlf );
            }

            sb.append( m_crlf + "</div>" + m_crlf );

            return sb.toString();
        }
        catch( Exception e )
        {
            log.error( "Could not construct InterwikiLinks plugin output, reason: ", e );
            throw new PluginException( rb.getString( "plugin.interwikilinks.noconstruct" ) );
        }
    }

    private StringBuffer generateListOutput( WikiContext context, String startTag, String endTag )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( startTag + m_crlf );
        for( String link : m_links )
        {
            sb.append( "    <li>" );
            sb.append( link );
            sb.append( " == " );
            sb.append( context.getEngine().getInterWikiURL( link ) );
            sb.append( "    </li>" + m_crlf );
        }
        return sb.append( endTag + m_crlf );
    }
}
