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

import org.apache.wiki.TestEngine;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * Regression tests for the Image plugin style/class/align validation: author CSS must not be able to reposition
 * content outside the plugin's own box (overlay phishing).
 */
public class ImageStyleTest {

    static TestEngine testEngine = TestEngine.build();

    @Test
    public void overlayStyleIsDropped() throws Exception {
        final String src = "[{Image src='img.png' link='https://evil.example/fake-login' style='position:fixed;top:0;left:0;width:100vw;height:100vh;z-index:9999;background:#fff'}]";
        testEngine.saveText( "ImageStylePage1", src );
        final String res = testEngine.getManager( RenderingManager.class ).getHTML( "ImageStylePage1" );
        Assertions.assertFalse( res.contains( "position" ), res );
        Assertions.assertFalse( res.contains( "z-index" ), res );
    }

    @Test
    public void benignStyleIsKept() throws Exception {
        final String src = "[{Image src='img.png' style='width:120px; border: 1px solid'}]";
        testEngine.saveText( "ImageStylePage2", src );
        final String res = testEngine.getManager( RenderingManager.class ).getHTML( "ImageStylePage2" );
        Assertions.assertTrue( res.contains( "width:120px" ), res );
    }

    @Test
    public void invalidAlignAndClassAreDropped() throws Exception {
        final String src = "[{Image src='img.png' align='none;position:fixed' class='x onmouseover=alert(1)'}]";
        testEngine.saveText( "ImageStylePage3", src );
        final String res = testEngine.getManager( RenderingManager.class ).getHTML( "ImageStylePage3" );
        Assertions.assertFalse( res.contains( "position:fixed" ), res );
        Assertions.assertFalse( res.contains( "onmouseover" ), res );
    }

    @Test
    public void isSafeStyleRejectsRepositioningAndEscapes() {
        Assertions.assertTrue( Image.isSafeStyle( "width:120px; border: 1px solid" ) );
        Assertions.assertFalse( Image.isSafeStyle( "position:fixed;top:0" ) );
        Assertions.assertFalse( Image.isSafeStyle( "\\70 osition:fixed" ) );
        Assertions.assertFalse( Image.isSafeStyle( "margin-top:-9999px" ) );
        Assertions.assertFalse( Image.isSafeStyle( "background:url(//evil.example/x)" ) );
        Assertions.assertFalse( Image.isSafeStyle( "transform:translate(-100px,-100px)" ) );
    }

    @Test
    public void isSafeCssClassAcceptsIdentifiersOnly() {
        Assertions.assertTrue( Image.isSafeCssClass( "imageplugin" ) );
        Assertions.assertTrue( Image.isSafeCssClass( "one two-three" ) );
        Assertions.assertFalse( Image.isSafeCssClass( "x{color:red}" ) );
        Assertions.assertFalse( Image.isSafeCssClass( "a;b" ) );
    }

}
