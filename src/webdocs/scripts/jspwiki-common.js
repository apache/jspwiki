// 004. Zebra tables  DF / May 2004
// %%zebra-table
//
var zebraCnt ;

// called after loading the page
function validateZebraTable()
{
  if (!document.createElement) return;

  // find a <div class="zebra-table"> element
  var divArr = document.getElementsByTagName("div");
  if (! divArr) return; 

  for (var i=0; i<divArr.length; i++) 
  {
    if ( divArr[i].className == "zebra-table" )
    {
      zebraCnt = 0;
      validateZebraTableNode(divArr[i]);   
    } 
  }  

}

function validateZebraTableNode(node)
{
  if ( node.nodeName == "TR") 
  {
     zebraCnt++;
     if (zebraCnt % 2 == 1) node.className = "odd";
  }
  
  if (node.hasChildNodes) 
  {
    for (var i=0; i<node.childNodes.length; i++) 
    { 
      validateZebraTableNode(node.childNodes[i]);
    }
  }

}

// Select skin
function skinSelect(skin)
{
  //var skin = document.forms["skinForm"].skinSelector;
  if (! skin) return; 

  for (var i=0; i<skin.length; i++)
  {
    if ( skin[i].selected )
    {
      document.cookie = "JspWikiSkin=" + skin[i].value + "#skin#" ;
    }
  }
  location.reload(); /* reload page */
}

function runOnLoad()
{ 
  validateZebraTable();
  googleSearchHighlight();
}

window.onload = runOnLoad;
