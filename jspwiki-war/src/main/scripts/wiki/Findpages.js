/*
Class: Wiki.SearchBox
    ...
    Adds/Removes 'li.findpages' elements to the dropdown menu.
    These elements are the results of the wiki rpc call, based on the query.


Example:
>    wiki.add('query', Wiki.SearchBox, {
>        rpc: function(value, callback){ wiki.jsonrpc('search.findPages', [value,20], callback },
>        toUrl: wiki.toUrl
>    });
*/
Wiki.Findpages = new Class({

    Binds:['search','action'],
    Implements: Events,

    initialize: function(element, options){

        this.rpc = options.rpc;
        this.toUrl = options.toUrl;
        this.query = element.getParent('form').query.observe( this.search );
        this.element = element; //ul.dropdown menu

    },

    getValue: function(){
        return this.query.value.stripScripts();
    },

    empty: function(){
        this.element.getElements('li.findpages').destroy();
    },

    search: function(){

        var value = this.getValue();

        if( (value == null) || (value.trim()=="") ){

            this.empty();

        } else {

            this.rpc( 'name:'+value, this.action );
            //for testing ...
            //this.action({"id":10000,"result":{"javaClass":"java.util.ArrayList","list":[{"map":{"page":"BrushedTemplateCollapse","score":99},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplate","score":95},"javaClass":"java.util.HashMap"},{"map":{"page":"CollapsibleList","score":61},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateInGerman","score":55},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateDiscussion","score":50},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateScreenshots","score":50},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateTypography","score":50},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedConditionalPlugin","score":48},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateDiscussion2006","score":45},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateMetadataEditor","score":44},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTablePlugin","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateColumns","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateCategories","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateToolbar","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateTOC","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateAccordion","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateTip","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateSlimbox","score":43},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedTemplateRoundedCorners","score":40},"javaClass":"java.util.HashMap"},{"map":{"page":"BrushedEditPageHelp","score":37},"javaClass":"java.util.HashMap"}]}});

        }

    },

    action: function( result ){

        var self = this,
            value = self.getValue(),
            elements = [], content, btn = 'span.btn.btn-xs.btn-danger';

        //helper function
        function addLI( page, score, isEdit, isClone ){

            content = [];
            if( isClone ){
                content.push(btn, {text: "Clone this page and Create".localize() });
            } else if( isEdit ){
                content.push(btn, {text: "Create".localize() });
            }
            content.push( 'span', {text:page});
            if( score ) content.push('span.badge.pull-right', {text:score});

            elements.push( "li.findpages", [
                ( isEdit ? "a.createpage" : "a" ), { href: self.toUrl(page, isEdit, isClone) }, 
                content     
            ]);

        }

        if( result.list ){

            item = result.list[0];
            if( !item || item.map.page!=value ){
                addLI(value, 0, true);  //create new page
                addLI(value, 0, true, true);  //clone current page into a new page
            }

            while(result.list[0]){
                item = result.list.shift().map; 
                addLI(item.page, item.score);                
            }

			self.empty();
			if(elements[0]) elements.slick().inject( self.element.getFirst('.divider'), 'before' );
            //self.fireEvent('complete');

        }
    }
});