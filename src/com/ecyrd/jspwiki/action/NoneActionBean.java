package com.ecyrd.jspwiki.action;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Represents a dummy WikiContext that doesn't bind to a URL, and doesn't
 * contain any embedded logic. When the NoneActionBean class is passed as a
 * parameter to {@link WikiActionBeanContext#getURL(Class, String)}, for
 * example, the resulting URL does not prepend anything before the page.
 * 
 * @author Andrew Jaquith
 * 
 */
@WikiRequestContext("none")
public class NoneActionBean extends WikiContext
{
}
