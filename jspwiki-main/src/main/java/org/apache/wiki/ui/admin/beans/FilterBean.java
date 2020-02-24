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

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.engine.FilterManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.util.XHTML;
import org.apache.wiki.util.XhtmlUtil;
import org.jdom2.Element;

import javax.management.NotCompliantMBeanException;
import java.util.Collection;


public class FilterBean extends ModuleBean {

    public FilterBean( final Engine engine ) throws NotCompliantMBeanException {
        super( engine );
    }

    /**
     * {@inheritDoc}
     */
    @Override public String getTitle() {
        return "Filters";
    }

    /**
     * {@inheritDoc}
     */
    @Override public int getType() {
        return CORE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Collection< WikiModuleInfo > modules() {
        return m_engine.getManager( FilterManager.class ).modules();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Element heading() {
        final Element trHead = XhtmlUtil.element( XHTML.tr );
        trHead.addContent( XhtmlUtil.element( XHTML.th ).addContent( "Name" ) )
              .addContent( XhtmlUtil.element( XHTML.th ).addContent( "Author" ) )
              .addContent( XhtmlUtil.element( XHTML.th ).addContent( "Notes" ) );
        return trHead;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Element rowBody( final WikiModuleInfo info ) {
        final Element tr = XhtmlUtil.element( XHTML.tr );
        tr.addContent( XhtmlUtil.element( XHTML.td ).addContent( info.getName() ) )
          .addContent( XhtmlUtil.element( XHTML.td ).addContent( info.getAuthor() ) )
          .addContent( XhtmlUtil.element( XHTML.td ).addContent( validModuleVersion( info ) ) );
        return tr;
    }

}
