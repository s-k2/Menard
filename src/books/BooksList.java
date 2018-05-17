package books;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.charset.Charset;
import org.json.*;

import books.MicroserviceConfig;

public class BooksList implements PathHandler {

	public void setInputStream(InputStream input) throws IOException {
	}

	public Response getResponse() throws IOException {
		JSONArray allBooks = new JSONArray();
		BookInfo.iterateAllBooks((bookDir) -> allBooks.put(jsonForBook(bookDir)));

		JSONResponse response = new JSONResponse();
		response.put("books", allBooks);
		
		return(response);
	}

	protected JSONObject jsonForBook(BookInfo book) {
		JSONObject thisBook = new JSONObject();

		thisBook.put("title", book.getTitle());
		thisBook.put("archiveUrl", book.getArchiveUrl());
		thisBook.put("date", book.getDate());
		thisBook.put("ocr", book.hasPlainOCR());

		return(thisBook);
	}
}
