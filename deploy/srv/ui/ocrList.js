"use strict";

class OcrList {
	static getName() {
		return("ocrList");
	}

	constructor(moduleRoot) {
		this.table = new PagedTable(document.querySelector(".ocrList"),
			this.renderRow.bind(this),
			this.loadData.bind(this));
		this.loadData();
	}

	renderRow(book) {
		let row = document.createElement("tr");
		let date = new Date(book.date * 1000);
		row.innerHTML = '<td><a href="/books/actions/downloadBook?bookId=' + 
			book.id + '">' + (book.title != "" ? book.title : ('Buch #' + book.id)) + '</a></td>' +
			'<td data-bookId="' + book.id + '">' + (book.ocr ? "Vorhanden" : "Fehlt") + '</td>';
		let uploadCell = row.querySelector("td:nth-child(2)");
			//.addEventListener("click", this.onUploadClick.bind(this));

		uploadCell.resumable = new Resumable({
			target: "/books/actions/uploadOCR?bookId=" + book.id,
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

		return(row);
	}
	
	loadData() {
		Server.Get("/books/actions/listOCR", (response) => {
				let data = JSON.parse(response);
				
				// validate response?
				data.books.sort(function(a, b) {
					return(a.id - b.id);
				});

				this.table.setData(data.books);
			},
			() => {
			});
	}

	onUploadClick(e) {
		e.target.resumable = new Resumable({
			target: "/books/actions/uploadOCR",
			query: "bookId=" + e.target.getAttribute("data-bookId"),
			testChunks: false,
			method: "octet",
		});

		alert("Hey, I'm here: " + e.target.getAttribute("data-bookId"));
	}
	
}
