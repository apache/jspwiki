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
 * Strict-Transport-Security (HSTS): Enforces HTTPS-only communication,
 * preventing downgrade attacks and cookie hijacking.
 */
public class STSFilter implements Filter {

    private String mode = "max-age=63072000; includeSubDomains; preload";

    /** {@inheritDoc} */
    @Override
    public void init( final FilterConfig filterConfig ) {
        final String configMode = FilterOperations.initValue( filterConfig, "STSValue", "sts.value" );
        if( configMode != null ) {
            mode = configMode;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws IOException, ServletException {
        HttpUtil.addHeader( response,"Strict-Transport-Security", mode );
        chain.doFilter( request, response );
    }

}
