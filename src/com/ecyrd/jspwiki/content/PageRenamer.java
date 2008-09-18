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
package com.ecyrd.jspwiki.content;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Provides page renaming functionality.  Note that there used to be
 *  a similarly named class in 2.6, but due to unclear copyright, the
 *  class was completely rewritten from scratch for 2.8.
 *
 *  @since 2.8
 */
public class PageRenamer
{

    private static final Logger log = Logger.getLogger( PageRenamer.class );
    
    private boolean m_camelCase = false;
    
    /**
     *  Renames a page.
     *  
     *  @param context The current context.
     *  @param renameFrom The name from which to rename.
     *  @param renameTo The new name.
     *  @param changeReferrers If true, also changes all the referrers.
     *  @return The final new name (in case it had to be modified)
     *  @throws WikiException If the page cannot be renamed.
     */
    public String renamePage( WikiContext context, 
                              String renameFrom, 
                              String renameTo, 
                              boolean changeReferrers )
        throws WikiException
    {
        //
        //  Sanity checks first
        //
        if( renameFrom == null || renameFrom.length() == 0 )
        {
            throw new WikiException( "From name may not be null or empty" );
        }
        if( renameTo == null || renameTo.length() == 0 )
        {
            throw new WikiException( "To name may not be null or empty" );
        }
       
        //
        //  Clean up the "to" -name so that it does not contain anything illegal
        //
        
        renameTo = MarkupParser.cleanLink( renameTo.trim() );
        
        if( renameTo.equals(renameFrom) )
        {
            throw new WikiException( "You cannot rename the page to itself" );
        }
        
        //
        //  Preconditions: "from" page must exist, and "to" page must not yet exist.
        //
        WikiEngine engine = context.getEngine();
        WikiPage fromPage = engine.getPage( renameFrom );
        
        if( fromPage == null )
        {
            throw new WikiException("No such page "+renameFrom);
        }
        
        WikiPage toPage = engine.getPage( renameTo );
        
        if( toPage != null )
        {
            throw new WikiException("Page already exists "+renameTo);
        }
        
        //
        //  Options
        //
        
        m_camelCase = TextUtil.getBooleanProperty( engine.getWikiProperties(), 
                                                   JSPWikiMarkupParser.PROP_CAMELCASELINKS, 
                                                   m_camelCase );

        Set<String> referrers = getReferencesToChange( fromPage, engine );

        //
        //  Do the actual rename by changing from the frompage to the topage, including
        //  all of the attachments
        //
        
        engine.getPageManager().getProvider().movePage( renameFrom, renameTo );
        
        if( engine.getAttachmentManager().attachmentsEnabled() )
        {
            engine.getAttachmentManager().getCurrentProvider().moveAttachmentsForPage( renameFrom, renameTo );
        }

        //
        //  Add a comment to the page notifying what changed.  This adds a new revision
        //  to the repo with no actual change.
        //
        
        toPage = engine.getPage( renameTo );
        
        if( toPage == null ) throw new InternalWikiException("Rename seems to have failed for some strange reason - please check logs!");

        toPage.setAttribute( WikiPage.CHANGENOTE, fromPage.getName() + " ==> " + toPage.getName() );
        toPage.setAuthor( context.getCurrentUser().getName() );
        
        engine.getPageManager().putPageText( toPage, engine.getPureText( toPage ) );

        //
        //  Update the references
        //
        
        engine.getReferenceManager().pageRemoved( fromPage );
        engine.updateReferences( toPage );

        //
        //  Update referrers
        //
        if( changeReferrers )
        {
            updateReferrers( context, fromPage, toPage, referrers );
        }


        //
        //  Done, return the new name.
        //
        return renameTo;
    }

    /**
     *  This method finds all the pages which have anything to do with the fromPage and
     *  change any referrers it can figure out in that page.
     *  
     *  @param context WikiContext in which we operate
     *  @param fromPage The old page
     *  @param toPage The new page
     */
    @SuppressWarnings("unchecked")
    private void updateReferrers( WikiContext context, WikiPage fromPage, WikiPage toPage, Set<String>referrers )
    {
        WikiEngine engine = context.getEngine();
        
        if( referrers.isEmpty() ) return; // No referrers
        
        for( String pageName : referrers )
        {
            //  In case the page was just changed from under us, let's do this
            //  small kludge.
            if( pageName.equals( fromPage.getName() ) )
            {
                pageName = toPage.getName();
            }
            
            WikiPage p = engine.getPage( pageName );
            
            String sourceText = engine.getPureText( p );
            
            String newText = replaceReferrerString( context, sourceText, fromPage.getName(), toPage.getName() );
            
            if( m_camelCase )
                newText = replaceCCReferrerString( context, newText, fromPage.getName(), toPage.getName() );
            
            if( !sourceText.equals( newText ) )
            {
                p.setAttribute( WikiPage.CHANGENOTE, fromPage.getName()+" ==> "+toPage.getName() );
                p.setAuthor( context.getCurrentUser().getName() );
         
                try
                {
                    engine.getPageManager().putPageText( p, newText );
                    engine.updateReferences( p );
                }
                catch( ProviderException e )
                {
                    //
                    //  We fail with an error, but we will try to continue to rename
                    //  other referrers as well.
                    //
                    log.error("Unable to perform rename.",e);
                }
            }
        }
    }

