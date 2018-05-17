"use strict";

class PagedTable {
	constructor(parentNode, onRender, onUpdate) {
		this.parentNode = parentNode;
		this.onRender = onRender;
		this.onUpdate = onUpdate;
		this.currentPage = 0;
		this.PageSize = 15;
		this.books = null;
		this.parentNode.querySelector("button").addEventListener("click", this.onUpdateClicked.bind(this));
	}

	setData(data) {
		this.data = data;
		this.goToPage(0);
	}

	goToPage(newPage) {
		let tbody = this.parentNode.querySelector("table tbody");
		tbody.innerHTML = "";

		for(let i = this.PageSize * newPage; i < this.data.length && i < this.PageSize * (newPage + 1); i++) {
			tbody.appendChild(this.onRender(this.data[i]));
		}

		let pageNavigation = this.parentNode.querySelector(".pageNavigation");
		pageNavigation.innerHTML = "";

		let lastPage = Math.ceil(this.data.length / this.PageSize) - 1;
		let visiblePageNumbers
			
		if(lastPage > 12)
			visiblePageNumbers = [ 0, Math.min(1, lastPage),  // first 2 pages
				Math.max(newPage - 2, 0), Math.max(newPage - 1, 0), // 2 pages before the current one
				newPage, // the current Page
				Math.min(newPage + 1, lastPage), Math.min(newPage + 2, lastPage), // 2 after the current one
				Math.max(lastPage - 1, 0), lastPage // the last 2 pages
			];
		else
			visiblePageNumbers = [...Array(lastPage + 1).keys()];


		let uniqueVisiblePageNumbers = [... new Set(visiblePageNumbers)];
		uniqueVisiblePageNumbers.sort(function(a, b) { 
			return(a - b); 
		});

		let prevPage = -1;
		for(let page of uniqueVisiblePageNumbers) {
			let element = document.createElement("li");
			if(page === newPage)
				element.classList.add("currentPage");
			if(page - 1 !== prevPage)
				pageNavigation.appendChild(document.createTextNode("... "));
			prevPage = page;

			element.innerHTML = (page + 1).toString();
			pageNavigation.appendChild(element);
			element.addEventListener("click", this.onPageClicked.bind(this));
		}

		this.currentPage = newPage;

	}

	onPageClicked(e) {
		this.goToPage(parseInt(e.target.innerHTML) - 1);
	}
	
	onUpdateClicked(e) {
		this.onUpdate();
	}
}

class Server {
	static Get(filename, okCallback, errorCallback) {
		let sendRequest = new XMLHttpRequest();
		sendRequest.open("GET", filename + "?" + new Date(), true);
		sendRequest.onreadystatechange = () => {
			if(sendRequest.readyState == 4) {
				if(sendRequest.status == 200) {
					okCallback(sendRequest.responseText);
				} else {
					errorCallback(sendRequest.responseText);
				}
			}
		}
		sendRequest.send(null);

	}
}

class ModuleLoader {
	constructor(classType, moduleRoot) {
		Server.Get(classType.getName() + ".html", (response) => {
			moduleRoot.innerHTML = response;
			new classType(moduleRoot);
		}, () => {});
	}
}

class MessageBox
{
	constructor(title, message) {
		let allContainers = document.querySelectorAll("body *");
		for(let container of allContainers) {
			container.classList.add("applyBlur");
		}

		let background = document.createElement("div");
		background.classList.add("messageBoxBackground");

		let box = document.createElement("div");
		box.classList.add("messageBoxContainer");
		box.innerHTML = '<div class="messageBox"><h1>' + title + '</h1><p>' + message + '</p><button>Ok</button></div>';
		box.querySelector("button").addEventListener("click", this.onOk.bind(this));
		document.querySelector("body").appendChild(background);
		document.querySelector("body").appendChild(box);
	}

	onOk() {
		let allContainers = document.querySelectorAll(".applyBlur");
		for(let container of allContainers) {
			container.classList.remove("applyBlur");
		}
		
		this.removeNode(document.querySelector(".messageBoxBackground"));
		this.removeNode(document.querySelector(".messageBoxContainer"));
	}

	removeNode(node) {
		if(node !== undefined && node != null) {
			node.parentNode.removeChild(node);
		}
	}


}

function init() {
	//let uploader = new BookUploader(document.querySelector(".container"));
	
	createContainers();
	/*
	if(document.querySelector(".container.tmpBookList"))
		loader = new ModuleLoader(BookList, document.querySelector(".container"));
	else
		loader = new ModuleLoader(BookUploader, document.querySelector(".container"));
	//let loader = new ModuleLoader(BookList, document.querySelector(".container"));*/
}

function createContainers() {
	let isFirst = true;
	
	let modulesToLoad = document.querySelectorAll("nav ul li");
	for(let moduleButton of modulesToLoad) {
		let newContainer = document.createElement("div");
		newContainer.classList.add("container");
		document.querySelector("body").appendChild(newContainer);
		
		let module = null;
		
		if(moduleButton.getAttribute("data-module") == "bookList")
			module = assignToModule(BookList, newContainer);
		else if(moduleButton.getAttribute("data-module") == "bookUploader")
			module = assignToModule(BookUploader, newContainer);
		else if(moduleButton.getAttribute("data-module") == "ocrList")
			module = assignToModule(OcrList, newContainer);
		else if(moduleButton.getAttribute("data-module") == "importZotero")
			module = assignToModule(ImportZotero, newContainer);
			
		newContainer.moduleButton = moduleButton;
		moduleButton.addEventListener("click", showModule.bind(newContainer));
		
		if(isFirst === true) {
			showModule.bind(newContainer).call();
			isFirst = false;
		}
	}
}

function showModule(newContainer) {
	let selectedContainer = document.querySelector(".container.active");
	if(selectedContainer !== newContainer) {
		if(selectedContainer !== null) {
			selectedContainer.classList.remove("active");
			selectedContainer.moduleButton.classList.remove("active");
		}
		this.classList.add("active");
		this.moduleButton.classList.add("active");
	}
}

function assignToModule(classType, newContainer) {
	newContainer.classList.add(classType.getName());
	
	return(new ModuleLoader(classType, newContainer));
}

window.addEventListener("load", init, false);

