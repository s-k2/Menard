package books;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.charset.Charset;
import org.json.*;

public class OCRList extends BooksList {
	@Override
	protected JSONObject jsonForBook(BookInfo book) {
		JSONObject thisBook = new JSONObject();

		thisBook.put("id", book.getId());
		if(!book.getTitle().equals(book.getArchiveUrl()))
			thisBook.put("title", book.getTitle());
		else
			thisBook.put("title", "");
		thisBook.put("ocr", book.hasOCR());

		return(thisBook);
	}
}

