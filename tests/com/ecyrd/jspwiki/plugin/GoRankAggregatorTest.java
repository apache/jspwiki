
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import junit.framework.*;
import java.io.*;
import java.util.*;
import javax.servlet.ServletException;

public class GoRankAggregatorTest extends TestCase
{
    Properties props = new Properties();
    TestEngine testEngine;
    WikiContext context;
    PluginManager manager;
    GoRankAggregator agg;

    public GoRankAggregatorTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        testEngine = new TestEngine(props);

        agg = new GoRankAggregator();
        context = new WikiContext( testEngine, new WikiPage("Test") );

        agg.initialize( context, new HashMap() );
    }

    public void tearDown()
    {
        testEngine.deletePage( "Ranking1" );
    }

    public void testParseRank1()
        throws Exception
    {
        assertEquals( -1, agg.parseRank("1 dan") );
    }

    public void testParseRank2()
        throws Exception
    {
        assertEquals( -7, agg.parseRank("7d") );
    }

    public void testParseRank3()
        throws Exception
    {
        assertEquals( 15, agg.parseRank("15k") );
    }

    public void testParseRank4()
        throws Exception
    {
        assertEquals( 30, agg.parseRank("30kyu") );
    }

    public void testEGFParser1()
        throws Exception
    {
        String test = "    1  Laatikainen Vesa         Hel    5D   2461   +14   64     T021025B";

        List res = agg.parseEGFRatingsList( context, test );

        GoRankAggregator.UserInfo u1 = (GoRankAggregator.UserInfo) res.get(0);

        assertEquals( "last name", "Laatikainen", u1.m_lastName );
        assertEquals( "first name", "Vesa", u1.m_firstName );
        assertEquals( "rank", -5, u1.m_rank );
        assertEquals( "egf", 2461, u1.m_egfRank );
    }

    /**
     *  Reads in sample test file.
     */
    public void testEGFParser2()
        throws Exception
    {        
        InputStream in = getClass().getClassLoader().getResourceAsStream("/ranks_allfi.html");

        if( in == null ) throw new FileNotFoundException("ranks_allfi.html NOT FOUND!");

        StringWriter out = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), out );
        out.close();

        String test = out.toString();

        List res = agg.parseEGFRatingsList( context, test );

        GoRankAggregator.UserInfo u1 = (GoRankAggregator.UserInfo) res.get(0);

        assertEquals( "last name", "Laatikainen", u1.m_lastName );
        assertEquals( "first name", "Vesa", u1.m_firstName );
        assertEquals( "rank", -5, u1.m_rank );
        assertEquals( "egf", 2461, u1.m_egfRank );

        u1 = (GoRankAggregator.UserInfo) res.get(76);

        assertEquals( "76: last name", "Ramo", u1.m_lastName );
        assertEquals( "76: first name", "Jouni", u1.m_firstName );
        assertEquals( "76: rank", 20, u1.m_rank );
        assertEquals( "76: egf", 100, u1.m_egfRank );
    }

    public void testAggregate()
        throws Exception
    {
        testEngine.saveText("Rating1", "----\n\n{{{\nClub: Helsinki\n"+
                            "Jalkanen, Janne, 7k\n"+
                            "Eskelinen, Olli, 2k\n"+
                            "Gobble, Foo, 2k\n\n}}}\n");

        List results = agg.getPersonsFromPage( context, "Rating1" );

        assertEquals( "size", 3, results.size() );

        GoRankAggregator.UserInfo u1 = (GoRankAggregator.UserInfo) results.get(0);
        GoRankAggregator.UserInfo u2 = (GoRankAggregator.UserInfo) results.get(1);
        GoRankAggregator.UserInfo u3 = (GoRankAggregator.UserInfo) results.get(2);

        assertEquals("User1 name", "Jalkanen", u1.m_lastName );
        assertEquals("User2 name", "Eskelinen", u2.m_lastName );
        assertEquals("User3 name", "Gobble", u3.m_lastName );

        assertEquals("User1 rank", 7, u1.m_rank );
        assertEquals("User2 rank", 2, u2.m_rank );
        assertEquals("User3 rank", 2, u3.m_rank );
    }

    public static Test suite()
    {
        return new TestSuite( GoRankAggregatorTest.class );
    }
}
