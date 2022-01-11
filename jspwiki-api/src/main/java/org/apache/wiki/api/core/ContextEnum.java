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
package org.apache.wiki.api.core;


public enum ContextEnum {

    GROUP_DELETE( "deleteGroup", "%uDeleteGroup.jsp?group=%n", null ),
    GROUP_EDIT( "editGroup", "%uEditGroup.jsp?group=%n", "EditGroupContent.jsp" ),
    GROUP_VIEW( "viewGroup", "%uGroup.jsp?group=%n", "GroupContent.jsp" ),

    PAGE_ATTACH( "att", "%uattach/%n", null ),
    PAGE_COMMENT( "comment", "%uComment.jsp?page=%n", "CommentContent.jsp" ),
    PAGE_CONFLICT ( "conflict", "%uPageModified.jsp?page=%n", "ConflictContent.jsp" ),
    PAGE_DELETE( "del", "%uDelete.jsp?page=%n", null ),
    PAGE_DIFF( "diff", "%uDiff.jsp?page=%n", "DiffContent.jsp" ),
    PAGE_EDIT( "edit", "%uEdit.jsp?page=%n", "EditContent.jsp" ),
    PAGE_INFO( "info", "%uPageInfo.jsp?page=%n", "InfoContent.jsp" ),
    PAGE_NONE( "", "%u%n", null ),
    PAGE_PREVIEW( "preview", "%uPreview.jsp?page=%n", "PreviewContent.jsp" ),
    PAGE_RENAME( "rename", "%uRename.jsp?page=%n", "InfoContent.jsp" ),
    PAGE_RSS( "rss", "%urss.jsp", null ),
    PAGE_UPLOAD( "upload", "%uUpload.jsp?page=%n", null ),
    PAGE_VIEW( "view", "%uWiki.jsp?page=%n", "PageContent.jsp" ),

    REDIRECT( "", "%u%n", null ),

    WIKI_ADMIN( "admin", "%uadmin/Admin.jsp", "AdminContent.jsp" ),
    WIKI_CREATE_GROUP( "createGroup", "%uNewGroup.jsp", "NewGroupContent.jsp" ),
    WIKI_ERROR( "error", "%uError.jsp", "DisplayMessage.jsp" ),
    WIKI_FIND( "find", "%uSearch.jsp", "FindContent.jsp" ),
    WIKI_INSTALL( "install", "%uInstall.jsp", null ),
    WIKI_LOGIN( "login", "%uLogin.jsp?redirect=%n", "LoginContent.jsp" ),
    WIKI_LOGOUT( "logout", "%uLogout.jsp", null ),
    WIKI_MESSAGE( "message", "%uMessage.jsp", "DisplayMessage.jsp" ),
    WIKI_PREFS( "prefs", "%uUserPreferences.jsp", "PreferencesContent.jsp" ),
    WIKI_WORKFLOW( "workflow", "%uWorkflow.jsp", "WorkflowContent.jsp" );

    private final String contentTemplate;
    private final String requestContext;
    private final String urlPattern;

    ContextEnum( final String requestContext, final String urlPattern, final String contentTemplate ) {
        this.requestContext = requestContext;
        this.urlPattern = urlPattern;
        this.contentTemplate = contentTemplate;
    }

    public String getContentTemplate() {
        return contentTemplate;
    }

    public String getRequestContext() {
        return requestContext;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

}
