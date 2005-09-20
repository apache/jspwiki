/*
 *  Chooses a suitable stylesheet based on browser.
 */
    var IE4 = (document.all && !document.getElementById) ? true : false;
    var NS4 = (document.layers) ? true : false;
    var IE5 = (document.all && document.getElementById) ? true : false;
    var NS6 = (document.getElementById && !document.all) ? true : false;
    var IE  = IE4 || IE5;
    var NS  = NS4 || NS6;
    var Mac = (navigator.platform.indexOf("Mac") == -1) ? false : true;

    var sheet = "";

    if( NS4 )
    {
        sheet = "jspwiki_ns.css";
    }
    else if( Mac )
    {
        sheet = "jspwiki_mac.css";
    }
    else if( IE )
    {
        sheet = "jspwiki_ie.css";
    }

    if( sheet != "" )
    {
        document.write("<link rel=\"stylesheet\" href=\"<wiki:BaseURL/>templates/<wiki:TemplateDir />/"+sheet+"\" />");
    }
