package com.ecyrd.jspwiki.filters;

import com.ecyrd.jspwiki.WikiContext;
import java.util.Properties;

/**
 *  Provides a definition for a page filter.  A page filter is a class
 *  that can be used to transform the WikiPage content being saved or
 *  being loaded at any given time.
 *  <p>
 *  Note that the WikiContext.getPage() method always returns the context
 *  in which text is rendered, i.e. the original request.  Thus the content
 *  may actually be different content than what what the wikiContext.getPage()
 *  implies!  This happens often if you are for example including multiple
 *  pages on the same page.
 *  <p>
 *  PageFilters must be thread-safe!  There is only one instance of each PageFilter 
 *  per each WikiEngine invocation.  If you need to store data persistently, use
 *  VariableManager, or WikiContext.
 *
 *  @author Janne Jalkanen
 */
public interface PageFilter
{
    /**
     *  Is called whenever the a new PageFilter is instantiated and
     *  reset.
     */
    public void initialize( Properties properties );

    /**
     *  This method is called whenever a page has been loaded from the provider,
     *  but not yet been sent through the TranslatorReader.  Note that you cannot
     *  do HTML translation here, because TranslatorReader is likely to escape it.
     *
     *  @param wikiContext The current wikicontext.
     *  @param content     WikiMarkup.
     */
    public String preTranslate( WikiContext wikiContext, String content );

    /**
     *  This method is called after a page has been fed through the TranslatorReader,
     *  so anything you are seeing here is translated content.  If you want to
     *  do any of your own WikiMarkup2HTML translation, do it here.
     */
    public String postTranslate( WikiContext wikiContext, String htmlContent );

    /**
     *  This method is called before the page has been saved to the PageProvider.
     */
    public String preSave( WikiContext wikiContext, String content );

    /**
     *  This method is called after the page has been successfully saved.
     *  If the saving fails for any reason, then this method will not
     *  be called.
     *  <p>
     *  Since the result is discarded from this method, this is only useful
     *  for things like counters, etc.
     */
    public void postSave( WikiContext wikiContext, String content );
}
