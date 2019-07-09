/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
/*
Class: Wiki.SearchBox
    ...
    Adds/Removes "li.findpages" elements to the dropdown menu.
    These elements are the results of the wiki rpc call, based on the query.


Example:
>    wiki.add("query", Wiki.Findpages, {
>        rpc: function(value, callback){ Wiki.ajaxJsonCall("/search/findPages",[value,"20"], callback },
>        rpc: function(value, callback){ wiki.jsonrpc("search.findPages", [value,20], callback },
>        toUrl: wiki.toUrl
>    });

DOM Structure:
(start code)

      //only ico no full match with query : options to create/clone new page
      li.findpages
          a.createpage[href="/wiki/B"] [Create] B
      li.findpages
          a.createpage[href="/wiki/B&clone=Main]  [Create] B [based on] Main
      //show all rpc results
      li.findpages
          a[href]

(end)
*/
Wiki.Findpages = new Class({

    Binds: ["search", "action"],
    Implements: Events,

    initialize: function(element, options){

        var self = this;

        self.rpc = options.rpc;
        self.toUrl = options.toUrl;
        self.allowClone = options.allowClone;
        self.query = element.getParent("form").query.observe( self.search );
        self.element = element; //ul.dropdown menu

        self.element.addEvent("click:relay(#cloney)", function(e){

            this.getParent("a").href = self.toUrl(self.getValue(), true, this.checked);

        });

    },

    getValue: function(){

        return this.query.value.escapeHtml();

    },

    empty: function(){

        $.remove("li.findpages", this.element);

    },

    search: function(){

        var value = this.getValue();

        if( (value == null) || (value.trim() == "") ){

            this.empty();

        } else {

            //this.rpc( "name:" + value, this.action );
            this.rpc( value, this.action );
            //for testing ...
            //this.action([{"score":91,"page":"Collapsible List"},{"score":78,"page":"BrushedTemplateEditor"},{"score":78,"page":"BrushedTemplate"},{"score":78,"page":"BrushedTemplateScreenshots"},{"score":76,"page":"BrushedWikiPlugin"},{"score":76,"page":"BrushedTemplateTypography"},{"score":76,"page":"BrushedTemplateDiscussion"},{"score":74,"page":"BrushedTemplateIdeas"},{"score":71,"page":"BrushedTemplateTOC"},{"score":70,"page":"BrushedTemplateDiscussion2006"},{"score":68,"page":"BrushedTemplateMetadataEditor"},{"score":66,"page":"BrushedTemplateEval"},{"score":66,"page":"BrushedTemplateForms"},{"score":66,"page":"BrushedTemplateJSP"},{"score":66,"page":"BrushedTemplateToolbar"},{"score":65,"page":"BrushedTemplateListTable"}]);

            //obsolete:
            //for testing ...
            //this.action({"id":10000,"result":{"javaClass":"java.util.ArrayList","list":[{"map":{"page":"BrushedTemplateCollapse","score":99},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplate","score":95},"javaClass":"java.util.HashMap"},{"map":{"page":"CollapsibleList","score":61},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateInGerman","score":55},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateDiscussion","score":50},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateScreenshots","score":50},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateTypography","score":50},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedConditionalPlugin","score":48},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateDiscussion2006","score":45},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateMetadataEditor","score":44},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTablePlugin","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateColumns","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateCategories","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateToolbar","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateTOC","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateAccordion","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateTip","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateSlimbox","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateRoundedCorners","score":40},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedEditPageHelp","score":37},"javaClass":"java.util.HashMap"}]}});

        }

    },

    action: function( result ){

        var self = this,
            value = self.getValue(),
            elements = [], item;

        if( result ){

            item = result[0];

            if( !item || item.page != value ){

                elements.push( "li.findpages", [
                    "a", { href: self.toUrl(value, true), title: "sbox.create".localize(value) }, [
                        "label.btn.btn-danger.btn-xs.pull-right", { for:"cloney", text: "sbox.clone".localize()}, [
                            "input#cloney[name=cloney][type=checkbox]"
                        ],
                        "span.createpage", { text: value }
                    ]
                ]);

            }

            while( result[0] ){

                item = result.shift();

                elements.push( "li.findpages", [
                    "a", { href: self.toUrl( item.page ) }, [
                        "span.badge.pull-right", { text: item.score },
                        "span", { text: item.page }
                    ]
                ]);
            }

			self.empty();
			if( elements[0] ){ elements.slick().inject( self.element.getFirst(".divider"), "before" ); }
            //self.fireEvent("complete");

        }
    }
});
