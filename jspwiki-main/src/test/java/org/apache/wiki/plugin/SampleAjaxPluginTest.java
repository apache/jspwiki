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
package org.apache.wiki.plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class SampleAjaxPluginTest {

    @Test
    public void testParamsCannotBreakOutOfOnclickAttribute() throws Exception {
        final SampleAjaxPlugin plugin = new SampleAjaxPlugin();
        final String payload = "\'><img src=x onerror=alert(document.cookie)>";
        final String result = plugin.execute( null, Map.of( "params", payload ) );
        Assertions.assertFalse( result.contains( payload ), "raw payload must not reach the HTML output" );
        Assertions.assertFalse( result.contains( "<img" ), "attribute breakout must not reach the HTML output" );
    }

    @Test
    public void testJsStringEscape() {
        Assertions.assertEquals( "\\u0027\\u003e\\u003cimg", SampleAjaxPlugin.jsStringEscape( "\'><img" ) );
        Assertions.assertEquals( "abc123", SampleAjaxPlugin.jsStringEscape( "abc123" ) );
        Assertions.assertEquals( "", SampleAjaxPlugin.jsStringEscape( null ) );
    }

}
