"use strict";

class BookList {
	static getName() {
		return("bookList");
	}

	constructor(moduleRoot) {
		this.table = new PagedTable(document.querySelector(".bookList"),
			function(book) {
				let row = document.createElement("tr");
				let date = new Date(book.date * 1000);
				let title = book.title == book.archiveUrl ?
						('<a href="' + book.archiveUrl + '" target="_blank">' + book.archiveUrl + '</a>') :
						(book.title + ' (<a href="' + book.archiveUrl + '" target="_blank">' + book.archiveUrl + '</a>)');
				
				row.innerHTML = '<td>' + title + '</td>' +
					'<td>' + date.toLocaleDateString() + ' ' + date.toLocaleTimeString() + '</td>' +
					'<td>' + (book.ocr ? "Ja" : "Nein") + '</td>';
				return(row);
			},
			this.loadData.bind(this));
		this.loadData();
	}
	
	loadData() {
		Server.Get("/books/actions/listBooks", (response) => {
				let data = JSON.parse(response);
				data.books.sort(function(a, b) {
					return(b.date - a.date);
				});

				// validate response?
				this.table.setData(data.books);
			},
			() => {
			});
	}
	
}
