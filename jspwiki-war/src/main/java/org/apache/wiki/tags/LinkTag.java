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
package org.apache.wiki.tags;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.TextUtil;

/**
 *  Provides a generic link tag for all kinds of linking
 *  purposes.
 *  <p>
 *  If parameter <i>jsp</i> is defined, constructs a URL pointing
 *  to the specified JSP page, under the baseURL known by the WikiEngine.
 *  Any ParamTag name-value pairs contained in the body are added to this
 *  URL to provide support for arbitrary JSP calls.
 *  <p>
 *  @since 2.3.50
 */
public class LinkTag extends WikiLinkTag implements ParamHandler, BodyTag {

	static final long serialVersionUID = 0L;
    private static final Logger log = Logger.getLogger( LinkTag.class );

    private String m_version = null;
    private String m_cssClass= null;
    private String m_style   = null;
    private String m_title   = null;
    private String m_target  = null;
    private String m_compareToVersion = null;
    private String m_rel       = null;
    private String m_jsp     = null;
    private String m_ref     = null;
    private String m_context = WikiContext.VIEW;
    private String m_accesskey = null;
    private String m_templatefile = null;

    private boolean m_absolute = false;
    private boolean m_overrideAbsolute = false;

    private Map<String, String> m_containedParams;

    private BodyContent m_bodyContent;

    public void initTag()
    {
        super.initTag();
        m_version = m_cssClass = m_style = m_title = m_target = m_compareToVersion = m_rel = m_jsp = m_ref = m_accesskey = m_templatefile = null;
        m_context = WikiContext.VIEW;
        m_containedParams = new HashMap<String, String>();
        m_absolute = false;
    }

    public void setTemplatefile( String key )
    {
        m_templatefile = key;
    }

    public void setAccessKey( String key )
    {
        m_accesskey = key;
    }

    public void setAbsolute( String arg )
    {
        m_overrideAbsolute = true;
        m_absolute = TextUtil.isPositive( arg );
    }

    public String getVersion()
    {
        return m_version;
    }

    public void setVersion( String arg )
    {
        m_version = arg;
    }

    public void setCssClass( String arg )
    {
        m_cssClass = arg;
    }

    public void setStyle( String style )
    {
        m_style = style;
    }

    public void setTitle( String title )
    {
        m_title = title;
    }

    public void setTarget( String target )
    {
        m_target = target;
    }

    public void setCompareToVersion( String ver )
    {
        m_compareToVersion = ver;
    }

    public void setRel( String rel )
    {
        m_rel = rel;
    }

    public void setRef( String ref )
    {
        m_ref = ref;
    }

    public void setJsp( String jsp )
    {
        m_jsp = jsp;
    }

    public void setContext( String context )
    {
        m_context = context;
    }

    /**
     * Support for ParamTag supplied parameters in body.
     */
    public void setContainedParameter( String name, String value )
    {
        if( name != null )
        {
            if( m_containedParams == null )
            {
                m_containedParams = new HashMap<String, String>();
            }
            m_containedParams.put( name, value );
        }
    }


