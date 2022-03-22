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
package org.apache.wiki.api.engine;

import org.apache.wiki.api.core.Engine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Properties;


@ExtendWith( MockitoExtension.class )
class EngineLifecycleExtensionTest {

    @Mock Engine engine;

    @Test
    void shouldInvokeEngineLifecycleExtensionTestImpl() throws Exception {
        final Properties properties = new Properties();
        Mockito.doAnswer( invocation -> {
            final String actual = invocation.getArgument( 0, Properties.class ).getProperty( "test" );
            Assertions.assertEquals( "onInit", actual );
            return null; // void
        } ).when( engine ).initialize( Mockito.any( Properties.class ) );
        Mockito.doReturn( properties ).when( engine ).getWikiProperties();
        Mockito.doCallRealMethod().when( engine ).start( Mockito.any( Properties.class ) );
        Mockito.doCallRealMethod().when( engine ).stop();

        engine.start( properties );
        Assertions.assertEquals( "onStart", properties.getProperty( "test" ) );

        engine.stop();
        Assertions.assertEquals( "onShutdown", properties.getProperty( "test" ) );
    }

}