    private Set<String> getReferencesToChange( WikiPage fromPage, WikiEngine engine )
    {
        Set<String> referrers = new TreeSet<String>();
        
        Collection<String> r = engine.getReferenceManager().findReferrers( fromPage.getName() );
        if( r != null ) referrers.addAll( r );
        
        try
        {
            Collection<Attachment> attachments = engine.getAttachmentManager().listAttachments( fromPage );

            for( Attachment att : attachments  )
            {
                Collection<String> c = engine.getReferenceManager().findReferrers(att.getName());

                if( c != null ) referrers.addAll(c);
            }
        }
        catch( ProviderException e )
        {
            // We will continue despite this error
            log.error( "Provider error while fetching attachments for rename", e );
        }
        return referrers;
    }

    /**
     *  Replaces camelcase links.
     */
    private String replaceCCReferrerString( WikiContext context, String sourceText, String from, String to )
    {
        StringBuilder sb = new StringBuilder( sourceText.length()+32 );
        
        Pattern linkPattern = Pattern.compile( "\\p{Lu}+\\p{Ll}+\\p{Lu}+[\\p{L}\\p{Digit}]*" );
        
        Matcher matcher = linkPattern.matcher( sourceText );
        
        int start = 0;
        
        while( matcher.find(start) )
        {
            String match = matcher.group();

            sb.append( sourceText.substring( start, matcher.start() ) );

            int lastOpenBrace = sourceText.lastIndexOf( '[', matcher.start() );
            int lastCloseBrace = sourceText.lastIndexOf( ']', matcher.start() );
            
            if( match.equals( from ) && lastCloseBrace >= lastOpenBrace )
            {
                sb.append( to );
            }
            else
            {
                sb.append( match );
            }
            
            start = matcher.end();
        }
        
        sb.append( sourceText.substring( start ) );
        
        return sb.toString();
    }

    private String replaceReferrerString( WikiContext context, String sourceText, String from, String to )
    {
        StringBuilder sb = new StringBuilder( sourceText.length()+32 );
        
        //
        //  This monstrosity just looks for a JSPWiki link pattern.  But it is pretty
        //  cool for a regexp, isn't it?  If you can understand this in a single reading,
        //  you have way too much time in your hands.
        //
        Pattern linkPattern = Pattern.compile( "([\\[\\~]?)\\[([^\\|\\]]*)(\\|)?([^\\|\\]]*)(\\|)?([^\\|\\]]*)\\]" );
        
        Matcher matcher = linkPattern.matcher( sourceText );
        
        int start = 0;
        
        // System.out.println("====");
        // System.out.println("SRC="+sourceText.trim());
        while( matcher.find(start) )
        {
            char charBefore = (char)-1;
            
            if( matcher.start() > 0 ) 
                charBefore = sourceText.charAt( matcher.start()-1 );
            
            if( matcher.group(1).length() > 0 || charBefore == '~' || charBefore == '[' ) 
            {
                //
                //  Found an escape character, so I am escaping.
                //
                sb.append( sourceText.substring( start, matcher.end() ) );
                start = matcher.end();
                continue;
            }

            String text = matcher.group(2);
            String link = matcher.group(4);
            String attr = matcher.group(6);
             
            /*
            System.out.println("MATCH="+matcher.group(0));
            System.out.println("   text="+text);
            System.out.println("   link="+link);
            System.out.println("   attr="+attr);
            */
            if( link.length() == 0 )
            {
                text = replaceSingleLink( context, text, from, to );
            }
            else
            {
                link = replaceSingleLink( context, link, from, to );
                
                //
                //  A very simple substitution, but should work for quite a few cases.
                //
                text = TextUtil.replaceString( text, from, to );
            }
        
            //
            //  Construct the new string
            //
            sb.append( sourceText.substring( start, matcher.start() ) );
            sb.append( "["+text );
            if( link.length() > 0 ) sb.append( "|" + link );
            if( attr.length() > 0 ) sb.append( "|" + attr );
            sb.append( "]" );
            
            start = matcher.end();
        }
        
        sb.append( sourceText.substring( start ) );
        
        return sb.toString();
    }

    /**
     *  This method does a correct replacement of a single link, taking into
     *  account anchors and attachments.
     */
    private String replaceSingleLink( WikiContext context, String original, String from, String newlink )
    {
        int hash = original.indexOf( '#' );
        int slash = original.indexOf( '/' );
        String reallink = original;
        
        if( hash != -1 ) reallink = original.substring( 0, hash );
        if( slash != -1 ) reallink = original.substring( 0,slash );
        
        reallink = MarkupParser.cleanLink( reallink );
        
        // WikiPage p  = context.getEngine().getPage( reallink );
        // WikiPage p2 = context.getEngine().getPage( from );
        
        // System.out.println("   "+reallink+" :: "+ from);
        // System.out.println("   "+p+" :: "+p2);
        
        //
        //  if the original contains blanks, then we should introduce a link, for example:  [My Page]  =>  [My Page|My Renamed Page]
        int blank = reallink.indexOf( " ");
        
        if( blank != -1 )
        {
            return reallink + "|" + newlink;
        }
        
        //
        //  Yes, these point to the same page.
        //
        if( reallink.equals(from) )
        {
            return newlink + ((hash > 0) ? original.substring( hash ) : "") + ((slash > 0) ? original.substring( slash ) : "") ;
        }
        
        return original;
    }
}
