/**
 *  SkinQute javascript extensions
 *  needed to initialise RoundedCorner elements.
 */
if ( RoundedCorners ) {  
  var r = RoundedCorners;
//  r.register( "header",    ['yyyy', 'lime', 'lime' ] );
//  r.register( "footer",    ['yyyy', 'lime', 'lime' ] );
}
 
/**
 *  Preload logo images. 
 */
function preloadImages() {
  var imageList = [
    "./templates/default/skins/CleanBlue/images/logo.png",
    "./templates/default/skins/CleanBlue/images/logo-hi.png"
  ];
  for ( var i = 0; i < imageList.length; i++ ) {
    var imageObject = new Image();
    imageObject.src = imageList[i];
  }
}
preloadImages();

