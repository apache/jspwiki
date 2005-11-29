<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%! 
    Category log = Category.getInstance("JSPWiki"); 
%>

<%
    // Init the errors list
    Set errors;
    if ( session.getAttribute( "errors" ) != null )
    {
       errors = (Set)session.getAttribute( "errors" );
    }
    else
    {
       errors = new HashSet();
       session.setAttribute( "errors", errors );
    }
%>

<h3>Your <wiki:Variable var="applicationname" /> profile</h3>

<!-- Tab definitions -->
<div class="tabmenu">
  <span>
    <a class="activetab" id="menu-prefs" class="activetab" onclick="TabbedSection.onclick('prefs')" >Preferences</a>
  </span>
  <span><a id="menu-profile" onclick="TabbedSection.onclick('profile')" >Profile</a>
  </span>
</div>

<div class="tabs">
  <!-- Tab 1: user preferences -->
  <div id="prefs" class="tab-Prefs">
    <wiki:Permission permission="editPreferences">
      <wiki:UserCheck status="anonymous">
        <div class="formcontainer">
          <div class="instructions">
            Set your user preferences here. Your choices will be saved in your browser as cookies.
          </div>
          <form id="setCookie" action="<wiki:Variable var="baseURL"/>UserPreferences.jsp" 
                method="POST" accept-charset="UTF-8">
            <div class="block">
              <label>Wiki name</label>
              <input type="text" name="assertedName" size="30" value="<wiki:UserProfile property="loginname"/>" />
              <div class="description">
                Your wiki name. If you haven't created a user profile
                yet, you can tell <wiki:Variable var="applicationname" /> 
                who you are by 'asserting' an identity. You wouldn't
                lie to us would you?
              </div>
              <div class="description">
                Note that setting your user name this way isn't a
                particularly trustworthy method of authentication, 
                and the wiki may grant you fewer privileges as a
                result. <a onclick="TabbedSection.onclick('profile')" >
                Create a user profile</a> if you'd prefer a
                traditional username and password, which is more
                secure.
              </div>
              <input type="submit" name="ok" value="Set user name" />
              <input type="hidden" name="action" value="setAssertedName" />
            </div>
          </form>
        </div>
      </wiki:UserCheck>
      
      <!-- Clearing the 'asserted name' cookie -->
      <wiki:UserCheck status="asserted">
        <div class="formcontainer">
          <form id="clearCookie" action="<wiki:Variable var="baseURL"/>UserPreferences.jsp" 
                method="POST" accept-charset="UTF-8">
            <div class="block">
              <div class="description">
                Clears your 'asserted' user name.
              </div>
              <input type="submit" name="ok" value="Clear user name" />
              <input type="hidden" name="action" value="clearAssertedName" />
            </div>
          </form>
        </div>
      </wiki:UserCheck>
    </wiki:Permission>
  </div>
  
  <!-- Tab 2: If user can register, allow edits to profile -->
  <div id="profile" class="tab-Profile" style="display:none;">
    <wiki:Permission permission="registerUser">
      <div class="formcontainer">
        <div class="instructions">
          <wiki:UserProfile property="new">
            Hi! Looks like you haven't set up a wiki profile yet.
            You can do that here. To set up your profile, we need
            to know a little bit about you.
          </wiki:UserProfile>
          <wiki:UserProfile property="exists">
            Edit your wiki profile here.
          </wiki:UserProfile>
        </div>
        <form id="editProfile" action="<wiki:Variable var="baseURL"/>UserPreferences.jsp" 
              method="POST" accept-charset="UTF-8">
              
          <!-- Login name -->
          <div class="block">
            <label>Login name</label>
            
            <wiki:UserCheck status="customAuth">
              <wiki:UserProfile property="new">
                <input type="text" name="loginname" size="30" value="<wiki:UserProfile property="loginname"/>" />
                <div class="description">
                  This is your login id; once set, it cannot be changed.
                  It is only used for authentication, not for page access control.
                </div>
              </wiki:UserProfile>
              <wiki:UserProfile property="exists">
                <p><wiki:UserProfile property="loginname"/></p>
                <div class="description">
                  This is your login id.
                </div>
              </wiki:UserProfile>
            </wiki:UserCheck>
            
            <wiki:UserCheck status="containerAuth">
              <p><wiki:UserProfile property="loginname"/></p>
              <div class="description">
                This is your login id.
              </div>
            </wiki:UserCheck>
          </div>
          
          <!-- Password; not displayed if container auth used -->
          <wiki:UserCheck status="customAuth">
            <div class="block">
              <label>Password</label>
              <input type="password" name="password" size="30" value="" />
              <div class="description">
                Sets your account password. It may not be blank.
              </div>
            </div>
      
            <div class="block">
              <label>Password (re-type)</label>
              <input type="password" name="password2" size="30" value="" />
              <div class="description">
                Type your password again.
              </div>
            </div>
          </wiki:UserCheck>
          
          <!-- Wiki name -->
          <div class="block">
            <label>Wiki name</label>
            <wiki:UserProfile property="new">
              <input type="text" name="wikiname" size="30" value="<wiki:UserProfile property="wikiname"/>" />
              <div class="description">
                This must be a proper WikiName; cannot contain
                spaces or punctuation.
              </div>
            </wiki:UserProfile>
            <wiki:UserProfile property="exists">
              <p><wiki:UserProfile property="wikiname"/></p>
              <div class="description">
                This is your WikiName.
              </div>
            </wiki:UserProfile>
          </div>
          
          <!-- Full name -->
          <div class="block">
            <label>Full name</label>
            <wiki:UserProfile property="new">
              <input type="text" name="fullname" size="30" value="<wiki:UserProfile property="fullname"/>" />
              <div class="description">
                This is your full name.
              </div>
            </wiki:UserProfile>
            <wiki:UserProfile property="exists">
              <p><wiki:UserProfile property="fullname"/></p>
              <div class="description">
                This is your full name.
              </div>
            </wiki:UserProfile>
          </div>
           
          <!-- E-mail -->
          <div class="block">
            <label>E-mail address</label>
            <input type="text" name="email" size="30" value="<wiki:UserProfile property="email"/>" />
            <div class="description">
              Your e-mail address is optional. In the future, it will be used
              by JSPWiki for resetting lost passwords.
            </div>
          </div>
          
          <div class="block">
            <!-- Any errors? -->
            <%
            if ( errors != null && errors.size() > 0 )
            { 
              out.println("<blockquote><p>Could not save profile:<ul>");
              for ( Iterator it = errors.iterator(); it.hasNext(); )
              {
                out.println( "<li>" + it.next().toString() + "</li>" );
              }
              out.println("</ul></p></blockquote>");
            }
            %>
            <div class="instructions">
              Click &#39;save profile&#39; to save your wiki profile.
            </div>
            <wiki:UserCheck status="assertionsAllowed">
              <div class="instructions">
                This wiki automatically remembers you using cookies,
                without requiring additional authentication. To use this
                feature, your browser must accept cookies from this
                website. When you click &#39;save profile,&#39; the cookie
                will be saved by your browser.
              </div>
            </wiki:UserCheck>
            <input type="submit" name="ok" value="Save profile" />
            <input type="hidden" name="action" value="saveProfile" />
          </div>
        </form>
      </div>
    </wiki:Permission> 
  </div>
</div>
<%
    errors.clear();
%>