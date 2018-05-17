package books;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.util.Map;

import books.ChunkedUploader;
import books.SaveBook;
import books.Microservice;

import java.io.InputStream;
import org.json.*;
class UpdateInternalData implements PathHandler {

	public void setInputStream(InputStream input) throws IOException {
	}

	public Response getResponse() throws IOException {
		JSONArray allBooks = new JSONArray();
		BookInfo.iterateAllBooks((bookInfo) -> processBook(bookInfo));

		JSONResponse response = new JSONResponse();
		response.put("ok", true);
		
		return(response);
	}

	private void processBook(BookInfo info) {
		try {
			if(!info.getBookDir().hasReaderInfo()) {
				System.out.println("Book " + info.getBookDir().getPath().toString() + " has no readerInfo, creating it...");
				ReaderInfo readerInfo = new ReaderInfo(info.getBookDir());
				readerInfo.write();
				System.out.println("Book " + info.getBookDir().getPath().toString() + " has now a readerInfo");
			}
		} catch(IOException e) {
			System.out.println("IoException while processing book " + Integer.toString(info.getId()));
		} catch(Exception e) {
			System.out.println("Other while processing book " + Integer.toString(info.getId()));
			e.printStackTrace();
		}
	}
}

public class BooksService extends Microservice {
	public static void main(String[] args) throws Exception {
		BooksService service = new BooksService(args);
	}

	BooksService(String[] args) throws IOException {
		super(args);
	}

	@Override
	protected PathHandler constructHandler(String path, HttpParameters parameters) {
		if(path.equals("/books/actions/uploadFileChunk")) 
			return(new ChunkedBookUploader(parameters));
		else if(path.equals("/books/actions/saveBook"))
			return(new SaveBook(parameters));
		else if(path.equals("/books/actions/listBooks"))
			return(new BooksList());
		else if(path.equals("/books/actions/downloadBook"))
			return(new DownloadBook(parameters));
		else if(path.equals("/books/actions/listOCR"))
			return(new OCRList());
		else if(path.equals("/books/actions/uploadOCR"))
			return(new ChunkedOCRUploader(parameters));

		else if(path.equals("/books/actions/uploadZotero"))
			return(new ChunkedZoteroUploader(parameters));
		else if(path.equals("/books/actions/search"))
			return(new Search(parameters));
		else if(path.equals("/books/actions/searchInScan"))
			return(new SearchInScan(parameters));

		else if(path.equals("/books/actions/tmpUpdateInternalData"))
			return(new UpdateInternalData());



		throw new IllegalArgumentException("URL " + path + " has no handler");
	}
}
