package com.ecyrd.jspwiki.tags;

import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 *  Just provides the TEI data for AttachmentsIteratorTag.
 *
 *  @since 2.0
 */
public class AttachmentsIteratorInfo extends TagExtraInfo
{
    public VariableInfo[] getVariableInfo(TagData data)
    {
        VariableInfo var[] = { new VariableInfo( data.getAttributeString("id"),
                                                 "com.ecyrd.jspwiki.attachment.Attachment",
                                                 true,
                                                 VariableInfo.NESTED )
        };

        return var;
        
    }
}
