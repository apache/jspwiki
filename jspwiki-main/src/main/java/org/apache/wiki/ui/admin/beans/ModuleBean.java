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
package org.apache.wiki.ui.admin.beans;

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.Release;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.ui.admin.SimpleAdminBean;
import org.apache.wiki.util.XHTML;
import org.apache.wiki.util.XhtmlUtil;
import org.jdom2.Element;

import javax.management.NotCompliantMBeanException;
import java.util.Collection;

public abstract class ModuleBean extends SimpleAdminBean {

    private static final String VER_WARNING = "<span class='warning'>This module is not compatible with this version of JSPWiki.</span>";

    public ModuleBean( final Engine engine ) throws NotCompliantMBeanException {
        m_engine = engine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAttributeNames() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getMethodNames() {
        return new String[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String doGet( final Context context ) {
        final Collection< WikiModuleInfo > filters = modules();
        final Element root = title();
        final Element tb = containerForModuleDetail( root );

        final Element trHead = heading();
        tb.addContent( trHead );

        for( final WikiModuleInfo info : filters ) {
            final Element tr = rowBody( info );
            tb.addContent( tr );
        }

        return XhtmlUtil.serialize( root, XhtmlUtil.EXPAND_EMPTY_NODES );
    }

    protected Element title() {
        final Element root = XhtmlUtil.element( XHTML.div );
        root.addContent( XhtmlUtil.element( XHTML.h4 ).addContent( getTitle() ) );
        return root;
    }

    protected Element containerForModuleDetail( final Element root ) {
        final Element tb = XhtmlUtil.element( XHTML.table ).setAttribute( "border", "1" );
        root.addContent( tb );
        return tb;
    }

    /**
     * Obtains the collection of modules which is going to be inspected at {@link #doGet(Context)}.
     *
     * @return a collection of {@link WikiModuleInfo}
     */
    protected abstract Collection< WikiModuleInfo > modules();

    /**
     * html blob describing the values of each {@link WikiModuleInfo} inspected.
     *
     * @return {@link Element} describing the values of each {@link WikiModuleInfo} inspected.
     */
    protected abstract Element heading();

    /**
     * html blob describing{@link Element} describing attributes
     *
     * @param module {@link WikiModuleInfo} inspected.
     * @return {@link Element} describing the {@link Element} inspected.
     */
    protected abstract Element rowBody( WikiModuleInfo module );

    protected String validModuleVersion( final WikiModuleInfo info ) {
        return Release.isNewerOrEqual( info.getMinVersion() ) && Release.isOlderOrEqual( info.getMaxVersion() )
               ? StringUtils.EMPTY
               : VER_WARNING;
    }

}
