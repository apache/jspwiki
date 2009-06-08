A key objective of JSPWiki 3.0 is to provide a more flexible and expressive system
for modeling activities performed by users. As you know, we've chosen to use
Stripes as our view controller layer. Stripes models user activities as JavaBean
methods called "actions."

Actions can be triggered by HTTP GETs or POSTs, and also by JavaScript. One of my
bigger tasks for 3.0 has been to recast the existing JSPs (the view layer) so that
they use ActionBeans. That's meant refactoring most of the scriptlet code in top-level
JSPs into Stripes ActionBeans. For example, all of the code in the top-level Wiki.jsp
that parses the "page" parameter and resolves special pages has been moved into the
class ViewActionBean, in the "handler event method" view(). We also have helper
annotations (for example: @HandlerPermission) that indicates to Stripes what the
access control rules for the event: for example, the user must possess the "view"
PagePermission.

But the 3.0 view controller overhaul isn't just about refactoring the JSPs. The tricky
part, for me, has been how to do the JavaScript parts. I have always felt that the
technique of funnelling AJAX/JSON calls through a JavaScript bridge, while necessary
in 2.x, was not as elegant as we could achieve with Stripes. And now, after having
done a little fooling around, I now know how to proceed. I'd like to enlist help
in the refactoring.

For JSPWiki 3.0, the primary change will be to refactor our JavaScript code so that
AJAX and JSON code call ActionBeans directly rather than an intermediate bridge.
I do not know how to do all of the necessary refactoring, so hence this note. The
longer-term objective will be to rip out the JSON global bridge, and the
JSONRPCManager. If we do things right, they will no longer be needed.

To give you an example of how this works, I wrote up a sample ActionBean, JSP
and JavaScript snippet that shows how these three components interact.

Here's a sample AjaxActionBean, which contains a sample event handler called "ajax"
that simply sends back the current date and time in a <p> element. It's meant to
be triggered by an AJAX call on a web page.

Experiment.jsp (html/head/body elements omitted):

    <h1>Experimental AJAX page</h1>
    <div id="experiment">
      <p>This is a test</p>
    </div>
    <form action="localhost:8080/nowhere" id="form" method="post">
      <div>
        <input type="button" name="refresh" id="refresh" value="Refresh"
          onclick="Experiment.refresh();" />
      </div>
    </form>

When the "Refresh" button is pressed, the Experiment.refresh() method executes.
This will use the Mootools Ajax() function to call the server. I have, just to
make it clear that it's the JavaScript that does the work, supplied a dummy
"localhost:8080/nowhere" action URL in the HTML. The response will replace the
<div> with the id "experiment" with the current date and time. Here's the
JavaScript that implements the refresh. 

var Experiment = {
    refresh: function(){
  		new Ajax('/JSPWiki/Ajax.action', {
			  postBody: 'ajax=',
			  update: 'experiment',
			  method: 'post',
		  }).request();
    }
}

As you can see, what Experiment.refresh() does is wire up an AJAX call to
"Ajax.action", the URL for the AjaxActionBean. The post body "ajax=" supplies
the parameter "ajax" with no value. This is how you tell Stripes the event that
the ActionBean should execute, in this case the "ajax" event that supplies the
response. Now, here's the ActionBean that implements the server-side logic:

public class AjaxActionBean extends AbstractActionBean
{
    ...

    @HandlesEvent( "ajax" )
    public Resolution ajax()
    {
        StreamingResolution s = new StreamingResolution( "text/xml" ) {
            @Override
            protected void stream( HttpServletResponse response ) throws Exception
            {
                response.getWriter().write( "<p>" + new Date() + "</p>" );
            }

        };
        return s;
    }
}

This ActionBean has an events named "ajax", is what implements the refresh logic.
All it does is create an XML island with the time/date and return it to the browser.

Behind the scenes, here's the sequence of events:
1) User navigates to Experiment.jsp. The browser loads our experiment.js JavaScript
   library along with Mootools.
