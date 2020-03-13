package org.apache.wiki.filters;

import com.example.filters.TwoXFilter;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class FilterFrom210Test {

    @Test
    public void testFilterNotUsingPublicApiStillWorks() {
        final Properties props = TestEngine.getTestProperties();
        // props.setProperty( FilterManager.PROP_FILTERXML, "filters.xml" );
        final WikiEngine engine = TestEngine.build( props ); // trigger page filter#initialize
        final FilterManager fm = engine.getManager( FilterManager.class );
        final RenderingManager rm = engine.getManager( RenderingManager.class );
        Assertions.assertTrue( fm.getFilterList().stream().anyMatch( f -> f instanceof TwoXFilter ) );

        final WikiContext context = new WikiContext( engine, new WikiPage( engine, "Testpage" ) );
        final String res = rm.textToHTML( context,"Incredible and super important content here" ); // trigger pre / post translate
        Assertions.assertEquals( "see how I care about yor content - hmmm...", res );
    }

}