    /**
     *  This method figures out what kind of an URL should be output.  It mirrors heavily
     *  on JSPWikiMarkupParser.handleHyperlinks();
     *
     * @return the URL
     * @throws ProviderException
     */
    private String figureOutURL()
        throws ProviderException
    {
        String url = null;
        WikiEngine engine = m_wikiContext.getEngine();

        if( m_pageName == null )
        {
            WikiPage page = m_wikiContext.getPage();

            if( page != null )
            {
                m_pageName = page.getName();
            }
        }

        if( m_templatefile != null )
        {
            String params = addParamsForRecipient( null, m_containedParams );
            String template = engine.getTemplateDir();
            url = engine.getURL( WikiContext.NONE, "templates/"+template+"/"+m_templatefile, params, false );
        }
        else if( m_jsp != null )
        {
            String params = addParamsForRecipient( null, m_containedParams );
            //url = m_wikiContext.getURL( WikiContext.NONE, m_jsp, params );
            url = engine.getURL( WikiContext.NONE, m_jsp, params, m_absolute );
        }
        else if( m_ref != null )
        {
            int interwikipoint;

            if( new LinkParsingOperations( m_wikiContext ).isExternalLink(m_ref) )
            {
                url = m_ref;
            }
            else if( (interwikipoint = m_ref.indexOf(":")) != -1 )
            {
                String extWiki = m_ref.substring( 0, interwikipoint );
                String wikiPage = m_ref.substring( interwikipoint+1 );

                url = engine.getInterWikiURL( extWiki );

                if( url != null )
                {
                    url = TextUtil.replaceString( url, "%s", wikiPage );
                }
            }
            else if( m_ref.startsWith("#") )
            {
                // Local link
            }
            else if( TextUtil.isNumber(m_ref) )
            {
                // Reference
            }
            else
            {
                int hashMark = -1;

                String parms = (m_version != null) ? "version="+getVersion() : null;

                //
                //  Internal wiki link, but is it an attachment link?
                //
                WikiPage p = engine.getPage( m_pageName );

                if( p instanceof Attachment )
                {
                    url = m_wikiContext.getURL( WikiContext.ATTACH, m_pageName );
                }
                else if( (hashMark = m_ref.indexOf('#')) != -1 )
                {
                    // It's an internal Wiki link, but to a named section

                    String namedSection = m_ref.substring( hashMark+1 );
                    String reallink     = m_ref.substring( 0, hashMark );

                    reallink = MarkupParser.cleanLink( reallink );

                    String matchedLink;
                    String sectref = "";
                    if( (matchedLink = engine.getFinalPageName( reallink )) != null )
                    {
                        sectref = "section-"+engine.encodeName(matchedLink)+"-"+namedSection;
                        sectref = "#"+sectref.replace('%', '_');
                    }
                    else
                    {
                        matchedLink = reallink;
                    }

                    url = makeBasicURL( m_context, matchedLink, parms, m_absolute ) + sectref;
                }
                else
                {
                    String reallink = MarkupParser.cleanLink( m_ref );

                    url = makeBasicURL( m_context, reallink, parms, m_absolute );
                }
            }
        }
        else if( m_pageName != null && m_pageName.length() > 0 )
        {
            WikiPage p = engine.getPage( m_pageName );

            String parms = (m_version != null) ? "version="+getVersion() : null;

            parms = addParamsForRecipient( parms, m_containedParams );

            if( p instanceof Attachment )
            {
                String ctx = m_context;
                // Switch context appropriately when attempting to view an
                // attachment, but don't override the context setting otherwise
                if( m_context == null || m_context.equals( WikiContext.VIEW ) )
                {
                    ctx = WikiContext.ATTACH;
                }
                url = engine.getURL( ctx, m_pageName, parms, m_absolute );
                //url = m_wikiContext.getURL( ctx, m_pageName, parms );
            }
            else
            {
                url = makeBasicURL( m_context, m_pageName, parms, m_absolute );
            }
        }
        else
        {
            String page = engine.getFrontPage();
            url = makeBasicURL( m_context, page, null, m_absolute );
        }

        return url;
    }

    private String addParamsForRecipient( String addTo, Map< String, String > params )
    {
        if( params == null || params.size() == 0 )
        {
            return addTo;
        }
        StringBuilder buf = new StringBuilder();
        Iterator< Map.Entry< String, String > > it = params.entrySet().iterator();
        while( it.hasNext() )
        {
            Map.Entry< String, String > e = it.next();
            String n = e.getKey();
            String v = e.getValue();
            buf.append( n );
            buf.append( "=" );
            buf.append( v );
            if( it.hasNext() )
            {
                buf.append( "&amp;" );
            }
        }
        if( addTo == null )
        {
            return buf.toString();
        }
        if( !addTo.endsWith( "&amp;" ) )
        {
            return addTo + "&amp;" + buf.toString();
        }
        return addTo + buf.toString();
    }

