package com.ecyrd.jspwiki.auth.permissions;

import java.security.acl.Permission;

/**
 *  Superclass for all JSPWiki permissions.
 *
 *  @author Janne Jalkanen
 */
public abstract class WikiPermission
    implements Permission
{
    private static WikiPermission c_viewPermission = new ViewPermission();
    private static WikiPermission c_editPermission = new EditPermission();
    private static WikiPermission c_commentPermission = new CommentPermission();
    private static WikiPermission c_deletePermission = new DeletePermission();
    private static WikiPermission c_uploadPermission = new UploadPermission();

    /**
     *  This method should return true, if the this permission implies
     *  also the given permission.  For example "Edit" should always imply "Comment" 
     *  as well, but not vice versa.  "Edit" should also imply itself, since
     *  this method is used to test for permissions.
     */
    public abstract boolean implies( WikiPermission permission );

    public static WikiPermission newInstance( String representation )
        throws IllegalArgumentException
    {
        if( representation.equalsIgnoreCase( "view" ) )
            return c_viewPermission;
        else if( representation.equalsIgnoreCase( "edit" ) )
            return c_editPermission;
        else if( representation.equalsIgnoreCase( "comment" ) )
            return c_commentPermission;
        else if( representation.equalsIgnoreCase( "delete" ) )
            return c_deletePermission;
        else if( representation.equalsIgnoreCase( "upload") )
            return c_uploadPermission;
        
        throw new IllegalArgumentException("No such permission: "+representation);
    }
}
