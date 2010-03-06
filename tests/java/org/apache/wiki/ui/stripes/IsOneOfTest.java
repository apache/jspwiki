/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.ui.stripes;

import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.util.ResolverUtil;

import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.filters.PageFilter;
import org.apache.wiki.plugin.WikiPlugin;

public class IsOneOfTest extends TestCase
{
    public void testCoreMatches()
    {
        ResolverUtil<Object> resolver = new ResolverUtil<Object>();
        ResolverUtil.Test test = new IsOneOf( WikiActionBean.class, PageFilter.class, WikiPlugin.class );
        resolver.find( test, "org.apache.wiki" );
        Set<Class<? extends Object>> matches = resolver.getClasses();
        assertEquals( 73, matches.size() );
    }

    public void testActionBeanMatches()
    {
        ResolverUtil<Object> resolver = new ResolverUtil<Object>();
        ResolverUtil.Test test = new IsOneOf( WikiActionBean.class );
        resolver.find( test, "org.apache.wiki.action" );
        Set<Class<? extends Object>> matches = resolver.getClasses();
        assertEquals( 27, matches.size() );
    }

    public void testPageFilterMatches()
    {
        ResolverUtil<Object> resolver = new ResolverUtil<Object>();
        ResolverUtil.Test test = new IsOneOf( PageFilter.class );
        resolver.find( test, "org.apache.wiki.filters" );
        Set<Class<? extends Object>> matches = resolver.getClasses();
        assertEquals( 6, matches.size() );
    }

    public void testWikiPluginMatches()
    {
        ResolverUtil<Object> resolver = new ResolverUtil<Object>();
        ResolverUtil.Test test = new IsOneOf( WikiPlugin.class );
        resolver.find( test, "org.apache.wiki.plugin" );
        Set<Class<? extends Object>> matches = resolver.getClasses();
        assertEquals( 30, matches.size() );
    }

    public static Test suite()
    {
        return new TestSuite( IsOneOfTest.class );
    }
}
