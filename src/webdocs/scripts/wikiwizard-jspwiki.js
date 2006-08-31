// This file is intended to be used with JSPWiki v2.3.x and later
var ie = (window.navigator.appName == "Microsoft Internet Explorer") ? true : false;

function Cancel(var1)
{
    document.editForm.cancel.click();
}

function Preview(var1)
{
    document.editForm._editedtext.value=var1;
    document.editForm.preview.click();
}

function Attach(var1)
{
    window.open('Upload.jsp?page='+var1,'Upload','width=640,height=480,toolbar=1,menubar=1,scrollbars=1,resizable=1,').focus()
}

function Save(var1)
{
    document.editForm._editedtext.value=var1;
    document.editForm.ok.click();
}

function unloadFunction()
{
	document.WikiWizard.getText();
}

function GetText(var1)
{
	document.editForm._editedtext.value=var1;
}

function setTextPane(var1)
{
    document.WikiWizard.setTextPane(document.editForm._editedtext.value);
}

function onTabChange()
{
	if ( document.WikiWizard )
    {
		document.WikiWizard.getText();
	}
}