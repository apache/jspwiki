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
package org.apache.wiki.tags;

import org.apache.wiki.api.core.Session;
import org.apache.wiki.http.filter.CsrfProtectionFilter;

/**
 * Outputs the hidden {@link CsrfProtectionFilter#ANTICSRF_PARAM}.
 */
public class CsrfProtectionTag extends WikiTagBase {

    private static final long serialVersionUID = -6828306125406112417L;
    private boolean meta;

    public void setFormat( final String format ) {
        meta = "meta".equalsIgnoreCase( format );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int doWikiStartTag() throws Exception {
        final Session session = m_wikiContext.getWikiSession();
        final String csrfProtectionHidden;
        if( meta ) {
            csrfProtectionHidden = "<meta name=\"wikiCsrfProtection\" content='" + session.antiCsrfToken() + "'/>";
        } else {
            csrfProtectionHidden = "<input type=\"hidden\" name=\"" + CsrfProtectionFilter.ANTICSRF_PARAM + "\" " +
                                          "id=\"" + CsrfProtectionFilter.ANTICSRF_PARAM + "\" " +
                                          "value=\"" + session.antiCsrfToken() + "\"/>";
        }
        pageContext.getOut().print( csrfProtectionHidden );
        return SKIP_BODY;
    }

}
