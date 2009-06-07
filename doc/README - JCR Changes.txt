# 
#    JSPWiki - a JSP-based WikiWiki clone.
#
#    Licensed to the Apache Software Foundation (ASF) under one
#    or more contributor license agreements.  See the NOTICE file
#    distributed with this work for additional information
#    regarding copyright ownership.  The ASF licenses this file
#    to you under the Apache License, Version 2.0 (the
#    "License"); you may not use this file except in compliance
#    with the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing,
#    software distributed under the License is distributed on an
#    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#    KIND, either express or implied.  See the License for the
#    specific language governing permissions and limitations
#    under the License.  
#
############################################################################

JSPWiki 3 contains an all-new backend based on the JSR-170 specification.
This document details the changes that this API will cause on the developers.

Please note that since the integration work is not yet complete, some
of the information in this document may not be up to date.

Please see the TODO list in JSPWIKI-421 for information as to the current
state of the migration.

Architectural changes
=====================

Previously, JSPWiki has created a difference between WikiPages and Attachments.
This separation is now gone since everything is now handled through the
ContentManager class.  The wiki markup is separated through its own particular
MIME type.  While this facility is not yet in 3.0, the intent is to turn this
gradually into a "renderer" metaphor, where different content is rendered
through different renderers - for example JSPWikiMarkup is parsed through 
the JSPWikiMarkupParser, then rendered with the XHTMLRenderer.

In addition, WikiPage has become an "active" object, in the sense that direct
manipulation of its attributes will reflect on the repository state.  However,
you will need to call WikiPage.save() in order for these changes to be permanent.

The basic idea is that we will use the rich metadata model allowed by JCR and
attach practically everything which has so far lived in separate subdirectories 
(e.g. in the workDir) as page properties.  For example, references are no longer
cached by ReferenceManager, but they are directly written in the repository
as a property of the WikiPage.  Also, variables manipulated with SET are also
directly written to the repository.

However, the difficult bits of the JCR Repository management are hidden
by the ContentManager class.  The biggest issue is that ContentManager holds
a ThreadLocal reference to JCR Session objects without releasing them which means
that you *need* to clear any changes you've made by calling ContentManager.discardModifications()
in case of failure, or they will stay around when the next modification comes.


Configuration
=============

All jspwiki.properties settings relating to providers are gone. They
are replaced by a single setting "jspwiki.repository".

Each backend will use its own configuration methodology. For Priha
(the JCR implementation we ship with) this is done in a file called
"priha.properties". Please see Priha configuration manual for further
details.


API Changes
===========

PageManager and AttachmentManager are now deprecated and will be gone
soon.

There is a new class, ContentManager, which takes care of all the functionality
of PageManager and AttachmentManager.

WikiProvider interface is gone, as are all different provider classes
which implemented it.

WikiPage has gained a series of new methods for direct manipulation of
the page content.


Versioning
==========

TBD, this has not yet been decided.
 