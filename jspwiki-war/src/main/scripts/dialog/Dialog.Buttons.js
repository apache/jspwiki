/*

*/
Dialog.Buttons = new Class({

	Extends: Dialog,
	options: {
		buttons:[], //list of button labels
		//onAction: function(button-label){},
		autoClose: true
	},
	initialize: function(options){

        this.setClass('.buttons',options);
        console.log("Dialog.Buttons",options);
		this.parent(options);
		this.setButtons( this.options.buttons );

	},
	/*
	Function: setButtons

	Arguments:
		buttons - objects with key:value as button-label:callback function

	Example:
		(start code)
			myDialog.setButtons(['Ok','Cancel']);
			
			//FIXME???
			myDialog.setButtons({
				Ok:function(){ callback( input.get('value') ); },
				Cancel:function(){}
			});
		(end)
	*/
	setButtons: function( buttons ){

		var self = this,
		    btns = self.get('.btn-group') || 'div.btn-group'.slick().inject(self.element);

		btns.empty().adopt( buttons.map(function(b){

			return 'a.btn.btn-link.btn-sm'.slick({
				html: b.localize(),
				events:{ click: self.action.bind(self,b) }
			});
		}) );

		return self;
	}
})
