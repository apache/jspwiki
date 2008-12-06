package com.ecyrd.jspwiki.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;

public class DenouncePluginTest extends TestCase {
    Properties props = new Properties();
    TestEngine engine;
    WikiContext context;
    PluginManager manager;
    Properties denounceProps;
    private final String pluginCmdLine = "[{Denounce link='http://www.mobileasses.com' text='peoples asses'}]";

    public DenouncePluginTest(String s) {
        super(s);
    }

    public void setUp()
            throws Exception {
        props.load(TestEngine.findTestProperties());

        engine = new TestEngine(props);
        try {


            ClassLoader loader = Denounce.class.getClassLoader();
            InputStream in = loader.getResourceAsStream("com/ecyrd/jspwiki/plugin/denounce.properties");

            if (in == null) {
                throw new IOException("No property file found! (Check the installation, it should be there.)");
            }
            denounceProps = new Properties();
            denounceProps.load(in);
        } catch (IOException e) {
            fail("failed to load denounce.properties");
        }


    }

    private void setupHTTPRequest(String host, String header) {
        MockHttpServletRequest request = engine.newHttpRequest();
        if (header != null)
            request.addHeader("User-Agent", header);
        //if(host != null)

        request.getParameterMap().put("page", new String[]{"TestPage"});
        context = engine.getWikiContextFactory().newViewContext( request, null, null );
        manager = new PluginManager(engine, props);
    }

    public void tearDown() {
        TestEngine.deleteTestPage("TestPage");
        TestEngine.deleteTestPage("Foobar");
        TestEngine.emptyWorkDir();
    }

    public void testSLURPBot() throws Exception {
        setupHTTPRequest(null, "Slurp/2.1");
        String res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);
        //
        setupHTTPRequest(null, "ETSlurp/");
        res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);

        setupHTTPRequest(null, "Slurp");
        res = manager.execute(context, pluginCmdLine);
        assertFalse(getDenounceText().equalsIgnoreCase(res));

    }
      public void testGoogleBotWithWrongCase() throws Exception {
        setupHTTPRequest(null, "gOOglebot/2.1");
        String res = manager.execute(context, pluginCmdLine);
        assertFalse(getDenounceText().equalsIgnoreCase(res));
      }
    public void testGoogleBot() throws Exception {
        setupHTTPRequest(null, "Googlebot/2.1");
        String res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);
        //
        setupHTTPRequest(null, "ETSGooglebot/2.1");
        res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);

        setupHTTPRequest(null, "ETSGooglebot");
        res = manager.execute(context, pluginCmdLine);
        assertEquals(getDenounceText(), res);

    }

    public void testPlugin() throws Exception {
        setupHTTPRequest(null, null);

        String res = manager.execute(context, pluginCmdLine);

        assertEquals("<a href=\"http://www.mobileasses.com\">peoples asses</a>", res);

    }

    private String getDenounceText() {
        return denounceProps.getProperty("denounce.denouncetext");
    }


    public static Test suite() {
        return new TestSuite(DenouncePluginTest.class);
    }
}