<?xml version="1.0" encoding="UTF-8"?>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="javax.servlet.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.providers.*" %>

<%!
    String message = null;
    String propertyResult = null;
    
	public String setProperty( String propertyfile, String key, String value )
	{
        int idx = 0;
        
		while( (idx < propertyfile.length()) && ((idx = propertyfile.indexOf(key,idx)) != -1) )
		{
            int prevret = propertyfile.lastIndexOf("\n",idx);
            if( prevret != -1 )
            {
                if( propertyfile.charAt(prevret+1) == '#' ) 
                {
                    idx += key.length();
                    continue;
                }
            }
			int eqsign = propertyfile.indexOf("=",idx);
			
			if( eqsign != -1 )
			{
				int ret = propertyfile.indexOf("\n",eqsign);
				
				if( ret == -1 ) ret = propertyfile.length();
				
				propertyfile = TextUtil.replaceString( propertyfile, eqsign+1, ret, value );
                
                return propertyfile;
			}
            // idx += key.length();
		}
		
		return propertyfile;
	}
	
    public void writeProperties( File propertyFile, String contents )
        throws IOException
    {
        byte[] bytes = contents.getBytes("ISO-8859-1");
        OutputStream out = null;
        
        try
        {
            out = new FileOutputStream( propertyFile );
            
            FileUtil.copyContents( new ByteArrayInputStream( bytes ),
                                   out );
        }
        finally
        {
            if( out != null ) out.close();
        }
    }
    
    public File findPropertyFile( ServletContext context )
    {
        String path = context.getRealPath("/");
        
        File f = new File( path, WikiEngine.DEFAULT_PROPERTYFILE );
        
        return f;
    }
    
	public String readProperties( ServletContext context )
		throws IOException
	{
        File f = findPropertyFile( context );
		FileReader in = null;
		String contents = null;
		try
		{
			in = new FileReader( f );
            contents = FileUtil.readContents( in );
		}
		finally
		{
			if( in != null ) in.close();
		}
		
		return contents;
	}
%>

<%
    String propertyString = readProperties( getServletContext() );

    String appname = request.getParameter( "appname" );
    String baseurl = request.getParameter( "baseurl" );
    String dir = request.getParameter( "dir" );
    String logdir = request.getParameter( "logdir" );
    String workdir = request.getParameter( "workdir" );

    if( request.getParameter("submit") != null )
    {
        if( dir.length() == 0 )
        {
            message = "You must define the location where the files are stored!";
        }
        else if( appname.length() == 0 )
        {
            message = "You must define application name";
        }
        else if( workdir.length() == 0 )
        {
            message = "You must define a work directory";
        }
        else if( logdir.length() == 0 )
        {
            message = "You must define a log directory";
        }
        else
        {
            propertyString = setProperty( propertyString, WikiEngine.PROP_APPNAME, appname );
            propertyString = setProperty( propertyString, WikiEngine.PROP_BASEURL, baseurl );
            propertyString = setProperty( propertyString, FileSystemProvider.PROP_PAGEDIR, dir );
            propertyString = setProperty( propertyString, BasicAttachmentProvider.PROP_STORAGEDIR, dir );
            propertyString = setProperty( propertyString, WikiEngine.PROP_WORKDIR, workdir );
            propertyString = setProperty( propertyString, "log4j.appender.FileLog.File", logdir );

            //
            //  Some default settings for the easy setup
            //
            propertyString = setProperty( propertyString, PageManager.PROP_PAGEPROVIDER, "VersioningFileProvider" );
            propertyString = setProperty( propertyString, WikiEngine.PROP_ENCODING, "UTF-8" );
            
            try
            {
                writeProperties( findPropertyFile(getServletContext()), propertyString );
                message = "Your new properties have been saved.  You can see what was written if you scroll down a bit.";
            }
            catch( IOException e )
            {
                message = "Unable to write properties: "+e.getMessage()+
                          ".  Please copy the file below as your jspwiki.properties";
            }
            propertyResult = propertyString;
        }
    }
%>

