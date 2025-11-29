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

package org.apache.wiki;

import org.apache.wiki.tags.ParamHandler;
import org.apache.wiki.tags.ParamTag;
import org.junit.jupiter.api.Test;

import jakarta.servlet.jsp.tagext.BodyContent;
import jakarta.servlet.jsp.tagext.BodyTagSupport;
import jakarta.servlet.jsp.tagext.Tag;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParamTagTest {


    @Test
    public void testDoEndTagWithNonParamHandlerParent() {
        // Create a mock parent tag that is not a ParamHandler
        final Tag parentTag = mock(Tag.class);
        when(parentTag.getParent()).thenReturn(null); // End of the parent chain

        // Create the ParamTag under test and set its parent
        final ParamTag paramTag = new ParamTag();
        paramTag.setParent(parentTag);

        // Call the method under test
        final int result = paramTag.doEndTag();

        // Assert that the method terminated and returned the expected value
        assertEquals(Tag.EVAL_PAGE, result);
    }


    @Test
    public void testDoEndTagWithInfiniteLoop() throws Exception {
        // Create a tag that will cause an infinite loop
        final Tag parentTag = mock(Tag.class);
        when(parentTag.getParent()).thenReturn(parentTag);

        final ParamTagOld paramTag = new ParamTagOld();
        paramTag.setParent(parentTag);

        // Run the code that should cause an infinite loop in a separate thread
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<Integer> future = executorService.submit(paramTag::doEndTag);

        try {
            // Allow the test to run for 5 seconds. If it takes longer, consider it an infinite loop.
            future.get(5, TimeUnit.SECONDS);
            fail("Expected an infinite loop, but code terminated normally.");
        } catch (final TimeoutException e) {
            // This is the expected outcome, as the code should run indefinitely.
        } finally {
            // Clean up by shutting down the executor
            executorService.shutdownNow();
        }
    }

    @Test
    void testNewCodeDoesNotCauseInfiniteLoop() {
        final ParamTag paramTag = new ParamTag(); // New code class

        final Tag parentTag = mock(Tag.class);
        when(parentTag.getParent()).thenReturn(parentTag); // Parent returns itself

        paramTag.setParent(parentTag);

        assertTimeout(Duration.ofSeconds(5), () -> {
            paramTag.doEndTag();
        }, "Execution should not have taken more than 5 seconds");
    }


    static class ParamTagOld
            extends BodyTagSupport {

        private static final long serialVersionUID = -4671059568218551633L;
        private String m_name;
        private String m_value;

        @Override
        public void release() {
            m_name = m_value = null;
        }

        public void setName(final String s) {
            m_name = s;
        }

        public void setValue(final String s) {
            m_value = s;
        }

        @Override
        public int doEndTag() {
            Tag t;
            do {
                t = getParent();
            } while (t != null && !(t instanceof ParamHandler));

            if (t != null) {
                String val = m_value;
                if (val == null) {
                    final BodyContent bc = getBodyContent();
                    if (bc != null) {
                        val = bc.getString();
                    }
                }
                if (val != null) {
                    ((ParamHandler) t).setContainedParameter(m_name, val);
                }
            }


            return EVAL_PAGE;
        }
    }
}