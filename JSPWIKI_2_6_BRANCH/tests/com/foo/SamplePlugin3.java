package com.foo;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.plugin.*;
import java.util.*;

/**
 *  Implements a simple plugin that just returns its text.
 *  <P>
 *  Parameters: text - text to return.
 *
 *  @author Janne Jalkanen
 */
public class SamplePlugin3
    implements WikiPlugin
{
    public void initialize( WikiEngine engine )
        throws PluginException
    {
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        return (String)params.get("text");
    }

}
