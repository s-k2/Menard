"use strict";

class ImportZotero {
	static getName() {
		return("importZotero");
	}

	constructor(moduleRoot) {
	
		let uploadCell = document.querySelector("#uploadButton");
		
		uploadCell.resumable = new Resumable({
			target: "/books/actions/uploadZotero",
			testChunks: false,
			method: "octet",
			simultaneousUploads: 1,
		});
		uploadCell.resumable.assignBrowse(uploadCell, false);
		uploadCell.resumable.on("fileAdded", function(file) {
			this.upload();
		});
		uploadCell.resumable.on("fileProgress", function(file) {
			if(file !== undefined && file.progress !== undefined)
				this.innerText = Math.round(file.progress() * 100) + "%";
		}.bind(uploadCell));
		uploadCell.resumable.on("fileSuccess", function(file) {
			this.innerText = "Vorhanden";
		}.bind(uploadCell));
		uploadCell.resumable.on("fileError", function(file, message) {
			this.innerText = "Fehler";
		}.bind(uploadCell));
	}	
}
