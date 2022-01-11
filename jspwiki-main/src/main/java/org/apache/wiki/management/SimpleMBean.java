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
package org.apache.wiki.management;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 *  A simple MBean which does not require an interface class unlike
 *  the StandardMBean class.  The methods are exposed through a method
 *  call, which in turn then calls the methods using the Reflection API.
 *  <p>
 *  This class is similar to the javax.management.StandardMBean, but it does
 *  require the API interface to be declared, so it's simpler.  It's not as
 *  powerful, but it does not require you to declare two classes (and keep
 *  them in sync).
 *
 *  @since  2.6
 */
// FIXME: This class should really use Annotations instead of a method call.
// FIXME: Exception handling is not probably according to spec...
public abstract class SimpleMBean implements DynamicMBean {

	private static final Logger LOG = LogManager.getLogger( SimpleMBean.class );
    protected MBeanInfo m_beanInfo;

    private static Method findGetterSetter(final Class<?> clazz, final String name, final Class<?> parm )
    {
        try
        { 
            final Class<?>[] params = { parm };
            final Class<?>[] emptyparms = {};

            return clazz.getDeclaredMethod( name, parm != null ? params : emptyparms );
        }
        catch( final Exception e )
        {
            // There's nothing to do, really - we just return a null.
        }

        return null;
    }

    /**
     *  Create a new SimpleMBean
     *
     *  @throws NotCompliantMBeanException if an error occurs registering the MBean.
     */
    protected SimpleMBean() throws NotCompliantMBeanException
    {
        //
        //  Create attributes
        //
        final String[] attlist = getAttributeNames();
        MBeanAttributeInfo[] attributes = null;

        if( attlist != null )
        {
            attributes = new MBeanAttributeInfo[attlist.length];

            for( int i = 0; i < attlist.length; i++ )
            {
                String name = attlist[i];
                name = StringUtils.capitalize( name );
                Method getter = findGetterSetter( getClass(), "get"+name, null );

                if( getter == null ) getter = findGetterSetter( getClass(), "is"+name, null );

                Method setter = null;

                if( getter != null )
                {
                    setter = findGetterSetter( getClass(), "set"+name, getter.getReturnType() );
                }

                //
                //  Check, if there's a description available
                //
                final Method descriptor = findGetterSetter( getClass(), "get"+name+"Description", null );
                String description = "";

                if( descriptor != null )
                {
                    try
                    {
                        description = (String) descriptor.invoke( this, (Object[])null );
                    }
                    catch( final Exception e )
                    {
                        description="Exception: "+e.getMessage();
                    }
                }

                final MBeanAttributeInfo info;
                try
                {
                    info = new MBeanAttributeInfo( attlist[i], description, getter, setter );
                }
                catch (final IntrospectionException e)
                {
                    throw new NotCompliantMBeanException( e.getMessage() );
                }

                attributes[i] = info;
            }
        }

        //
        //  Create operations.
        //
        final String[] oplist = getMethodNames();
        final MBeanOperationInfo[] operations = new MBeanOperationInfo[oplist.length];

        final Method[] methods = getClass().getMethods();

        for( int i = 0; i < oplist.length; i++ )
        {
            Method method = null;

            for( final Method value : methods ) {
                if ( value.getName().equals( oplist[ i ] ) ) {
                    method = value;
                }
            }

            if( method == null )
            {
                throw new NotCompliantMBeanException("Class declares method "+oplist[i]+", yet does not implement it!");
            }

            final MBeanOperationInfo info = new MBeanOperationInfo( method.getName(), method );

            operations[i] = info;
        }

        //
        //  Create the actual BeanInfo instance.
        //
        final MBeanConstructorInfo[] constructors = null;
        final MBeanNotificationInfo[] notifications = null;

        m_beanInfo = new MBeanInfo( getClass().getName(),
                                    getDescription(),
                                    attributes,
                                    constructors,
                                    operations,
                                    notifications );
    }

    /**
     *  Customization hook: Override this to get a description for your MBean.  By default,
     *  this is an empty string.
     *
     *  @return A description for the MBean.
     */
    protected String getDescription()
    {
        return "";
    }

    /**
     *  Gets an attribute using reflection from the MBean.
     *
     *  @param name Name of the attribute to find.
     *  @return The value returned by the corresponding getXXX() call
     *  @throws AttributeNotFoundException If there is not such attribute
     *  @throws MBeanException
     *  @throws ReflectionException
     */
    @Override
    public Object getAttribute(final String name)
        throws AttributeNotFoundException, MBeanException, ReflectionException
    {
        final Method m;
        Object res = null;
        try {
            final String mname = "get"+StringUtils.capitalize( name );
            m = findGetterSetter( getClass(), mname, null );

            if( m == null ) throw new AttributeNotFoundException( name );
            res = m.invoke( this, (Object[])null );
        } catch (final SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
        	LOG.error( e.getMessage(), e );
        }

        return res;
    }

