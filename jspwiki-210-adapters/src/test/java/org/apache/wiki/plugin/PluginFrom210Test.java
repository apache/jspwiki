package org.apache.wiki.plugin;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class PluginFrom210Test {

    @Test
    public void testPluginNotUsingPublicApiStillWorks() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( PluginManager.PROP_SEARCHPATH, "com.example.plugins" );
        final WikiEngine engine = TestEngine.build( props );
        final PluginManager pm = engine.getManager( PluginManager.class );
        final WikiContext context = new WikiContext( engine, new WikiPage( engine, "Testpage" ) );

        final String res = pm.execute( context,"{TwoXPlugin}" );
        Assertions.assertEquals( "hakuna matata", res );
    }

}
