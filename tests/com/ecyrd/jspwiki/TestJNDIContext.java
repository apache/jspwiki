package com.ecyrd.jspwiki;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
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
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1 $ $Date: 2005-10-19 12:13:23 $
 * @since 2.3
 */
public class TestJNDIContext implements Context
{

    private final Map      m_bindings  = new HashMap();

    private static boolean initialized = false;

    /**
     * InitialContextFactory class that configures the JVM to
     * always return a particular TestJNDIContext.
     * @author Andrew R. Jaquith
     * @version $Revision: 1.1 $ $Date: 2005-10-19 12:13:23 $
     */
    public static class Factory implements InitialContextFactory
    {

        private static Context ctx = null;

        public Context getInitialContext( Hashtable environment ) throws NamingException
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
    public Object addToEnvironment( String propName, Object propVal ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#bind(javax.naming.Name, java.lang.Object)
     */
    public void bind( Name name, Object obj ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#close()
     */
    public void close() throws NamingException
    {
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#composeName(javax.naming.Name, javax.naming.Name)
     */
    public Name composeName( Name name, Name prefix ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#composeName(java.lang.String, java.lang.String)
     */
    public String composeName( String name, String prefix ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#createSubcontext(javax.naming.Name)
     */
    public Context createSubcontext( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#createSubcontext(java.lang.String)
     */
    public Context createSubcontext( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#destroySubcontext(javax.naming.Name)
     */
    public void destroySubcontext( Name name ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#destroySubcontext(java.lang.String)
     */
    public void destroySubcontext( String name ) throws NamingException
    {
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getEnvironment()
     */
    public Hashtable getEnvironment() throws NamingException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getNameInNamespace()
     */
    public String getNameInNamespace() throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getNameParser(javax.naming.Name)
     */
    public NameParser getNameParser( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#getNameParser(java.lang.String)
     */
    public NameParser getNameParser( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#list(javax.naming.Name)
     */
    public NamingEnumeration list( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#list(java.lang.String)
     */
    public NamingEnumeration list( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#listBindings(javax.naming.Name)
     */
    public NamingEnumeration listBindings( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#listBindings(java.lang.String)
     */
    public NamingEnumeration listBindings( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#lookup(javax.naming.Name)
     */
    public Object lookup( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * Binds an object to a supplied String key.
     * @see javax.naming.InitialContext#bind(java.lang.String, java.lang.Object)
     */
    public void bind( String name, Object obj ) throws NamingException
    {
        m_bindings.put( name, obj );
    }

    /**
     * Retrieves an object using a String key. If not found, 
     * throws a NamingException.
     * @see javax.naming.InitialContext#lookup(java.lang.String)
     */
    public Object lookup( String name ) throws NamingException
    {
        Object obj = m_bindings.get( name );
        if ( name == null )
        {
            throw new NamingException( "Object named '" + name + "' not found in JNDI context." );
        }
        return obj;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#lookupLink(javax.naming.Name)
     */
    public Object lookupLink( Name name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#lookupLink(java.lang.String)
     */
    public Object lookupLink( String name ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#rebind(javax.naming.Name, java.lang.Object)
     */
    public void rebind( Name name, Object obj ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#rebind(java.lang.String, java.lang.Object)
     */
    public void rebind( String name, Object obj ) throws NamingException
    {
    }

    /**
     * No-op; always returns <code>null</code>.
     * @see javax.naming.Context#removeFromEnvironment(java.lang.String)
     */
    public Object removeFromEnvironment( String propName ) throws NamingException
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.naming.Context#rename(javax.naming.Name, javax.naming.Name)
     */
    public void rename( Name oldName, Name newName ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#rename(java.lang.String, java.lang.String)
     */
    public void rename( String oldName, String newName ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#unbind(javax.naming.Name)
     */
    public void unbind( Name name ) throws NamingException
    {
    }

    /**
     * No-op.
     * @see javax.naming.Context#unbind(java.lang.String)
     */
    public void unbind( String name ) throws NamingException
    {
    }
}
