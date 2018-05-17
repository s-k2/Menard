"use strict";

class BookUploader {
	static getName() {
		return("bookUploader");
	}

	constructor(moduleRoot) {
		this.filesList = moduleRoot.querySelector(".filesList");
		this.drophere = moduleRoot.querySelector(".filesList>li:last-child");

		this.resumable = new Resumable({
			testChunks: false,
			target: "/books/actions/uploadFileChunk",
			method: "octet",
			simultaneousUploads: 1,
		});

		this.resumable.assignDrop(this.filesList);
		this.resumable.assignBrowse(this.drophere);

		this.resumable.on("fileAdded", this.onFileNew.bind(this));
		this.resumable.on("fileProgress", this.onFileProgress.bind(this));
		this.resumable.on("fileSuccess", this.onFileSucessfullyUploaded.bind(this));
		this.resumable.on("fileError", this.onFileError.bind(this));

		moduleRoot.querySelector(".saveButton").addEventListener("click", this.onSave.bind(this));
	}


	// print information about the current book (page-range, containing OCR)
	updateInformation() {
		let hasText = false;
		let rangesOfAll = [];

		let allUploads = this.filesList.querySelectorAll("li:not(:last-child)");
		for(let upload of allUploads) {
			if(upload.hasAttribute("data-hasText"))
				hasText = hasText || (upload.getAttribute("data-hasText") === "true");

			if(upload.hasAttribute("data-range")) {
				let ranges = JSON.parse(upload.getAttribute("data-range"));
				for(let range of ranges) {
					if(range.length == 2) // it is an range for itself
						rangesOfAll.push(range);
					else // this is a single page
						rangesOfAll.push([range, range]);
				}
			}
		}

		// sort by page-number
		rangesOfAll.sort(function(a, b) {
			return(a[0] - b[0]);
		});

		// merge all ranges that follow each other without any gap
		for(let i = 1; i < rangesOfAll.length; i++) {
			if(rangesOfAll[i - 1][1] == rangesOfAll[i][0] - 1) {
				rangesOfAll[i - 1][1] = rangesOfAll[i][1];
				rangesOfAll.splice(i, 1);
				i = i - 1; // we removed one item, so we must decrement our index
			}
		}

		// in all single-page ranges just remove the second entry
		for(let i = 1; i < rangesOfAll.length; i++) 
			if(rangesOfAll[i][0] == rangesOfAll[i][1])
				rangesOfAll[i].splice(1);

		// output the page-ranges nicely
		let rangesOfAllString = [];
		for(let rangeItem of rangesOfAll)
			rangesOfAllString.push(rangeItem.join("–"));
		// and output all ranges in a nice way
		document.querySelector(".pagesRange").innerHTML = rangesOfAllString.join(", ");
		
		// do we have searchable text?
		document.querySelector(".hasText").innerHTML = 
			hasText === true ? "Ja, enthält durchsuchbahren Text" : "Nein, es wurde kein durchsuchbarer Text hochgeladen";
	}

	removeFileFromList(node, message) {
		if(message !== undefined)
			alert(message);

		this.filesList.removeChild(node);
	}

	// a new file to upload has been selcted by the user
	onFileNew(file) {
		let newEntry = document.createElement("li");
		newEntry.className = "entry";

		// BUGFIX: Safari does not normalize filename which may look ugly with some fonts
		newEntry.innerHTML = file.fileName.normalize() + 
			'<progress value="0" max="100"></progress>' +
			'<button aria-label="Abbrechen">&#128473;</button>';

		this.filesList.insertBefore(newEntry, this.drophere);
		file.listEntry = newEntry;
		
		this.resumable.upload();
	}

	onFileProgress(file) {
		if(file !== undefined && file.progress !== undefined && file.listEntry.querySelector("progress") !== null)
			file.listEntry.querySelector("progress").setAttribute("value", Math.round(file.progress() * 100));
	}

	onFileSucessfullyUploaded(file, message) {
		// no need to show progress indicator any more
		file.listEntry.removeChild(file.listEntry.querySelector("progress"));
		file.listEntry.removeChild(file.listEntry.querySelector("button"));

		let response = JSON.parse(message);
		if(!("hasText" in response) || !("range" in response) || !Array.isArray(response.range) || !("id" in response))
		{
			this.removeFileFromList(file.listEntry, "Ungültige Serverantwort");
			return;
		}
		
		file.listEntry.setAttribute("data-hasText", response.hasText);
		file.listEntry.setAttribute("data-range", JSON.stringify(response.range));
		file.listEntry.setAttribute("data-id", response.id);

		this.updateInformation();
	}

	onFileError(file, message) {
		console.log("fileError (" + file.fileName.normalize() + "): " + message);
		let response = JSON.parse(message);

		this.resumable.removeFile(file);
		this.removeFileFromList(file.listEntry, response.errorMessage);
	}

	onSave() {
		// validate the data
		let data = {
			"description": document.querySelector(".description").value.trim(),
			"files": [],
		};

		for(let file of this.filesList.querySelectorAll("li:not(:last-child)"))
			if(file.hasAttribute("data-id"))
				data.files.push(file.getAttribute("data-id"));

		if(data.files.length === 0) {
			new MessageBox("Speichern (noch) nicht möglich", "Du hast noch keine Dateien in Schritt 1 hochgeladen");
			return;
		}
		if(!data.description) {
			new MessageBox("Speichern (noch) nicht möglich", "Du hast noch keinen Permalink in Schritt 3 angegeben");
			return;
		}
		
		let sendRequest = new XMLHttpRequest();
		sendRequest.open("POST", "/books/actions/saveBook", true);
		sendRequest.onreadystatechange = () => {
			if(sendRequest.readyState == 4) {
				if(sendRequest.status == 200) {
					new MessageBox("Buch gespeichert", "Spätestens in ein paar Minuten findest du das Buch auf dem Server");
					this.resetAll();
				} else {
					new MessageBox("Fehler beim Speichern", "Irgendetwas lief schrief, die Dateien konnten nicht gespeichert werden");
				}
			}
		}
		sendRequest.send(JSON.stringify(data));
		
		//let m = new MessageBox("Buch gespeichert", "Du kannst das Buch unter diesem Link lesen");

	}

	resetAll() {
		for(let file of this.filesList.querySelectorAll("li:not(:last-child)"))
			this.filesList.removeChild(file);

		document.querySelector(".pagesRange").innerHTML = "Noch keine Scans hochgeladen";
		document.querySelector(".hasText").innerHTML = "Nein, es wurde kein durchsuchbarer Text hochgeladen";

		document.querySelector(".description").value = "";	

		// cancel all resumable uploads (even if all uploads are complete this is 
		// necessary to allow re-uploading the same file again)
		// ATTENTION: There is one little problem, this function call the onProgress-handler again,
		// so we do it at last and just accept the exception
		this.resumable.cancel();
	}
}


