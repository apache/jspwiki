/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.parser;

/**
 *  This class is used to store the headings in a manner which
 *  allow the building of a Table Of Contents.
 *  
 *  @since 2.4
 *  @author Janne Jalkanen
 */
public class Heading
{
    public static final int HEADING_SMALL  = 1;
    public static final int HEADING_MEDIUM = 2;
    public static final int HEADING_LARGE  = 3;

    public int    m_level;
    public String m_titleText;
    public String m_titleAnchor;
    public String m_titleSection;
}