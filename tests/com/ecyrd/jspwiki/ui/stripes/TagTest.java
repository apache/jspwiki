package com.ecyrd.jspwiki.ui.stripes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TagTest extends TestCase
{
    public void testAddChildEmptyElement()
    {
        JspDocument doc = new JspDocument();
        
        // Set up tag <foo/>
        Tag parentTag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        parentTag.setName( "foo" );
        doc.getRoot().addChild( parentTag );
        assertEquals( "<foo/>", parentTag.toString() );
        assertEquals( 1, doc.getRoot().getChildren().size() );
        
        // Add child tag <bar/>
        Tag childTag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        childTag.setName( "bar" );
        parentTag.addChild( childTag );
        
        // Should result in <foo><bar/></foo>
        assertEquals( "<foo><bar/></foo>", doc.toString() );
        assertEquals( 2, doc.getRoot().getChildren().size() );
    }

    public void testAddChild()
    {
        JspDocument doc = new JspDocument();
        
        // Set up tag <foo>
        Tag parentTag = new Tag( doc, NodeType.START_TAG);
        parentTag.setName( "foo" );
        doc.getRoot().addChild( parentTag );
        assertEquals( "<foo>", parentTag.toString() );
        assertEquals( 1, doc.getRoot().getChildren().size() );
        
        // Add child tag <bar/>
        Tag childTag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        childTag.setName( "bar" );
        parentTag.addChild( childTag );
        
        // Should result in <foo><bar/> (with bar a child of foo)
        assertEquals( "<foo><bar/>", doc.toString() );
        assertEquals( 1, doc.getRoot().getChildren().size() );
    }

    public void testAddChildAtIndex()
    {
        JspDocument doc = new JspDocument();
        
        // Set up tag <foo>
        Tag parentTag = new Tag( doc, NodeType.START_TAG);
        parentTag.setName( "foo" );
        doc.getRoot().addChild( parentTag );
        assertEquals( "<foo>", parentTag.toString() );
        assertEquals( 1, doc.getRoot().getChildren().size() );
        
        // Add child tag <bar/>
        Tag childTag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        childTag.setName( "bar" );
        parentTag.addChild( childTag );
        assertEquals( 1, doc.getRoot().getChildren().size() );
        
        // Add a second child <golf/>  before <bar>
        childTag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        childTag.setName( "golf" );
        parentTag.addChild( childTag, 0 );
        
        // Should result in <foo><golf/><bar/> (with golf and bar children of foo)
        assertEquals( "<foo><golf/><bar/>", doc.toString() );
        assertEquals( 1, doc.getRoot().getChildren().size() );
        assertEquals( 2, doc.getRoot().getChildren().get( 0 ).getChildren().size() );
    }
    
    public static Test suite()
    {
        return new TestSuite( TagTest.class );
    }

}
