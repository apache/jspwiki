/**
 * 
 */
package com.ecyrd.jspwiki.ui.stripes;

public enum NodeType
{
    /** Root node */
    ROOT(null, null),
    /** Attribute node */
    ATTRIBUTE(null,null),
    /** Text node */
    TEXT("", ""),
    /** HTML start tag */
    HTML_START_TAG("<", ">"),
    /** HTML end tag */
    HTML_END_TAG("</", ">"),
    /** HTML end tag */
    HTML_COMBINED_TAG("<", "/>"),
    /** HTML tag, but not sure whether it's a start, end or combined tag. */
    UNRESOLVED_HTML_TAG("<", null),
    /** JSP comments, e.g., &lt;%-- comment --%&gt; */
    JSP_COMMENT("<%--", "--%>"),
    /**
     * JSP declaration, e.g., &lt;%! declaration; [ declaration; ]+ ...
     * %&gt;
     */
    JSP_DECLARATION("<%!", "%>"),
    /** JSP expression, e.g., &lt;%= expression %&gt; */
    JSP_EXPRESSION("<%=", "%>"),
    /**
     * JSP scriptlet, e.g., &lt;% code fragment %&gt;. Note the whitespace
     * after the %.
     */
    SCRIPTLET("<%", "%>"),
    /**
     * JSP page, import or taglib directive, e.g., &lt;%@ include... %&gt;
     * &lt;%@ page... %&gt; &lt;%@ taglib... %&gt;
     */
    JSP_DIRECTIVE("<%@", "%>"),
    /** JSP tag, but not sure what kind.. */
    UNRESOLVED_JSP_TAG(null, null),
    /** Parser has seen &lt;, but hasn't figured out what it is yet. */
    UNRESOLVED(null, null);
    private final String m_tagStart;

    private final String m_tagEnd;

    NodeType(String tagStart, String tagEnd )
    {
        m_tagStart = tagStart;
        m_tagEnd = tagEnd;
    }

    public String getTagEnd()
    {
        return m_tagEnd;
    }

    public String getTagStart()
    {
        return m_tagStart;
    }
    
    public String toString() {
        return name();
    }
    
}