/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
 
/**
 ** Javascript routines to support JSPWiki UserPreferences
 ** since v.2.6.0
 ** uses mootools v1.1
 **/

var WikiPreferences =
{
	onPageLoad: function(){

		window.onbeforeunload = (function(){

			if( $('prefs').getFormElements().some(function(el){
				return (el.getValue() != el.getDefaultValue());  
			}) ) return "prefs.areyousure".localize();

		}).bind(this);
 	},
 	
	savePrefs: function(){
		var prefs = {
			'prefSkin':'SkinName',
			'prefTimeZone':'TimeZone',
			'prefTimeFormat':'DateFormat',
			'prefOrientation':'Orientation',
			'editor':'editor',
			'prefLanguage':'Language',
			'prefSectionEditing':'SectionEditing'
		};
		for(var el in prefs){
			if($(el)) Wiki.prefs.set(prefs[el],$(el).getValue());
		};
	}
}

window.addEvent('load', WikiPreferences.onPageLoad.bind(WikiPreferences) );

// refactor me
var WikiGroup =
{
	MembersID   : "membersfield",
	//GroupTltID  : "grouptemplate",
	GroupID     : "groupfield",
	NewGroupID  : "newgroup",
	GroupInfoID : "groupinfo",
	CancelBtnID : "cancelButton",
	SaveBtnID   : "saveButton",
	CreateBtnID : "createButton",
	DeleteBtnID : "deleteButton",
	groups      : { "(new)": { members:"", groupInfo:"" } },
	cursor      : null,
	isEditOn    : false,
	isCreateOn  : false,

	putGroup: function(group, members, groupInfo, isSelected){
		this.groups[group] = { members: members, groupInfo: groupInfo };

		var g = $("grouptemplate");
			gg = g.clone().removeProperty('id').setHTML(group).inject(g.getParent()).show();

		if(isSelected || !this.cursor) this.onMouseOverGroup(gg);
	} ,

	onMouseOverGroup: function(node){
		if(this.isEditOn) return;
		this.setCursor(node);

		var g = this.groups[ (node.id == this.GroupID) ? "(new)": node.innerHTML ];
		$(this.MembersID).value = g.members;
		$(this.GroupInfoID).innerHTML = g.groupInfo;
	} ,

	setCursor: function(node){
		if(this.cursor) $(this.cursor).removeClass('cursor');
		this.cursor = $(node).addClass('cursor');
	} ,

	//create new group: focus on input field
	onClickNew: function(){
		if(this.isEditOn) return;

		this.isCreateOn = true;
		$(this.MembersID).value = "";
		this.toggle();
	} ,

	//toggle edit status of Group Editor
	toggle: function(){
		this.isEditOn = !this.isEditOn; //toggle

		$(this.MembersID  ).disabled =
		$(this.SaveBtnID  ).disabled =
		$(this.CreateBtnID).disabled =
		$(this.CancelBtnID).disabled = !this.isEditOn;
		var del = $(this.DeleteBtnID);
		if(del) del.disabled = this.isCreateOn || !this.isEditOn;

		if(this.isCreateOn) { $(this.CreateBtnID).toggle(); $(this.SaveBtnID).toggle() };

		var newGrp  = $(this.NewGroupID),
			members = $(this.MembersID);

		if(this.isEditOn){
			members.getParent().addClass("cursor");

			newGrp.disabled = !this.isCreateOn;
			if(this.isCreateOn) { newGrp.focus(); } else { members.focus(); }
		} else {
			members.getParent().removeClass("cursor");

			if(this.isCreateOn){
				this.isCreateOn = false;
				newGrp.value = newGrp.defaultValue;
				members.value = "";
			}
			newGrp.blur();
			members.blur();
			newGrp.disabled = false;
		}
	} ,

	// submit form to create new group
	onSubmitNew: function(form, actionURL){
		var newGrp = $(this.NewGroupID);
		if(newGrp.value == newGrp.defaultValue){
			alert("group.validName".localize());
			newGrp.focus();
		} else this.onSubmit(form, actionURL);
	} ,

	// submit form: fill out actual group and members info
	onSubmit: function(form, actionURL){
		if(! this.cursor) return false;
		var g = (this.cursor.id == this.GroupID) ? $(this.NewGroupID).value: this.cursor.innerHTML;

		/* form.action = actionURL; -- doesn't work in IE */
		form.setAttribute("action", actionURL) ;
		form.group.value = g;
		form.members.value = $(this.MembersID).value;
		form.action.value = "save";

		Wiki.submitOnce(form);
		form.submit();
	}
}