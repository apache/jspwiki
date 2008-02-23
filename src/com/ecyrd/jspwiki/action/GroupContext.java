package com.ecyrd.jspwiki.action;

import com.ecyrd.jspwiki.auth.authorize.Group;

/**
 * Represents an ActionBean that acts on a Group.
 * @author Andrew Jaquith
 *
 */
public interface GroupContext
{

    public abstract Group getGroup();

    public abstract void setGroup(Group group);

}
