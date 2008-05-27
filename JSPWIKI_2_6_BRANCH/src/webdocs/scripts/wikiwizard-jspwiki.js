// This file is intended to be used with JSPWiki v2.3.x and later
var ie = (window.navigator.appName == "Microsoft Internet Explorer") ? true : false;

// Call changeWidth when page is loaded.
// http://dean.edwards.name/weblog/2005/09/busted/
document.addEventListener("DOMContentLoaded", changeWidth, null);

function Cancel(var1)
{
    document.editform.cancel.click();
}

function Preview(var1)
{
    document.editform._editedtext.value=var1;
    document.editform.preview.click();
}

function Attach(var1)
{
    window.open('Upload.jsp?page='+var1,'Upload','width=640,height=480,toolbar=1,menubar=1,scrollbars=1,resizable=1,').focus()
}

function Save(var1)
{
    document.editform._editedtext.value=var1;
    document.editform.ok.click();
}

function unloadFunction()
{
	document.WikiWizard.getText();
}

function GetText(var1)
{
	document.editform._editedtext.value=var1;
}

function setTextPane(var1)
{
    document.WikiWizard.setTextPane(document.editform._editedtext.value);
}

function onTabChange()
{
	if ( document.WikiWizard )
    {
		document.WikiWizard.getText();
	}
}

// Browser detection needed for Netscape (others added for possible future need)
function changeWidth() {
	var detect = navigator.userAgent.toLowerCase();
	var OS,browser,version,total,thestring;
	
	if (checkIt('konqueror'))
	{
	    browser = "Konqueror";
	    OS = "Linux";
	}
	else if (checkIt('safari')) browser = "Safari"
	else if (checkIt('omniweb')) browser = "OmniWeb"
	else if (checkIt('opera')) browser = "Opera"
	else if (checkIt('webtv')) browser = "WebTV";
	else if (checkIt('icab')) browser = "iCab"
	else if (checkIt('msie')) browser = "Internet Explorer"
	else if (!checkIt('compatible'))
	{
	    browser = "Netscape Navigator"
	    version = detect.charAt(8);
	}
	else browser = "An unknown browser";
	
	if (!version) version = detect.charAt(place + thestring.length);
	
	if (!OS)
	{
	    if (checkIt('linux')) OS = "Linux";
	    else if (checkIt('x11')) OS = "Unix";
	    else if (checkIt('mac')) OS = "Mac"
	    else if (checkIt('win')) OS = "Windows"
	    else OS = "an unknown operating system";
	}
	
	function checkIt(string)
	{
	    place = detect.indexOf(string) + 1;
	    thestring = string;
	    return place;
	}
	
	var myWidth = 0, myHeight = 0;
	if( typeof( window.innerWidth ) == 'number' )
	{
	    //Non-IE
	    myWidth = window.innerWidth;
	    myHeight = window.innerHeight;
	}
	
	if ( browser == "Netscape Navigator" || 
	     browser == "Opera" )
	{
		document.getElementById("editarea").setAttribute("style","height:" + myHeight * .70 + "px");
		document.getElementById("WikiWizard").setAttribute("height","100%");
	}
}