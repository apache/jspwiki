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
package com.ecyrd.jspwiki.dav.methods;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import com.ecyrd.jspwiki.dav.DavContext;
import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;
import com.ecyrd.jspwiki.dav.WebdavServlet;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *
 *  @since 
 */
public class PropFindMethod
    extends DavMethod
{
    private static Logger log = Logger.getLogger( PropFindMethod.class );
 
    /**
     * 
     */
    public PropFindMethod( DavProvider provider )
    {
        super( provider );
    }

    public void sendMultiResponse( HttpServletResponse res, Element response )
        throws IOException
    {
        res.setContentType("application/xml; charset=\"UTF-8\"");
        res.setStatus( WebdavServlet.SC_MULTISTATUS );
        
        Document doc = new Document();

        doc.setRootElement( response );
        
        XMLOutputter output = new XMLOutputter();
     
        // System.out.println("Returning");
        output.setFormat( Format.getPrettyFormat() );
        // output.output( doc, System.out );
        output.output( doc, res.getWriter() );
    }
    
    private Element getPropertyNames( DavContext dc )
    {
        log.debug("Retrieving all property names for context "+dc);
        
        Namespace davns = Namespace.getNamespace( "DAV:" );
        Element root = new Element("multistatus", davns);
    
        DavItem di = m_provider.getItem( dc.getPath() );

        for( Iterator i = di.iterator(dc.getDepth()); i.hasNext(); )
        {
            di = (DavItem) i.next();
                    
            Element response = new Element("response", davns);

            response.addContent( new Element("href",davns).setText( di.getHref() ) );
                    
            Element propstat = new Element("propstat", davns);
                    
            //
            //  Wiki specifics.
            //
            Collection c = di.getPropertySet();
            
            Element prop = new Element("prop",davns);
            
            for( Iterator j = c.iterator(); j.hasNext(); )
            {
                Element el = (Element)j.next();
                el.removeContent();
                prop.addContent( el );
            }
            
            propstat.addContent( prop );
            propstat.addContent( new Element("status",davns).setText("HTTP/1.1 200 OK"));
                    
            response.addContent( propstat );
                                        
            root.addContent( response );
        }

        return root;
    }
    
    private Element getProperties( DavContext dc, List askedprops )
        throws IOException
    {
        Namespace davns = Namespace.getNamespace( "DAV:" );
        
        Element root = new Element("multistatus", davns);

        DavItem di = m_provider.getItem( dc.getPath() );

        if( di == null )
        {
            throw new FileNotFoundException( dc.getPath().toString() );
        }
        
        for( Iterator i = di.iterator(dc.getDepth()); i.hasNext(); )
        {
            di = (DavItem) i.next();
                    
            Element response = new Element("response", davns);

            response.addContent( new Element("href",davns).setText( di.getHref() ) );
                    
            Element props = new Element("prop", davns);
            Element failedprops = new Element("prop", davns );
            
            //
            //  Get the matching property set
            //
            Collection c = di.getPropertySet();
            
            if( askedprops == null )
            {
                for( Iterator j = c.iterator(); j.hasNext(); )
                {
                    Element el = (Element)j.next();
                
                    props.addContent( el );
                }
            }
            else
            {
                for( Iterator x = askedprops.iterator(); x.hasNext(); )
                {
                    Element askedElement = (Element)x.next();
                
                    boolean found = false;
                    
                    for( Iterator j = c.iterator(); j.hasNext(); )
                    {
                        Element el = (Element)j.next();
                    
                        if( askedElement.getNamespaceURI().equals( el.getNamespaceURI() ) &&
                            askedElement.getName().equals( el.getName() ) )
                        {
                            //
                            // This protects in case there are multiple elements
                            // in the request.
                            //
                            if( props.indexOf(el) == -1 )
                            {
                                props.addContent( el );
                            }
                            found = true;
                            break;
                        }
                    }
               
                    if( !found ) 
                    {
                        Element el = (Element)askedElement.clone();
                        failedprops.addContent( el );
                    }
                }
                
            }
            
            if( props.getContentSize() > 0 )
            {
                Element ps = new Element("propstat",davns);
                ps.addContent( props );
                ps.addContent( new Element("status",davns).setText("HTTP/1.1 200 OK"));
                response.addContent( ps );
            }
            
            if( failedprops.getContentSize() > 0 )
            {
                Element ps = new Element("propstat",davns);
                ps.addContent( failedprops );
                ps.addContent( new Element("status",davns).setText("HTTP/1.1 404 Not found"));    
                response.addContent( ps );
            }
            
            root.addContent( response );
        }        
        
        return root;
    }
    
    private Element getAllProps( DavContext dc )
        throws ProviderException, IOException
    {
        log.debug("Retrieving all properties for context "+dc.getPath());
        
        return getProperties( dc, null );
    }
    

    public void execute( HttpServletRequest req, HttpServletResponse res, DavPath dp )
        throws ServletException, IOException
    {  
        DavContext dc = new DavContext( req, dp );

        try
        {
            Document doc = new SAXBuilder().build( req.getInputStream() );
                    
            XPath xpath = XPath.newInstance("/D:propfind/*");
            xpath.addNamespace( "D", "DAV:" );
        
            Element firstnode = (Element)xpath.selectSingleNode( doc );

            Element davresponse = null;

            System.out.println("Request="+dc.getPath()+" depth="+dc.getDepth());

            if( firstnode == null || firstnode.getName().equals("allprop") )
            {
                davresponse = getAllProps( dc );
            }
            else if( firstnode.getName().equals("propname") )
            {
                davresponse = getPropertyNames( dc );
            }
            else if( firstnode.getName().equals("prop") )
            {
                XPath ndxp = XPath.newInstance("/D:propfind/D:prop/*");
                ndxp.addNamespace( "D", "DAV:" );
                
                List nodes = ndxp.selectNodes( doc );
 
                davresponse = getProperties( dc, nodes );
            }
            
            sendMultiResponse( res, davresponse );
        }
        catch( JDOMException e )
        {
            // This is probably someone trying to poke at us
            log.info( "Broken XML received", e );
            
            res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Parse error" );
        }
        catch( ProviderException e )
        {
            log.error( "Provider failed", e );
            
            res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage() );
        }
        catch( FileNotFoundException e )
        {
            res.sendError( HttpServletResponse.SC_NOT_FOUND, e.getMessage() );
        }
    }
    
}
