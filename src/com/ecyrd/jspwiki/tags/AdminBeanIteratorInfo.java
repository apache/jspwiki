package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 *  Just provides iteration support for AdminBeanIteratorTag
 *  
 *  @author jalkanen
 *  @since 2.6.
 */
public class AdminBeanIteratorInfo extends TagExtraInfo
{
    public VariableInfo[] getVariableInfo(TagData data)
    {
        VariableInfo var[] = { new VariableInfo( data.getAttributeString("id"),
                                                 "com.ecyrd.jspwiki.ui.admin.AdminBean",
                                                 true,
                                                 VariableInfo.NESTED )
        };

        return var;
        
    }
}

