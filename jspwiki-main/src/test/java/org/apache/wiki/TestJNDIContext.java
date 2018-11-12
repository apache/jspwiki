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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * <p>Mock JNDI context that permits String names to be bound to objects. 
 * It is intended to simulate JNDI factories in web containers and
 * application servers, and provides only the bare minimum functions.
 * This class contains a static member InitialContextFactory class 
 * called Factory that installs a "root" instance of TestJNDIContext
 * as the initial context for the entire JVM. Additional contexts can 
 * be bound by supplying pairs of Strings and Objects using
 * {@link #bind(String, Object)}. Objects are looked up and retrieved 
 * using {@link #lookup(String)}. All other methods in this class no-op.</p>
 * <p>For example, simulating a JNDI DataSource requires us to first
 * establish an initial context of <code>java:comp/env</code>. Inside
 * the initial context, we bind our data source:</p>
 * <blockquote><code>Context initCtx = new InitialContext();<br/>
 * initCtx.bind( "java:comp/env", new TestJNDIContext() );<br/>
 * Context ctx = (Context)initCtx.lookup("java:comp/env");<br/>
 * DataSource ds = new TestJDBCDataSource();<br/>
 * ctx.bind( "jdbc/UserDatabase", ds);<br/>
 * </code></blockquote>
 * 
 * @since 2.3
 */
public class TestJNDIContext implements Context
{

    private final Map<String, Object> m_bindings  = new HashMap<>();

    private static boolean initialized = false;

    /**
     * InitialContextFactory class that configures the JVM to
     * always return a particular TestJNDIContext.
     */
    public static class Factory implements InitialContextFactory
    {

        private static Context ctx = null;

        @Override
        public Context getInitialContext( Hashtable<?,?> environment ) throws NamingException
        {
            return ctx;
        }

        protected static void setContext( Context context )
        {
            if ( ctx == null )
            {
                ctx = context;
            }
        }
    }

    /**
     * Constructs a new mock JNDI context. Note that the instance has no
     * relationship to the JVM's initial context <em>per se</em>.
     * To configure the JVM so that it always returns a TestJNDIContext
     * instance, see {@link #initialize()}.
     */
    public TestJNDIContext()
    {
    }

    /**
     * Static factory method that creates a new TestJNDIContext and 
     * configures the JVM so that the <code>new InitialContext()</code>
     * always returns this context.
     */
    public static void initialize()
    {
        if ( !initialized )
        {
            System.setProperty( Context.INITIAL_CONTEXT_FACTORY, Factory.class.getName() );
            Factory.setContext( new TestJNDIContext() );
            initialized = true;
        }
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#addToEnvironment(java.lang.String, java.lang.Object)
     */
    @Override
    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    @Override
    public void bind( Name name, Object obj ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#close()
     */
    @Override
    public void close() throws NamingException
    {
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#composeName(javax.naming.Name, javax.naming.Name)
     */
    @Override
    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    @Override
    public String composeName( String name, String prefix ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    @Override
    public Context createSubcontext( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    @Override
    public Context createSubcontext( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    @Override
    public void destroySubcontext( Name name ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    @Override
    public void destroySubcontext( String name ) throws NamingException
    {
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getEnvironment()
     */
    @Override
    public Hashtable<?,?> getEnvironment() throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getNameInNamespace()
     */
    @Override
    public String getNameInNamespace() throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    @Override
    public NameParser getNameParser( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    @Override
    public NameParser getNameParser( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    @Override
    public NamingEnumeration<NameClassPair> list( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#list(java.lang.String)
     */
    @Override
    public NamingEnumeration<NameClassPair> list( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    @Override
    public NamingEnumeration<Binding> listBindings( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    @Override
    public NamingEnumeration<Binding> listBindings( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    @Override
    public Object lookup( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * Binds an object to a supplied String key.
     * @see javax.naming.InitialContext#bind(java.lang.String, java.lang.Object)
     */
    @Override
    public void bind( String name, Object obj ) throws NamingException
    {
        m_bindings.put( name, obj );
    }

    /**
     * Retrieves an object using a String key. If not found, 
     * throws a NamingException.
     * @see javax.naming.InitialContext#lookup(java.lang.String)
     */
    @Override
    public Object lookup( String name ) throws NamingException
    {
        Object obj = m_bindings.get( name );
        if( obj == null )
        {
            throw new NamingException( "Object named '" + name + "' not found in JNDI context." );
        }
        return obj;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    @Override
    public Object lookupLink( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    @Override
    public Object lookupLink( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    @Override
    public void rebind( Name name, Object obj ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    @Override
    public void rebind( String name, Object obj ) throws NamingException
    {
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    @Override
    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    @Override
    public void rename( Name oldName, Name newName ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    @Override
    public void rename( String oldName, String newName ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    @Override
    public void unbind( Name name ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    @Override
    public void unbind( String name ) throws NamingException
    {
    }
}