    /**
     *  Gets multiple attributes at the same time.
     *
     *  @param arg0 The attribute names to get
     *  @return A list of attributes
     */
    @Override
    public AttributeList getAttributes(final String[] arg0) {
        final AttributeList list = new AttributeList();
        for( final String s : arg0 ) {
            try {
                list.add( new Attribute( s, getAttribute( s ) ) );
            } catch ( final AttributeNotFoundException | MBeanException | ReflectionException e ) {
                LOG.error( e.getMessage(), e );
            }
        }

        return list;
    }

    /**
     *  Return the MBeanInfo structure.
     *
     *  @return the MBeanInfo
     */
    @Override
    public MBeanInfo getMBeanInfo()
    {
        return m_beanInfo;
    }

    /**
     *  Invokes a particular method.
     *
     *  @param arg0 Method name
     *  @param arg1 A list of arguments for the invocation
     */
    @Override
    public Object invoke(final String arg0, final Object[] arg1, final String[] arg2)
        throws MBeanException, ReflectionException
    {
        final Method[] methods = getClass().getMethods();

        for( final Method method : methods ) {
            if( method.getName().equals( arg0 ) ) {
                try {
                    return method.invoke( this, arg1 );
                } catch ( final IllegalArgumentException e ) {
                    throw new ReflectionException( e, "Wrong arguments" );
                } catch ( final IllegalAccessException e ) {
                    throw new ReflectionException( e, "No access" );
                } catch ( final InvocationTargetException e ) {
                    throw new ReflectionException( e, "Wrong target" );
                }
            }
        }

        throw new ReflectionException(null, "There is no such method "+arg0); 
    }

    @Override
    public void setAttribute(final Attribute attr)
        throws AttributeNotFoundException,
               InvalidAttributeValueException,
               MBeanException,
               ReflectionException
    {
        final Method m;

        final String mname = "set"+StringUtils.capitalize( attr.getName() );
        m = findGetterSetter( getClass(), mname, attr.getValue().getClass() );

        if( m == null ) throw new AttributeNotFoundException( attr.getName() );

        final Object[] args = { attr.getValue() };

        try
        {
            m.invoke( this, args );
        }
        catch (final IllegalArgumentException e)
        {
            throw new InvalidAttributeValueException( "Faulty argument: "+e.getMessage() );
        }
        catch (final IllegalAccessException e)
        {
            throw new ReflectionException( e, "Cannot access attribute "+e.getMessage() );
        }
        catch (final InvocationTargetException e)
        {
            throw new ReflectionException( e, "Cannot invoke attribute "+e.getMessage() );
        }
    }

    @Override
    public AttributeList setAttributes( final AttributeList arg0 ) {
        final AttributeList result = new AttributeList();
        for( final Object o : arg0 ) {
            final Attribute attr = ( Attribute ) o;

            //
            //  Attempt to set the attribute.  If it succeeds (no exception),
            //  then we just add it to the list of successfull sets.
            //
            try {
                setAttribute( attr );
                result.add( attr );
            } catch ( final AttributeNotFoundException | InvalidAttributeValueException | MBeanException | ReflectionException e ) {
                LOG.error( e.getMessage(), e );
            }
        }

        return result;
    }
    /**
     *  This method must return a list of attributes which are
     *  exposed by the SimpleMBean.  If there's a getXXX() method
     *  available, it'll be exposed as a getter, and if there's a
     *  setXXX() method available, it'll be exposed as a setter.
     *  For example:
     *  <pre>
     *     public void setFoo( String foo ) ...
     *     public String getFoo() ...
     *
     *     public String[] getAttributeNames()
     *     {
     *         String[] attrs = { "foo" };
     *
     *         return attrs;
     *     }
     *  </pre>
     *  Also, methods starting with "is" are also recognized as getters
     *  (e.g. <code>public boolean isFoo()</code>.)
     *
     *  @return An array of attribute names that can be get and optionally set.
     */
    public abstract String[] getAttributeNames();

    /**
     *  This method must return a list of operations which
     *  are to be exposed by the SimpleMBean.  Note that using overloaded
     *  method names is not supported - only one will be exposed as a JMX method
     *  at random.
     *
     *  @return An array of method names that should be exposed as
     *          JMX operations.
     */
    public abstract String[] getMethodNames();
}
