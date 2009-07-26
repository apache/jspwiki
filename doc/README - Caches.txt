
As of 3.0, JSPWiki uses EhCache (http://ehcache.sourceforge.net/) as the primary
caching solution.  The 2.x caching solution was based around OSCache, but EhCache
provides some good benefits:

* EhCache is distributable, that is, you can tell multiple instances to use
  the same cache.
* EhCache has better configurability, allowing you to configure it in multiple
  ways, including a JMX bean.
* EhCache is simpler to use from a programmer's point of view
* Priha uses EhCache, so there's no need for two caching solutions.
* EhCache 1.6, is in JSPWiki use, approximately 50% faster.

JSPWiki uses the default CachingManager instance, in which it installs several
caches.  You may configure them by installing your own "ehcache.xml" document, or
you can just let JSPWiki use its own configurations.  Some of the caches can
be configured from jspwiki.properties as well to provide backwards compatibility.



CACHES
======

Name:                   jspwiki.rssCache
Default configuration:  500 entries; 
						memory only caching; 
						expiry time 1 hr.

This cache stores RSS fragments as Strings to make it easier to deal with 
RSS-harvesting bots.  If you have a large wiki, you might want to consider
increasing the size of this cache and enable disk persistence.

-------------------------------------------------------------------------------

Name:					jspwiki.renderingCache
Default configuration:	1000 entries
						memory only caching;
						expiry time 24 hrs.
						
This cache stores WikiDocuments so that the RenderingManager can skip the 
expensive parsing part of the pipeline.  Note that the WikiDocuments are
not Serializable, so therefore you CANNOT distribute nor use disk persistence
for this cache.

The cache size is also controlled by the jspwiki.properties -setting
"jspwiki.renderingManager.capacity".  Any EhCache configuration takes precedence
over jspwiki.properties.

-------------------------------------------------------------------------------

Name:					jspwiki.dynamicAttachmentCache
Default configuration:	256 entries
						memory only caching;
						No expiry time.
						
Stores Dynamic Attachments.  In this case, EhCache is just used as a replacement
for a HashMap, but allows for configurability.

-------------------------------------------------------------------------------

