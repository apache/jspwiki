<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%
  //  Determine the name for the user's favorites page
  WikiContext c = WikiContext.findContext( pageContext );
  String pagename = c.getName();
  String username = null;
 
  username = c.getEngine().getVariable( c, "username" );
  if( username == null ) username = "";

  String myFav = username + "Favorites";
%>

<h3>Your <wiki:Variable var="applicationname" /> profile</h3>

<wiki:TabbedSection defaultTab='<%=request.getParameter("tab")%>'>
  <!-- Tab 1: user preferences -->
  <wiki:Tab id="prefs" title="Preferences">
    <div class="formcontainer">
      <div class="instructions">
        Set your user preferences here. Your choices will be saved in your browser as cookies.
      </div>
    </div>
    <wiki:Permission permission="editPreferences">
      <wiki:UserCheck status="anonymous">
        <div class="formcontainer">
          <div class="instructions">
            <wiki:Messages div="error" topic="prefs" prefix="Could not save prefs: "/>
          </div>
          <form id="setCookie" action="<wiki:Link jsp="UserPreferences.jsp" format="url"><wiki:Param name="tab" value="prefs"/></wiki:Link>" 
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
          <form id="clearCookie" action="<wiki:Link format="url" jsp="UserPreferences.jsp"><wiki:Param name="tab" value="prefs"/></wiki:Link>"
                method="POST" accept-charset="UTF-8">
            <div class="block">
              <div class="description">
                Clears your 'asserted' user name.
              </div>
              <input type="submit" name="ok" value="Clear user name" />
              <input type="hidden" name="action" value="clearAssertedName" />
            </div>
          </form>
          <form action="<wiki:Link format="url" page="<%=myFav%>"/>" method="GET">
             <div class="block">
              <div class="description">
                 You may set your personal Favourites list which will appear in the navigation menu.
              </div>
              <input type="submit" name="ok" value="Edit or create your personal favourites"/>
             </div>
          </form>
        </div>
      </wiki:UserCheck>
    </wiki:Permission>
  </wiki:Tab>
  
  <!-- Tab 2: If user can register, allow edits to profile -->
  <wiki:Tab id="profile" title="Profile">
    <wiki:Permission permission="editProfile">
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
        <div class="instructions">
          <wiki:Messages div="error" topic="profile" prefix="Could not save profile: "/>
        </div>
        <form id="editProfile" action="<wiki:Link jsp="UserPreferences.jsp" format="url"><wiki:Param name="tab" value="profile"/></wiki:Link>" 
              method="POST" accept-charset="UTF-8">
              
          <!-- Login name -->
          <div class="block">
            <label>Login name</label>
            <wiki:UserProfile property="new">
              <input type="text" name="loginname" size="30" value="<wiki:UserProfile property="loginname"/>" />
              <div class="description">
                This is your login id; once set, it cannot be changed.
              </div>
            </wiki:UserProfile>
            <wiki:UserProfile property="exists">
              <p><wiki:UserProfile property="loginname"/></p>
              <div class="description">
                This is your login id.
              </div>
            </wiki:UserProfile>
          </div>
          
          <!-- Password; not displayed if container auth used -->
          <wiki:UserCheck status="setPassword">
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
              Your e-mail address is optional. If you lose your password, you can ask to
              have a new, random password sent to this address.
            </div>
          </div>
          
          <div class="block">
            <wiki:UserCheck status="assertionsAllowed">
              <div class="instructions">
                This wiki automatically remembers you using cookies,
                without requiring additional authentication. To use this
                feature, your browser must accept cookies from this
                website. When you click &#39;save profile,&#39; the cookie
                will be saved by your browser.
              </div>
            </wiki:UserCheck>
            <wiki:UserProfile property="exists">
              <div class="instructions">
                Access control lists or wiki groups containing your identity
                should specify <strong><wiki:UserProfile property="wikiname"/></strong>
                or <strong><wiki:UserProfile property="fullname"/></strong>.
                You are also a member of these roles: 
                <strong><wiki:UserProfile property="roles" /></strong>,
                and the following groups: <strong><wiki:UserProfile property="groups" /></strong>.
                ACLs containing these roles and groups should work, too.
              </div>
            </wiki:UserProfile>
            <div class="instructions">
              <wiki:UserProfile property="exists">
                You created your profile on <wiki:UserProfile property="created"/>,
                and last saved it on <wiki:UserProfile property="modified"/>
              </wiki:UserProfile>
              Click &#39;save profile&#39; to save your wiki profile.
            </div>
            <input type="submit" name="ok" value="Save profile" />
            <input type="hidden" name="action" value="saveProfile" />
          </div>
        </form>
      </div>
    </wiki:Permission> 
  </wiki:Tab>
</wiki:TabbedSection>
