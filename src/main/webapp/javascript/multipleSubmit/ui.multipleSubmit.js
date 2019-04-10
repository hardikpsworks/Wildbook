/*

These functions create the many HTML components in the page. Many are nested together. 
There are listed in decending as much at that is applicable, ie imageTile contains an imageDataOverlay which contains 
an encNumDropdown ect.

*/


multipleSubmitUI = {

    getImageIdForIndex: function(index) {
        return "img-"+String(index);
    },
    
    getImageUIIdForIndex: function(index) {
        return "img-input-"+String(index);
    },

    generateMetadataTile: function(index) {
        var metadataTile = "";
        metadataTile += "<div id=\"encounter-metadata-"+index+"\" class=\"encounter-tile-div col-xs-12 col-xl-12\">";
        metadataTile += "   <label>Encounter "+index+"</label>";
        metadataTile += "   <input id=\"encLocation\" type=\"text\" name=\"encLocation\" required placeholder=\"Enter Location\">";
	    metadataTile +=	"	<input name=\"encDate\" title=\"Sighting Date/Time\" type=\"text\" placeholder=\"Enter Date\" class=\"encDate\"/>";
        metadataTile += "   <label>&nbsp;</label>";   
        metadataTile += "   <br/>";
        metadataTile += "</div>";
        return metadataTile;
    }, 

    generateImageTile: function(file, index) {
        var imageTile = "";
        imageTile += "<div id=\"image-tile-div-"+index+"\" class=\"image-tile-div col-xs-6 col-sm-4 col-md-3 col-lg-3 col-xl-3\" onclick=\"imageTileClicked("+index+")\" onmouseover=\"showOverlay("+index+")\" onmouseout=\"hideOverlay("+index+")\" >";
        //imageTile += "  <label class=\"image-filename\">File: "+file.name+"</label>";
        imageTile += "  <img class=\"image-element\" id=\""+multipleSubmitUI.getImageIdForIndex(index)+"\" src=\"#\" alt=\"Displaying "+file.name+"\" />";
        imageTile += multipleSubmitUI.generateImageDataOverlay(file,index);                
        imageTile += "</div>";
        //console.log("image tile: "+imageTile);
        return imageTile;
    },
                    
    generateImageDataOverlay: function(file,index) {
        var uiClass = multipleSubmitUI.getImageUIIdForIndex(index);
        var overlay = "";
        overlay += "  <div hidden id=\"img-overlay-"+index+"\" class=\"img-overlay-"+index+" img-input "+uiClass+"\" >";
        // make a "click to focus" prompt here on hover
        overlay += multipleSubmitUI.generateEncNumDropdown(index);
        overlay += "      <textarea class=\""+uiClass+"\" placeholder=\"More Info\" rows=\"3\" cols=\"23\" />";                     
        overlay += "      <label class=\"image-filename "+uiClass+"\">File: "+file.name+"</label>";
        overlay += "  </div>";
        return overlay; 
    },
    
    generateEncNumDropdown: function(index) { 
        var uiClass = multipleSubmitUI.getImageUIIdForIndex(index);
        var encDrop = "";
        encDrop += "<select class=\""+uiClass+"\" name=\"enc-num-dropdown-"+index+"\">";
        encDrop += "    <option selected=\"selected\" value=\""+i+"\" disabled>Choose Encounter Number</option>";
        for (var i=0;i<multipleSubmitUI.encsDefined();i++) {
            encDrop += "<option value=\""+i+"\">"+(i+1)+"</option>";
        }
        encDrop += "</select>";
        return encDrop;
    },


    renderImageInBrowser: function(file,id) {
        if (this.notNullOrEmptyString(String(file))) {
            var reader = new FileReader();
            reader.onload = function(e) {
                console.log("Target ID for image render: #"+multipleSubmitUI.getImageIdForIndex(id));
                $('#'+multipleSubmitUI.getImageIdForIndex(id)).attr('src', e.target.result); // This is the target.. where we want the preview
            }
            reader.readAsDataURL(file);
        }
    },
    
    notNullOrEmptyString: function(entry) {
        if (entry==undefined||entry==""||!entry) return false;
        return true; 
    }, 

    encsDefined() {
        return document.getElementById('numberEncounters').value;
    }
    
};