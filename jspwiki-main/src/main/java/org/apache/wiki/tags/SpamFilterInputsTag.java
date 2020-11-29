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


import org.apache.wiki.filters.SpamFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

/**
 * Provides hidden input fields which are checked by the {@code SpamFilter}.
 *
 * @since 2.11.0-M8
 */
public class SpamFilterInputsTag extends WikiTagBase {

    /**
     * {@inheritDoc}
     */
    @Override
    public int doWikiStartTag() throws Exception {
        final String encodingCheckInput = SpamFilter.insertInputFields( pageContext );
        final String hashCheckInput =
            "<input type='hidden' name='" + SpamFilter.getHashFieldName( ( HttpServletRequest ) pageContext.getRequest() ) + "'" +
            " value='" + pageContext.getAttribute( "lastchange", PageContext.REQUEST_SCOPE ) + "' />\n";

        // This following field is only for the SpamFilter to catch bots which are just randomly filling all fields and submitting.
        // Normal user should never see this field, nor type anything in it.
        final String botCheckInput =
            "<input class='hidden' type='text' name='" + SpamFilter.getBotFieldName() + "' id='" + SpamFilter.getBotFieldName() + "' value='' />\n";
        pageContext.getOut().print( encodingCheckInput + hashCheckInput + botCheckInput );
        return SKIP_BODY;
    }

}
