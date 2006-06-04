package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 *  Just provides the TEI data for EditorIteratorTag.
 *
 *  @author Chuck Smith
 *  @since 2.4.12
 */
public class EditorIteratorInfo extends TagExtraInfo
{
    public VariableInfo[] getVariableInfo(TagData data)
    {
        VariableInfo var[] = { new VariableInfo( data.getAttributeString("id"),
                                                 "com.ecyrd.jspwiki.ui.Editor",
                                                 true,
                                                 VariableInfo.NESTED )
        };

        return var;        
    }

}
