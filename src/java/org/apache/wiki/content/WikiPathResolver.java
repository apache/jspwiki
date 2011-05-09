package org.apache.wiki.content;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.wiki.content.jcr.JCRWikiPage;
import org.apache.wiki.providers.ProviderException;

/**
 * <p>
 * Provides methods for looking up, resolving and canonicalizing correct
 * WikiNames based on the contents of the JCR. For example, a WikiPage originally
 * saved to the JCR with the name "Main:TestPage" can also be referred to by
 * WikiPaths "main:Testpage" or "Main:testPage". However, the canonical name is
 * "Main:TestPage"; this is the one that should always be used when a canonical
 * name is needed.
 * </p>
 * <p>
 * WikiPathResolver also provides utility methods for caching mappings between
 * WikiPaths and JCR Node UUIDs, which is used by {@link ReferenceManager}.
 * </p>
 * <p>
 * WikiPathResolver is a singleton instance per ContentManager. The current
 * instance for a given ContentManager can be obtained by calling
 * {@link #getInstance(ContentManager)}.
 * </p>
 */
public class WikiPathResolver
{
    public static final PathRoot[] PATH_ROOTS = new PathRoot[] { PathRoot.NOT_CREATED, PathRoot.PAGES };

    private Node[] m_searchNodes = null;

    /**
     * A branch of the JCR repository where pages are stored.
     */
    public static class PathRoot
    {
        /**
         * JCR branch for pages that have been saved, or are newly created but
         * not yet saved.
         */
        public static final PathRoot PAGES = new PathRoot( ContentManager.JCR_PAGES_PATH );

        /**
         * JCR branch for pages that are referred to by other pages, but have
         * not yet been created. These pages are essentially "stubs" and contain
         * only a limited subset of page attributes, such as
         * {@link ReferenceManager#PROPERTY_REFERRED_BY}, the UUID, and a few
         * others. The JCR node hierarchy mirrors the {@link #PAGES} path root.
         */
        public static final PathRoot NOT_CREATED = new PathRoot( ReferenceManager.NOT_CREATED + "/" );

        private final String m_path;

        /**
         * Returns the JCR path prefix; will always start and end with a slash
         * (/);
         * 
         * @return the JCR path prefix.
         */
        public String path()
        {
            return m_path;
        }

        /**
         * Creates a new page PathRoot object.
         * 
         * @param jcrPath the path prefix; will always start and end with a
         *            slash (/);
         */
        private PathRoot( String jcrPath )
        {
            m_path = jcrPath;
        }
    }

    private static final Map<ContentManager, WikiPathResolver> c_resolvers = new HashMap<ContentManager, WikiPathResolver>();

    private final ContentManager m_cm;

    /**
     * Private constructor to prevent direct instantiation.
     * 
     * @param contentManager the ContentManager used to look up JCR nodes.
     */
    private WikiPathResolver( ContentManager contentManager )
    {
        super();
        m_cm = contentManager;
    }

    private final Map<String, WikiPath> m_uuids = new HashMap<String, WikiPath>();

    private final Map<WikiPath, String> m_paths = new HashMap<WikiPath, String>();

    private final Map<String, WikiPath> m_canonicalPaths = new HashMap<String, WikiPath>();

    /**
     * Returns the WikiPathResolver for the ContentManager, lazily creating one
     * if needed.
     * 
     * @param contentManager the ContentManager
     * @return the instantiated WikiPathResolver
     */
    public static WikiPathResolver getInstance( ContentManager contentManager )
    {
        WikiPathResolver cache = c_resolvers.get( contentManager );
        if( cache == null )
        {
            cache = new WikiPathResolver( contentManager );
            c_resolvers.put( contentManager, cache );
        }
        return cache;
    }

