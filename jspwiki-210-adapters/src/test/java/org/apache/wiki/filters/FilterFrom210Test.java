/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.filters;

import com.example.filters.TwoXFilter;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class FilterFrom210Test {

    @Test
    public void testFilterNotUsingPublicApiStillWorks() throws WikiException {
        final Properties props = TestEngine.getTestProperties();
        // props.setProperty( FilterManager.PROP_FILTERXML, "filters.xml" );
        final TestEngine engine = TestEngine.build( props ); // trigger page filter#initialize
        final FilterManager fm = engine.getManager( FilterManager.class );
        final RenderingManager rm = engine.getManager( RenderingManager.class );
        Assertions.assertTrue( fm.getFilterList().stream().anyMatch( f -> f instanceof TwoXFilter ) );

        engine.saveText( "Testpage", "Incredible and super important content here" ); // trigger pre / post methods
        final TwoXFilter txf = ( TwoXFilter )fm.getFilterList().stream().filter( f -> f instanceof TwoXFilter ).findAny().get();
        // post save triggers page references' update which in turn renders the page, which in turn triggers the preTranslate
        // filter method, so we end up with 5 invocations to any given filter on a page save + 1 more from initialize
        Assertions.assertEquals( 6, txf.invocations() );

        final WikiContext context = new WikiContext( engine, new WikiPage( engine, "Testpage" ) );
        final String res = rm.textToHTML( context,"Incredible and super important content here" ); // test only pre / post translate
        Assertions.assertEquals( "see how I care about yor content - hmmm...", res );
    }

}
