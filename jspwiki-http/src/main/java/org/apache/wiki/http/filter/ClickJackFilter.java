/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.http.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.apache.wiki.util.HttpUtil;

import java.io.IOException;


/**
 * X-Frame-Options: Prevents clickjacking attacks by controlling whether a page can be
 * rendered within an {@code <frame>}, {@code <iframe>}, {@code <embed>}, or {@code <object>}.
 */
public class ClickJackFilter implements Filter {

    private String mode = "DENY";

    /** {@inheritDoc} */
    @Override
    public void init( final FilterConfig filterConfig ) {
        final String configMode = FilterOperations.initValue( filterConfig, "mode", "clickjack.mode" );
        if( configMode != null && ( configMode.equals( "DENY" ) || configMode.equals( "SAMEORIGIN" ) ) ) {
            mode = configMode;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws IOException, ServletException {
        HttpUtil.addHeader( response,"X-FRAME-OPTIONS", mode );
        chain.doFilter( request, response );
    }

}
