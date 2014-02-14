/*
Class: Wiki.RecentSearches
 * FIXME: remember 10 most recent search topics (cookie based)
 * Extended with quick links for view, edit and clone (ref. idea of Ron Howard - Nov 05)
 * Refactored for mootools, April 07

Uses:
    #recentClear
    #recentSearches
Depends:
    wiki

DOM-structure:
(start code)
    <div id="recentSearches">
      <fmt:message key="sbox.recentsearches"/>
      <a href="#"><fmt:message key="sbox.clearrecent"/></a>
      <ul>
          <li><a href...>...</a></li>
      </ul>
    </div>
(end code)

Examples:
>    wiki.add('.searchbox-recents', function(element){
>       new Wiki.Recents(element, {
>           items: wiki.prefs.get('RecentSearch'),
>           onChange: function(recents){ wiki.set('RecentSearch',recents); }
>       });
>   });

*/
Wiki.Recents = new Class({

    Implements: [Events, Options],

    initialize:function(dropdown,options){

        var self = this, 
            items, i=0, len, 
            list=[], li='li.recents';

		self.setOptions( options );
        //self.options.items = ['foo','bar']; //test

        self.items = items = self.options.items || list;
        self.form = dropdown.getParent('form').addEvent('submit', self.submit.bind(self) );

        //build li.recents dropdown items
        if( items[0] ){

            while(items[i]){
                list.push(li, ['a', { html:items[i++].stripScripts() }] );
            }
            //list.push(li+'.clear',['a',{html:'[Clear Recent Searches]' }]);
            list.push(li+'.clear',['a', [ 'span.btn.btn-xs.btn-default',{text:'Clear Recent Searches' }]]);
            dropdown.adopt( list.slick() );
        }
        dropdown.addEvent('click:relay('+li+')', function(ev){ ev.stop(); self.action(this); });

    },

    action: function( element ){

        var self = this, form = self.form;

        if( element.match('.clear') ){

            //element.getSiblings('li.recents').destroy();
            //element.destroy();
            element.getElements('!> > li.recents').destroy(); //!> == direct parent
            self.items = [];
            self.fireEvent('change'/*,null*/);

        } else {

            form.query.value = element.get('text');
            form.submit();

        }

    },

    submit:function(){

        var self = this,
            items = self.items,
            value = self.form.query.value.stripScripts(); //xss

        if( items.indexOf( value ) < 0 ){

            //insert new item at the start of the list, and cap list on max 10 items
            if( items.unshift(value) > 9){ items = items.slice(0,9); }
            self.fireEvent('change', [self.items = items] );

        }

    }

});