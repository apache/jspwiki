
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.io.*;
import java.util.*;
import javax.servlet.ServletException;

public class GoDiagramTest extends TestCase
{
    GoDiagram dia = new GoDiagram();

    public GoDiagramTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testSides1()
    {
        String src = "-------\n"+
                     "| . . .\n"+
                     "| . . .\n"+
                     "| . . .\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    info.topSide );
        assertTrue( "bottom", !info.bottomSide );
        assertTrue( "left",   info.leftSide );
        assertTrue( "right",  !info.rightSide );
    }

    public void testSides2()
    {
        String src = "+------\n"+
                     "| . . .\n"+
                     "| . . .\n"+
                     "| . . .\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    info.topSide );
        assertTrue( "bottom", !info.bottomSide );
        assertTrue( "left",   info.leftSide );
        assertTrue( "right",  !info.rightSide );
    }


    public void testSides3()
    {
        String src = " ------\n"+
                     "| . . .\n"+
                     "| . . .\n"+
                     "| . . .\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    info.topSide );
        assertTrue( "bottom", !info.bottomSide );
        assertTrue( "left",   info.leftSide );
        assertTrue( "right",  !info.rightSide );
    }


    public void testSides4()
    {
        String src = "| . . .\n"+
                     "| . . .\n"+
                     "| . . .\n"+
                     " ------\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    !info.topSide );
        assertTrue( "bottom", info.bottomSide );
        assertTrue( "left",   info.leftSide );
        assertTrue( "right",  !info.rightSide );
    }

    public void testSides5()
    {
        String src = ". . . |\n"+
                     ". . . |\n"+
                     ". . . |\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    !info.topSide );
        assertTrue( "bottom", !info.bottomSide );
        assertTrue( "left",   !info.leftSide );
        assertTrue( "right",  info.rightSide );
    }


    public void testSides6()
    {
        String src = ". . . |\n"+
                     ". . . |\n"+
                     ". . . |\n"+
                     "----- \n";
        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    !info.topSide );
        assertTrue( "bottom", info.bottomSide );
        assertTrue( "left",   !info.leftSide );
        assertTrue( "right",  info.rightSide );
    }

    public void testSides7()
    {
        String src = ". . . \n"+
                     ". . . \n"+
                     ". . . \n";
        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    !info.topSide );
        assertTrue( "bottom", !info.bottomSide );
        assertTrue( "left",   !info.leftSide );
        assertTrue( "right",  !info.rightSide );
    }

    public void testSides8()
    {
        String src = "-----\n"+
                     ". . . \n"+
                     ". . . \n"+
                     ". . . \n"+
                     "-----\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    info.topSide );
        assertTrue( "bottom", info.bottomSide );
        assertTrue( "left",   !info.leftSide );
        assertTrue( "right",  !info.rightSide );
    }

    public void testSides9()
    {
        String src = " --------\n"+
                     "| . . . |\n"+
                     "| . . . |\n"+
                     "| . . . |\n"+
                     "--------\n";

        GoDiagram.DiagramInfo info = dia.getDiagramInfo(src);

        assertTrue( "top",    info.topSide );
        assertTrue( "bottom", info.bottomSide );
        assertTrue( "left",   info.leftSide );
        assertTrue( "right",  info.rightSide );
    }


    public static Test suite()
    {
        return new TestSuite( GoDiagramTest.class );
    }
}