    /**
     * Constructs and returns the JCR path for a given WikiPath in a
     * {@link PathRoot} subtree. All path components are converted to lower
     * case.
     * 
     * @param path the WikiPath
     * @param foundry the JCR subtree containing the nodes, for example
     *            {@link PathRoot#PAGES} or {@link PathRoot#NOT_CREATED}
     * @return a full JCR path. The WikiPath's space and page name will be
     *         converted to lower case.
     */
    public static String getJCRPath( WikiPath path, PathRoot foundry )
    {
        String spaceName = path.getSpace().toLowerCase();
        String spacePath = path.getPath().toLowerCase();
        return foundry.path() + spaceName + "/" + spacePath;
    }

    /**
     * Flushes the resolver's cache.
     */
    public synchronized void clear()
    {
        m_canonicalPaths.clear();
        m_paths.clear();
        m_uuids.clear();
    }

    /**
     * Adds a single WikiPath/UUID to the cache.
     * 
     * @param path the WikiPath
     * @param uuid the UUID
     * @throws RepositoryException
     */
    protected synchronized void add( WikiPath path, String uuid ) throws RepositoryException
    {
        WikiPath canonicalPath = canonicalizeFromJCRPath( path.getSpace() + "/" + path.getPath() );
        m_paths.put( canonicalPath, uuid );
        m_uuids.put( uuid, canonicalPath );
    }

    /**
     * Removes a single WikiPath from the cache.
     * 
     * @param path the WikiPath
     */
    protected synchronized void remove( WikiPath path )
    {
        if( m_paths.containsKey( path ) )
        {
            String uuid = m_paths.get( path );
            m_canonicalPaths.remove( path );
            m_paths.remove( path );
            m_uuids.remove( uuid );
        }
    }

    /**
     * Extracts a WikiPath from a full JCR path, which must start with the path
     * to a supplied {@link PathRoot} subtree, for example
     * {@link PathRoot#PAGES}. Each path component is constructed by examining
     * each intermediate Node's {@link JCRWikiPage#ATTR_TITLE} property.
     * Resolved WikiPaths are cached.
     * 
     * @param jcrPath the full JCR Path used to get the {@link WikiPath}
     * @param foundry the JCR subtree containing the nodes, for example
     *            {@link PathRoot#PAGES} or {@link PathRoot#NOT_CREATED}
     * @return the {@link WikiPath} for the requested JCR path
     * @throws ProviderException if the backend fails.
     */
    // FIXME: Should be protected - fix once WikiPage moves to content-package
    public WikiPath getWikiPath( String jcrPath, PathRoot foundry ) throws ProviderException
    {
        if( jcrPath.startsWith( foundry.path() ) )
        {
            try
            {
                return canonicalizeFromJCRPath( jcrPath.substring( foundry.path().length() ) );
            }
            catch( RepositoryException e )
            {
                throw new ProviderException( "Could not canonicalize WikiPath: " + jcrPath, e );
            }
        }
        throw new ProviderException( "This is not a valid JSPWiki JCR path: " + jcrPath );
    }