<%    
    File propertyFile = findPropertyFile( getServletContext() );
    
    Properties props = new Properties();
    props.load( new ByteArrayInputStream(propertyString.getBytes("ISO-8859-1")) );
    
	if( appname == null ) appname = props.getProperty( WikiEngine.PROP_APPNAME, "MyWiki" );
    
	if( baseurl == null ) 
	{
		baseurl = HttpUtils.getRequestURL(request).toString();
		baseurl = baseurl.substring( 0, baseurl.lastIndexOf('/') )+"/";
        
        baseurl = props.getProperty( WikiEngine.PROP_BASEURL, baseurl );
	}
	
	if( dir == null ) dir = props.getProperty( FileSystemProvider.PROP_PAGEDIR, "Please configure me!" );
	
	if( logdir == null ) logdir = props.getProperty( "log4j.appender.FileLog.File", "/tmp/" );

	if( workdir == null ) workdir = props.getProperty( WikiEngine.PROP_WORKDIR, "/tmp/" );
	
	// FIXME: encoding as well.
%>

<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
   <title>JSPWiki configuration helper</title>
   <style>
	   .configopt { margin-top: 1ex;
	   				margin-bottom: 1ex; }
	   #content { width: 60%;
	   		  	padding: 12px;
	   	      	margin-left: auto;
	   	      	margin-right: auto; 
	   	      	background: #eeeeee; }
   </style>
</head>

<body bgcolor="#FFFFFF">

	<div id="content">
	<h1>JSPWiki configuration</h1>
	
	<p>Welcome!  This little JSP page is here to help you do the first difficult stage of JSPWiki
	installation.  If you're seeing this page, you have already installed JSPWiki correctly
	inside your container.</p>
	
	<p>There are now some things that you should configure.  When you press submit, the "jspwiki.properties"
	-file from the distribution will be modified, or if it can't be found, a new one will be created.</p>

	<p>This setup system is really meant for people who just want to be up and running really quickly.
	If you want to integrate JSPWiki with an existing system, I would recommend that you go and edit
	the jspwiki.properties file directly.  You can find a sample config file from 
	<tt>yourwiki/WEB-INF/</tt>.</p>

    <%if(propertyFile!=null) {%>
        <p>(Read these properties from '<%=propertyFile.getAbsolutePath()%>')</p>
    <%}%>

    <%if( message != null ) { %>
    <div style="border:1px inset solid; color: red; padding:4px;">
       <%=message%>
    </div>
    <%}%>

	<form action="Install.jsp" method="post">

	<h3>Mandatory options</h3>
	
	<div class="configopt">
		<b>Application Name</b>: <input type="text" name="appname" size="20" value="<%=appname%>"/><br />
		<i>What should your wiki be called?  Try and make this a relatively short one.</i>
	</div>

	<div class="configopt">
		<b>Base URL</b>: <input type="text" name="baseurl" size="80" value="<%=baseurl%>"/><br />
		<i>Please tell me where your wiki is located.</i>
	</div>

	<div class="configopt">
		<b>File storage</b>: <input type="text" name="dir" size="80" value="<%=dir%>"/><br />
		<i>By default, I will use the VersioningFileProvider that will store files in a particular
		directory on your hard drive.  If you specify a directory that does not exist, I'll
		create one for you.  All attachments will also be put in the same directory.</i>
	</div>

	<div class="configopt">
		<b>Work directory</b>: <input type="text" name="workdir" size="80" value="<%=workdir%>"/><br />
		<i>This is the place where all caches and other runtime stuff is stored.</i>
	</div>

	<h3>Other useful settings</h3>
	
<%--	
	<div class="configopt">
		<b>URL format</b>: <input type="radio" name="urlfmt" value="relative" checked="checked"/>Relative
		<input type="radio" name="urlfmt" value="absolute"/>Absolute<br />
		<i>Would you like to use relative URLs whenever possible (they are shorter)?</i>
	</div>
	
	<div class="configopt">
		<b>Encoding</b>: <input type="radio" name="encoding" value="UTF-8"  checked="checked"/>UTF-8
		<input type="radio" name="encoding" value="ISO-8859-1"/>ISO-8859-1<br />
		<i>UTF-8 is the standard encoding in the internet these days, but ISO-8859-1 (aka Latin1) might
		   be useful for legacy applications.</i>
	</div>
--%>

	<div class="configopt">
		<b>Log files</b>: <input type="text" name="logdir" value="<%=logdir%>" size="80"/><br />
		<i>JSPWiki uses Jakarta Log4j for logging.  Please tell me where the log files should go to?</i>
	</div>

	<div style="width:100px; margin-left:auto;margin-right:auto;margin-top:2ex;">
		<input type="submit" name="submit" value="Configure!" />
	</div>
	
	</form>
    
    <%if(propertyResult != null) {%>
    <hr />
    <h3>Here is your new jspwiki.properties</h3>
    <pre>
<%=propertyResult%>
    </pre>
    <%}%>
	</div>
</body>
</html>