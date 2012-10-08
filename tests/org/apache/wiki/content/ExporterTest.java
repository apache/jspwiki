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
package org.apache.wiki.content;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.providers.ProviderException;

public class ExporterTest extends TestCase
{
    private TestEngine m_engine;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        
        props.load( TestEngine.findTestProperties() );

        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "true" );
        
        TestEngine.emptyWorkDir();
        m_engine = new TestEngine(props);  
    }

    protected void tearDown() throws ProviderException
    {
        m_engine.deletePage("FooBar");
    }
    
    // FIXME: Not yet completed.
    public void testExport1() throws Exception
    {
        m_engine.saveText( "FooBar", "test" );

        m_engine.addAttachment( "FooBar", "test.jpg", "1234567890".getBytes() );
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        Exporter x = new Exporter(out,false);
        
        x.export(m_engine);
        
        String res = out.toString( "UTF-8" );
        
        System.out.println("Result is");
        System.out.println(res);
    }
}
