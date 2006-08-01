/*
 * Edit Find & Replace functionality
 */

var SearchReplace = new Object();

SearchReplace.editReplace = function(form, dataField)
{
  if( !form ) { return; }
  var findText    = form.findText.value; if( findText == "") return;
  var replaceText = form.replaceText.value;
  var isRegExp    = form.regExp.checked;
  var reGlobal    = ((form.global.checked) ? "g" : "") ;
  var reMatchCase = ((form.matchCase.checked) ? "" : "i") ;
  var data = dataField.value;

  if( !isRegExp ) /* escape all special re characters */
  {
    var re = new RegExp( "([\.\*\\\?\+\[\^\$])", "gi");
    findText = findText.replace(re,"\\$1");
  }
  
  var re = new RegExp(findText, reGlobal+reMatchCase+"m"); //multiline
  if( !re.exec(data) )
  {
    alert("No match found!");
    return(true);
  } 
    
  data = data.replace(re,replaceText);  

  form.undoMemory.value = dataField.value; 
  var undoButton = document.getElementById("undoHideOrShow"); //!! to elements now
  undoButton.style.visibility = "visible";
  dataField.value = data;    
  if( dataField.onchange ) dataField.onchange();
  
  return(true);
}


SearchReplace.editUndo = function(form, dataField)
{
  var undoButton = document.getElementById("undoHideOrShow");
  if( undoButton.style.visibility == "hidden") return(true);
  undoButton.style.visibility = "hidden";
  dataField.value = form.undoMemory.value;
  if( dataField.onchange ) dataField.onchange();
  form.undoMemory.value = ""; 
  return(true);
}
