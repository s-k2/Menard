package books;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.nio.file.*;
import static java.nio.file.StandardCopyOption.*;
import org.json.*;
import java.util.concurrent.TimeUnit;

import books.MicroserviceConfig;

class ExtractAndMove implements Runnable {
	private List<AssignedZip> uploads = new ArrayList<AssignedZip>();
	private BookDir bookDir;

	public ExtractAndMove(JSONArray files, String description) throws IOException {
		bookDir = new BookDir();
		BookInfo bookInfo = new BookInfo(bookDir, description);

		for(int i = 0; i < files.length(); i++) {
			assignScans(files.getString(i));
		}
	}

	private void assignScans(String identifier) throws IOException {
		ExpiringFile file = ExpiringFiles.getInstance().find(identifier);
		if(file == null) 
			throw new IllegalArgumentException("There is no file with id " + identifier);
		if(!(file instanceof UnassignedZip))
			throw new IllegalArgumentException("There is no valid zipped scan with id " + identifier);
		UnassignedZip unassigned = (UnassignedZip) file;
		
		AssignedZip assigned = new AssignedZip(unassigned, bookDir);
		ExpiringFiles.getInstance().remove(identifier);

		uploads.add(assigned);
	}

	public String getId() {
		return(bookDir.getPath().toString());
	}

	public synchronized void run() {
		try {
			String temporaryPath = bookDir.getPath().toString();
			System.out.println("Extracting all Zip-files of " + temporaryPath);
			extractAll();
			bookDir.moveToPermanentStorage();
			createReaderInfo(); // TODO: We need to do this after moving the file, else the urls won't be in the right format
			System.out.println("Completed extraction and info-creation for book " + temporaryPath + " and moved to " + bookDir.getPath().toString());
		} catch(Exception e) {
			System.out.printf("Something went wrong with book %s (%s)\n", 
					bookDir.getPath().toString(), e.toString());
			// TODO: Delete all files here
		}
	}

	private void extractAll() {
		for(AssignedZip upload : uploads) {
			upload.extract();
		}
	}

	private void createReaderInfo() throws IOException {
		ReaderInfo readerInfo = new ReaderInfo(bookDir);
		readerInfo.write();
	}
}

public class SaveBook implements PathHandler {
	static Executor saveExecutor = Executors.newSingleThreadExecutor();
	JSONResponse response = new JSONResponse();

	public SaveBook(HttpParameters parameters) throws IllegalArgumentException {
	}

	public void setInputStream(InputStream input) throws IOException {
		JSONObject json = new JSONObject(new JSONTokener(input));

		String description = json.getString("description");
		JSONArray files = json.getJSONArray("files");

		ExtractAndMove extractAndMove = new ExtractAndMove(files, description);
		response.put("id", extractAndMove.getId());

		saveExecutor.execute(extractAndMove);
	}

	public Response getResponse() {
		return(response);
	}
}
