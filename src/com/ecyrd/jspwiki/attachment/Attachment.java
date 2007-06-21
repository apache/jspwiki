/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.attachment;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  Describes an attachment.  Attachments are actually derivatives of
 *  a WikiPage, since they do actually have a WikiName as well.
 *
 *  @author Erik Bunn
 *  @author Janne Jalkanen
 */
public class Attachment
    extends WikiPage
{
    private String m_fileName;
    private String m_parentName;
    private boolean m_cacheable = true;

    /**
     *  Creates a new attachment.  The final name of the attachment will be 
     *  a synthesis of the parent page name and the file name.
     *  
     *  @param engine     The WikiEngine which is hosting this attachment.
     *  @param parentPage The page which will contain this attachment.
     *  @param fileName   The file name for the attachment.
     */
    public Attachment( WikiEngine engine, String parentPage, String fileName )
    {
        super( engine, parentPage+"/"+fileName );

        m_parentName = parentPage;
        m_fileName   = fileName;
    }

    /**
     *  Returns a human-readable, only-debugging-suitable description.
     *  
     *  @return A debugging string
     */
    public String toString()
    {
        return "Attachment ["+getName()+";mod="+getLastModified()+"]";
    }

    /**
     *  Returns the file name of the attachment.
     *  
     *  @return A String with the file name.
     */
    public String getFileName()
    {
        return m_fileName;
    }

    /**
     *  Sets the file name of this attachment. 
     *  
     *  @param name The name of the attachment.  Must be a legal file name without
     *              the path.
     */
    public void setFileName( String name )
    {
        m_fileName = name;
    }
/*
    public int getStatus()
    {
        return m_status;
    }

    public void setStatus( int status )
    {
        m_status = status;
    }
*/
    /**
     *  Returns the name of the parent of this Attachment, i.e. the page
     *  which contains this attachment.
     *  
     *  @return String depicting the parent of the attachment.
     */
    public String getParentName()
    {
        return m_parentName;
    }

    /**
     *  Returns true, if this attachment can be cached by the user agent.  By default
     *  attachments are cacheable.
     *  
     *  @return False, if the attachment should not be cached by the user agent.
     *  @since 2.5.34
     */
    public boolean isCacheable()
    {
        return m_cacheable;
    }

    /**
     *  Sets this attachment to be cacheable or not.  This mostly concerns things
     *  like DynamicAttachments, but it may be useful for certain AttachmentProviders
     *  as well.
     *  
     *  @param value True or false, depending on whether you want this attachment
     *               to be cacheable or not.
     *  @since 2.5.34
     */
    public void setCacheable(boolean value)
    {
        m_cacheable = value;
    }
}