    private String makeBasicURL( String context, String page, String parms, boolean absolute )
    {
        String url;
        WikiEngine engine = m_wikiContext.getEngine();

        if( context.equals( WikiContext.DIFF ) )
        {
            int r1 = 0;
            int r2 = 0;

            if( DiffLinkTag.VER_LATEST.equals(getVersion()) )
            {
                WikiPage latest = engine.getPage( page, WikiProvider.LATEST_VERSION );

                r1 = latest.getVersion();
            }
            else if( DiffLinkTag.VER_PREVIOUS.equals(getVersion()) )
            {
                r1 = m_wikiContext.getPage().getVersion() - 1;
                r1 = (r1 < 1 ) ? 1 : r1;
            }
            else if( DiffLinkTag.VER_CURRENT.equals(getVersion()) )
            {
                r1 = m_wikiContext.getPage().getVersion();
            }
            else
            {
                r1 = Integer.parseInt( getVersion() );
            }

            if( DiffLinkTag.VER_LATEST.equals(m_compareToVersion) )
            {
                WikiPage latest = engine.getPage( page, WikiProvider.LATEST_VERSION );

                r2 = latest.getVersion();
            }
            else if( DiffLinkTag.VER_PREVIOUS.equals(m_compareToVersion) )
            {
                r2 = m_wikiContext.getPage().getVersion() - 1;
                r2 = (r2 < 1 ) ? 1 : r2;
            }
            else if( DiffLinkTag.VER_CURRENT.equals(m_compareToVersion) )
            {
                r2 = m_wikiContext.getPage().getVersion();
            }
            else
            {
                r2 = Integer.parseInt( m_compareToVersion );
            }

            parms = "r1="+r1+"&amp;r2="+r2;
        }

        //url = m_wikiContext.getURL( m_context, m_pageName, parms );
        url = engine.getURL( m_context, m_pageName, parms, m_absolute );

        return url;
    }

    public int doWikiStartTag() throws Exception
    {
        return EVAL_BODY_BUFFERED;
    }

    public int doEndTag()
    {
        try
        {
            if( !m_overrideAbsolute )
            {
                // TODO: see WikiContext.getURL(); this check needs to be specified somewhere.
                WikiEngine engine = m_wikiContext.getEngine();
                m_absolute = "absolute".equals( engine.getWikiProperties().getProperty( WikiEngine.PROP_REFSTYLE ) );
            }

            JspWriter out = pageContext.getOut();
            String url = figureOutURL();

            StringBuilder sb = new StringBuilder( 20 );

            sb.append( (m_cssClass != null)   ? "class=\""+m_cssClass+"\" " : "" );
            sb.append( (m_style != null)   ? "style=\""+m_style+"\" " : "" );
            sb.append( (m_target != null ) ? "target=\""+m_target+"\" " : "" );
            sb.append( (m_title != null )  ? "title=\""+m_title+"\" " : "" );
            sb.append( (m_rel != null )    ? "rel=\""+m_rel+"\" " : "" );
            sb.append( (m_accesskey != null) ? "accesskey=\""+m_accesskey+"\" " : "" );

            switch( m_format )
            {
              case URL:
                out.print( url );
                break;
              default:
              case ANCHOR:
                out.print("<a "+sb.toString()+" href=\""+url+"\">");
                break;
            }

            // Add any explicit body content. This is not the intended use
            // of LinkTag, but happens to be the way it has worked previously.
            if( m_bodyContent != null )
            {
                String linktext = m_bodyContent.getString().trim();
                out.write( linktext );
            }

            //  Finish off by closing opened anchor
            if( m_format == ANCHOR ) out.print("</a>");
        }
        catch( Exception e )
        {
            // Yes, we want to catch all exceptions here, including RuntimeExceptions
            log.error( "Tag failed", e );
        }

        return EVAL_PAGE;
    }

    public void setBodyContent( BodyContent bc )
    {
        m_bodyContent = bc;
    }

    public void doInitBody() throws JspException
    {
    }
}
