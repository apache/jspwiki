Working on the conversion of the JSPWiki website to the ASF CMS,
see https://issues.apache.org/jira/browse/JSPWIKI-727

To build the site, locally:
- install python and some needed dependencies, as described in 
  http://apache.org/dev/cmsref.html#local-build
  
- check/out export the build scripts (https://svn.apache.org/repos/infra/websites/cms/build/) 
  to some directory, for example, let's say we have:
  + $SVN_HOME/site
    ++ trunk                 (from svn, contains cms site files)
    ++ scripts               (svn-exported from the previous URL)

- make sure the markdown processing daemon is up:
  $ export MARKDOWN_SOCKET=`pwd`/scripts/markdown.socket PYTHONPATH=`pwd`
  $ python ./scripts/markdownd.py

- create a new directory in which the generated site files will be created. So, our
  previous example would look like this:
  + $SVN_HOME/site
    ++ trunk                 (from svn, contains cms site files)
    ++ scripts               (svn-exported from the previous URL)
    ++ target-site           (newly created directory, NOT under svn control)
  
- cd into scripts directory and execute: 
  $ ./build_site.pl --source-base ../trunk/ --target-base ../target-site
  
Editing the site online instructions at http://apache.org/dev/cms.html#usage
More info at http://apache.org/dev/cmsref.html#overview