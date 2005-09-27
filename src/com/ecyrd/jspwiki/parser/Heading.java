package com.ecyrd.jspwiki.parser;

/**
 *  This class is used to store the headings in a manner which
 *  allow the building of a Table Of Contents.
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