2) The user presses the refresh button.
3) The Experiment.refresh() method initializes an AJAX request to the server.
4) The StripesFilter intercepts the request. Seeing that it has the URL Ajax.action,
   it finds the matching ActionBean AjaxActionBean class and creates a new instance.
5) StripesFilter parses the parameter "ajax" with no value, and matches it to the
   @HandlerEvent "ajax". This is the event it will execute.
6) Method ajax() is called, and it returns the XML island to the browser.
7) Experiment.refresh() replaces the contents of <div id='experiment'>.

Already this is pretty great: no intermediate bridge, and a nice clean server-side
implementation.

What else can we do? We can, for example, pass additional parameters to AjaxActionBean.
Suppose we want to pass a "page" parameter to the ActionBean so that our ajax()
method can provide different responses for each WikiPage. In the JavaScript
refresh() method's we'd supply a postBody that looks like this:

   postBody: 'ajax=&page=Main'.

How can we get the ActionBean ajax() method to use this value? Very easily! To get
Stripes to use the new parameter, just add JavaBean property getter/setter methods
to AjaxActionBean. Example:

private WikiPage m_page = null;
public void setPage( WikiPage page ) { m_page = page; }
public WikiPage getPage() { return m_page }

That is all that needs to be done. Stripes will automatically bind the page parameter
passed in the POST to the m_page field. Our special WikiPageTypeConverter is what
causes the String "page" to be converted into a WikiPage. It's all completely automatic.
In addition to WikiPage, You can use Stripes' automatic parameter type conversion
for Date, boolean, int, long, Principal, Group and other types. All that needs to be
done is to declare the field and getter/setter methods using the desired target type.

Pretty cool, huh? Now, what about security... how do we implement that? That's easy
too. To secure particular event methods so that access is restricted to users who
possess the correct privileges, you add a special Annotation called @HandlerPermission
to the event handler method. Here's how would lock down the ajax() method.

    @HandlerPermission( permissionClass = PagePermission.class,
        target = "${page.path}", actions = PagePermission.VIEW_ACTION )
    @HandlesEvent( "ajax" )
    public Resolution ajax()
    {
       ...
    }
    
In plain English, this @HandlerPermission says, "anybody who wants to call the
ajax() event needs to possess a PagePermission with the 'view' privilege for
the current page's path." The annotation knows what the page is because of the
EL variable expression ${page.path}. The variable ${page} denotes the WikiPage
returned by our (new!) getPage() method. That method value, in turn, was set
when Stripes parsed the parameters supplied in the AJAX call. And the "path"
is the full path to the WikiPage as returned by WikiPage.getPath(). Thus, if the
JSON POST URL passed the body "ajax=&page=Main", the required permission would
be PagePermission "Main:Main","view".

Accessing the HTTP request and WikiEngine
-----------------------------------------
Lastly, you might be thinking: Andrew, this is way cool. But your example is
kind of silly. How do we get access to the WikiContext, the HTTP request,
and WikiEngine -- stuff that a real event handler might need?

This, too, is easy. Just call the ActionBean's getContext() superclass method,
which returns the Stripes ActionBeanContext. Our implementation is called
WikiActionBeanContext, and it's also a WikiContext!. All of the usual WikiContext
methods are there, for example getEngine(). This gives ActionBean authors all
the tools they need to write complex handler methods.

Summary
-------
As we migrate more of the JSP layer to Stripes, we will need to migrate some
of the JavaScript AJAX code too. Here's what needs to be done:

- JSP scriptlet code moves into ActionBean event handler methods.
- AJAX calls should call the ActionBean URL directly: e.g., /JSPWiki/Ajax.action?ajax=, /JSPWiki.SearchAction?search=&query=Test
- Event methods that requiring parameters need only implement getter/setters in the ActionBean. Fields, return and paramter types should be declared in the desired target type; Stripes will take care of the parsing and conversion. 
- Event methods can be secured with the @HandlerPermission annotation
- Event methods needing access to the WikiContext can get it via the ActionBean's getContext() method
