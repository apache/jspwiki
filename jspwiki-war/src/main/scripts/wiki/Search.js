/*
Class: Wiki.Search
    ...

Depends:
    Graphbar

DOM Structure:
    (start code)
    (end)

Example:
    (start code)
    new wiki.Search( form, {
        url: wiki.XHRSearch,
        onComplete: function(){ this.wiki.set('PrevQuery', form.query); }
    });
    (end)

*/
Wiki.Search = new Class({

    Binds: ['action','setQuery','setStart'],
    Implements: [Events, Options],

    initialize:function(form,options){

        var self = this, s;

        self.setOptions(options);
        self.form = form;
        form.getElements('input[name=scope]').addEvent('click', self.setQuery);
        form.details.addEvent('click', self.action);

        self.result = form.getNext().addEvent('click:relay(.pagination a)', self.setStart);

        self.query = form.query.observe( function(){
            form.start.value='0';   // reset the start page before re-running new ajax searches
            self.action();
        });

        // hash may contain a query pagination parameter: (-1=all,0,1,2...)
        if( location.hash ){
            s = decodeURIComponent(location.hash.slice(1)).match(/(.*):(-?\d+)$/);
            if(s && s[2] /*s.length==3*/ ){
                //console.log(s);
                query.value = s[1];
                form.start.value = s[2];
                self.setQuery();
            }
        }
    },

    scopeRE : /^(?:author:|name:|contents:|attachment:)/,
    setQuery: function( ){

        this.query.value =
            this.form.getElement("input[name=scope]:checked").value +
            this.query.get('value').replace(this.scopeRE, '');

        this.action();

    },

    setScope: function( query ){

        var scope = query.match( this.scopeRE ) ||"";
        scope = this.form.getElement("input[name=scope][value="+scope+"]");
        if( scope ){ scope.checked=true; }

    },

    setStart: function(event){    
        event.stop();
        //this.form.start.value = event.target.get('data-start');
        this.action();
    },    

    action: function(){

        var self = this,
            form = self.form,
            query = self.query.value,
            result = self.result;

        if( !query || (query.trim()=='') ){
            return result.empty();
        }

        self.setScope( query );

        new Request.HTML({
            url: self.options.xhrURL,
            data: form,
            //method: 'post',  //is default
            update: result,
            onComplete: function(){
                spin.hide();
                new GraphBar( result.getElement('.graphBars' ));
                self.fireEvent('onComplete');
            }
        }).send();

        location.hash = '#'+ query+':'+form.start.value;  // push the query into the url history

    }

});

/*
FIXME : hack the old runfullsearch() 
    <a title="Show items from 21 to 40" 
        onclick="$('start').value=20; SearchBox.runfullsearch();">2</a>
    
Better:
    <a title="Show items from 21 to 40" data-start="20" >2</a>
*/
var SearchBox = { runfullsearch : function(){} }
