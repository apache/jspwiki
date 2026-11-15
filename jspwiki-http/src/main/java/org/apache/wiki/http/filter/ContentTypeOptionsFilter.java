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
 * X-Content-Type-Options: Prevents MIME type sniffing, which can lead to XSS
 * attacks if browsers misinterpret content types.
 */
public class ContentTypeOptionsFilter implements Filter {

    private String mode = "nosniff";

    /** {@inheritDoc} */
    @Override
    public void init( final FilterConfig filterConfig ) {
        final String configMode = FilterOperations.initValue( filterConfig, "ContentTypeOptionsValue", "content-type-options.value" );
        if (configMode != null) {
            mode = configMode;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doFilter( final ServletRequest request, final ServletResponse response, final FilterChain chain ) throws IOException, ServletException {
        HttpUtil.addHeader( response,"X-Content-Type-Options", mode );
        chain.doFilter( request, response );
    }

}
