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
package org.apache.wiki.auth.acl;

import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.auth.acl.adv.RuleNode;
import org.apache.wiki.util.Synchronizer;

import java.io.Serializable;
import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;


/**
 * JSPWiki implementation of an Access Control List.
 *
 * @since 2.3
 */
public class AclImpl implements Acl, Serializable {

    /**
     * A lock used to ensure thread safety when accessing shared resources.
     * This lock provides more flexibility and capabilities than the intrinsic locking mechanism,
     * such as the ability to attempt to acquire a lock with a timeout, or to interrupt a thread
     * waiting to acquire a lock.
     *
     * @see java.util.concurrent.locks.ReentrantLock
     */
    private final ReentrantLock lock = new ReentrantLock();

    private static final long serialVersionUID = 1L;
    private final List< AclEntry > m_entries = new CopyOnWriteArrayList<>();
    private final List< AdvancedRule > m_rules = new CopyOnWriteArrayList<>();

    /** An advanced rule and the permission it grants to the users that satisfy it. */
    private static final class AdvancedRule implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Permission permission;
        private final RuleNode rule;

        private AdvancedRule( final Permission permission, final RuleNode rule ) {
            this.permission = permission;
            this.rule = rule;
        }
    }

    /**
     * Adds an advanced rule, which grants a permission to the users that satisfy an expression of roles, groups and users joined by
     * AND, OR and NOT. A rule adds to the entries and to the other rules of the same permission: a user is granted the permission if
     * any of them grants it.
     *
     * @param permission the permission the rule grants.
     * @param rule the expression.
     * @since 3.0.2
     */
    public void addRule( final Permission permission, final RuleNode rule ) {
        // The same page is parsed again each time it is rendered without the cache, and that must not add its rules again
        Synchronizer.synchronize( lock, () -> {
            for( final AdvancedRule r : m_rules ) {
                if( r.permission.equals( permission ) && r.rule.toString().equals( rule.toString() ) ) {
                    return;
                }
            }
            m_rules.add( new AdvancedRule( permission, rule ) );
        } );
    }

    /**
     * Replaces the names of some principals in the advanced rules with another principal, as when a user renames a profile.
     *
     * @param oldPrincipals the principals to replace.
     * @param newPrincipal the principal that takes their place.
     * @return {@code true} if any rule was changed.
     * @since 3.0.2
     */
    public boolean renameInRules( final Principal[] oldPrincipals, final Principal newPrincipal ) {
        final Set< String > oldNames = new HashSet<>();
        for( final Principal p : oldPrincipals ) {
            oldNames.add( p.getName() );
        }
        return Synchronizer.synchronize( lock, () -> {
            boolean changed = false;
            for( int i = 0; i < m_rules.size(); i++ ) {
                final AdvancedRule r = m_rules.get( i );
                final RuleNode renamed = r.rule.rename( oldNames, newPrincipal );
                if( renamed != r.rule ) {
                    m_rules.set( i, new AdvancedRule( r.permission, renamed ) );
                    changed = true;
                }
            }
            return changed;
        } );
    }

    /**
     * Returns the advanced rules that grant a permission, which are the ones of that permission and of the permissions that imply it.
     *
     * @param permission the permission asked for.
     * @return the expressions of the rules, in the order they were added.
     * @since 3.0.2
     */
    public List< RuleNode > findRules( final Permission permission ) {
        final List< RuleNode > rules = new ArrayList<>();
        for( final AdvancedRule r : m_rules ) {
            if( r.permission.implies( permission ) ) {
                rules.add( r.rule );
            }
        }
        return rules;
    }

    /**
     * Returns the permissions that advanced rules grant, with the expressions of the rules, in the order they were added.
     *
     * @return one entry for each rule.
     * @since 3.0.2
     */
    public List< Map.Entry< Permission, RuleNode > > getRules() {
        final List< Map.Entry< Permission, RuleNode > > rules = new ArrayList<>();
        for( final AdvancedRule r : m_rules ) {
            rules.add( Map.entry( r.permission, r.rule ) );
        }
        return rules;
    }

    /** {@inheritDoc} */
    @Override
    public Principal[] findPrincipals( final Permission permission ) {
        final List< Principal > principals = new ArrayList<>();
        final Enumeration< AclEntry > entries = aclEntries();
        while( entries.hasMoreElements() ) {
            final AclEntry entry = entries.nextElement();
            final Enumeration< Permission > permissions = entry.permissions();
            while( permissions.hasMoreElements() ) {
                final Permission perm = permissions.nextElement();
                if( perm.implies( permission ) ) {
                    principals.add( entry.getPrincipal() );
                }
            }
        }
        return principals.toArray( new Principal[0] );
    }

    private boolean hasEntry( final AclEntry entry ) {
        if( entry == null ) {
            return false;
        }

        for( final AclEntry e : m_entries ) {
            final Principal ep = e.getPrincipal();
            final Principal entryp = entry.getPrincipal();

            if( ep == null || entryp == null ) {
                throw new IllegalArgumentException( "Entry is null; check code, please (entry=" + entry + "; e=" + e + ")" );
            }

            if( ep.getName().equals( entryp.getName() ) ) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addEntry( final AclEntry entry ) {
        return Synchronizer.synchronize(lock, () -> {
            if( entry.getPrincipal() == null ) {
                throw new IllegalArgumentException( "Entry principal cannot be null" );
            }

            if( hasEntry( entry ) ) {
                return false;
            }

            m_entries.add( entry );

            return true;
        });
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeEntry( final AclEntry entry ) {
        return Synchronizer.synchronize(lock, () -> m_entries.remove( entry ));
    }

    /** {@inheritDoc} */
    @Override
    public Enumeration< AclEntry > aclEntries() {
        return Collections.enumeration(m_entries);
    }

    /** {@inheritDoc} */
    @Override
    public AclEntry getAclEntry( final Principal principal ) {
        return m_entries.stream().filter(entry -> entry.getPrincipal().getName().equals(principal.getName())).findFirst().orElse(null);

    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return m_entries.isEmpty() && m_rules.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for( final AclEntry entry : m_entries ) {
            final Principal pal = entry.getPrincipal();
            if( pal != null ) {
                sb.append( "  user = " ).append( pal.getName() ).append( ": " );
            } else {
                sb.append( "  user = null: " );
            }
            sb.append( "(" );
            for( final Enumeration< Permission > perms = entry.permissions(); perms.hasMoreElements(); ) {
                final Permission perm = perms.nextElement();
                sb.append( perm.toString() );
            }
            sb.append( ")\n" );
        }
        for( final AdvancedRule r : m_rules ) {
            sb.append( "  rule = " ).append( r.rule ).append( ": (" ).append( r.permission ).append( ")\n" );
        }
        return sb.toString();
    }

}
    