    /**
     * Builds and caches a canonicalized WikiPath with canonical,
     * case-sensitive, names based on a JCR path fragment. Each path component
     * is constructed by examining each intermediate Node's
     * {@link JCRWikiPage#ATTR_TITLE} property. Resolved WikiPaths are cached.
     * 
     * @param rawPath the raw (lowercase) JCR path, stripped of page roots, for
     *            example "main/foobar"
     * @return the resolved WikiPath
     * @throws RepositoryException if any Nodes cannot be retrieved
     */
    protected WikiPath canonicalizeFromJCRPath( String rawPath ) throws RepositoryException
    {
        // Quick check: have we seen this wikipath before?
        WikiPath canonicalPath = m_canonicalPaths.get( rawPath );
        if( canonicalPath != null )
        {
            return canonicalPath;
        }

        // Init search nodes
        if( m_searchNodes == null )
        {
            Node root = m_cm.getCurrentSession().getRootNode();
            m_searchNodes = new Node[] { root.getNode( PathRoot.NOT_CREATED.path() ), root.getNode( PathRoot.PAGES.path() ) };
        }

        // Split path components
        String[] components = rawPath.split( "/" );
        String path = "";
        String jcrPath = "";
        boolean seenSpace = false;
        for( String component : components )
        {
            String title = null;
            jcrPath = jcrPath + component;
            for( Node searchNode : m_searchNodes )
            {
                try
                {
                    if( searchNode.hasNode( jcrPath ) )
                    {
                        Node node = searchNode.getNode( jcrPath );
                        title = node.getProperty( JCRWikiPage.ATTR_TITLE ).getString();
                        break;
                    }
                }
                catch( PathNotFoundException e )
                {
                }
            }
            jcrPath = jcrPath + "/";
            path = path + (title == null ? component : title) + (seenSpace ? "/" : ":");
            seenSpace = true;
        }
        if( path.endsWith( "/" ) )
            path = path.substring( 0, path.length() - 1 );
        canonicalPath = WikiPath.valueOf( path );
        m_canonicalPaths.put( rawPath, canonicalPath );
        return canonicalPath;
    }

    /**
     * Looks up and retrieves a WikiPage by UUID and returns the WikiPath,
     * regardless of which branch the page is in, for example the "pages" or
     * "uncreated" branches.
     * 
     * @param uuid the UUID of the {@link Node}
     * @throws RepositoryException if the back-end JCR throws any other
     *             exception
     */
    protected WikiPath getByUUID( String uuid ) throws RepositoryException
    {
        if( uuid == null )
        {
            throw new IllegalArgumentException( "null UUID given to getByUUID()" );
        }

        // Return the path if we've stashed it already
        WikiPath path = m_uuids.get( uuid );
        if( path != null )
        {
            return path;
        }

        // Construct a new WikiPath based on Node's location
        Node node = m_cm.getCurrentSession().getNodeByUUID( uuid );
        String jcrPath = node.getPath();
        for( PathRoot pathRoot : PATH_ROOTS )
        {
            String prefix = pathRoot.path();
            if( jcrPath.startsWith( prefix ) )
            {
                try
                {
                    return getWikiPath( jcrPath, pathRoot );
                }
                catch( ProviderException e )
                {
                    throw new RepositoryException( "Could not construct WikiPath for " + jcrPath, e );
                }
            }
        }
        throw new RepositoryException( "This is not a valid JSPWiki JCR path: " + jcrPath );
    }

    /**
     * Looks up and retrieves the UUID for the JCR node for a given wiki page,
     * whether or not it has been created. If the page exists, the existing
     * Node's UUID will be returned. If it does not exist, an
     * {@link ItemNotFoundException} will be thrown.
     * 
     * @param path the path
     * @return the {@link Node} UUID
     * @throws ItemNotFoundException if the backend fails
     * @throws ProviderException if the backend fails
     */
    protected String getUUID( WikiPath path ) throws RepositoryException, ItemNotFoundException
    {
        if( path == null )
        {
            throw new IllegalArgumentException( "null WikiPath given to getUUID()" );
        }
        JCRWikiPage page;

        // Return the UUID if we've stashed it already
        String uuid = m_paths.get( path );
        if( uuid != null )
        {
            return uuid;
        }

        // Get UUID of path
        try
        {
            if( m_cm.pageExists( path ) )
            {
                page = m_cm.getPage( path );
                uuid = page.getJCRNode().getUUID();
                add( path, uuid );
                return uuid;
            }
        }

        catch( ProviderException e )
        {
            throw new RepositoryException( "Error getting path " + path.toString() + ".", e );
        }

        catch( PageNotFoundException e )
        {
            throw new RepositoryException( "Bug: could not retrieve " + path.toString()
                                           + " even though ContentManager said it existed.", e );
        }

        throw new ItemNotFoundException( "No saved node for path " + path + " was found." );
    }
}
