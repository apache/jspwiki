/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.attachment;

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
    private long   m_fileSize;

    public Attachment( String name )
    {
        super( name );

        // -1 for unknown size; anything >= 0 is valid.
        m_fileSize = -1;
    }

    public String toString()
    {
        return "Attachment ["+getName()+",mod="+getLastModified()+"]";
    }

    public String getFileName()
    {
        return( m_fileName );
    }

    public void setFileName( String name )
    {
        m_fileName = name;
    }

    public long getSize()
    {
        return( m_fileSize );
    }

    public void setSize( long size )
    {
        m_fileSize = size;
    }
}
