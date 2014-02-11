package org.apache.wiki.ui.admin.beans;

import java.util.Properties;

import javax.management.NotCompliantMBeanException;

import junit.framework.TestCase;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;


public class PluginBeanTest extends TestCase {
    
    Properties props = TestEngine.getTestProperties();

    TestEngine testEngine;
    
    public void testDoGet() throws WikiException, NotCompliantMBeanException {
        testEngine = new TestEngine( props );
        WikiContext context = new WikiContext( testEngine, new WikiPage( testEngine, "TestPage01" ) );
        PluginBean pb = new PluginBean( testEngine );
        String expectedHtml = "<div>" +
                                "<h4>Plugins</h4>" +
                                "<table border=\"1\">" +
                                  "<tr><th>Name</th><th>Alias</th><th>Author</th><th>Notes</th></tr>" +
                                  "<tr><td>IfPlugin</td><td>If</td><td>Janne Jalkanen</td><td></td></tr>" +
                                  "<tr><td>Note</td><td></td><td>Janne Jalkanen</td><td></td></tr>" +
                                  "<tr><td>SamplePlugin</td><td>samplealias</td><td>Janne Jalkanen</td><td></td></tr>" +
                                  "<tr><td>SamplePlugin2</td><td>samplealias2</td><td>Janne Jalkanen</td><td></td></tr>" +
                                "</table>" +
                              "</div>";
        assertEquals( expectedHtml, pb.doGet( context ) );
    }

}
