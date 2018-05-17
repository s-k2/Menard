package books;

import java.util.function.*;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.*;
import java.nio.charset.*;
import static java.nio.file.StandardCopyOption.*;
import java.util.UUID;

import org.json.*;

class BookInfo {
	BookDir bookDir;
	JSONObject json;

	public BookInfo(int bookId) throws IOException {
		bookDir = new BookDir(bookId);
		readJson();
	}

	private BookInfo(Path bookDirPath) throws IOException {
		bookDir = new BookDir(bookDirPath);
		readJson();
	}

	public void readJson() throws IOException {
		Path jsonPath = bookDir.getInfoPath();
		try(BufferedReader reader = Files.newBufferedReader(jsonPath, Charset.forName("UTF-8"))) {
			json = new JSONObject(new JSONTokener(reader));
		}
	}

	public BookInfo(BookDir bookDir, String archiveUrl) throws IOException {
		this.bookDir = bookDir;
		initializeDefaultJson(archiveUrl);
	}

	public void initializeDefaultJson(String archiveUrl) throws IOException {
		json = new JSONObject();
		json.put("description", archiveUrl);
		json.put("archiveUrl", archiveUrl);
		json.put("date", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));

		Files.write(bookDir.getInfoPath(), json.toString().getBytes("utf-8"));
	}


	public BookDir getBookDir() {
		return(bookDir);
	}

	public int getId() {
		return(bookDir.getId());
	}

	public String getTitle() {
		if(!json.has("description"))
			throw new RuntimeException("INTERNAL-DATA-ERROR: " + 
					bookDir.toString() + " has no description in its JSON");
		return(json.getString("description"));
	}

	public long getDate() {
		if(!json.has("date"))
			throw new RuntimeException("INTERNAL-DATA-ERROR: " + 
					bookDir.toString() + " has no date in its JSON");
		return(json.getLong("date"));
	}

	public String getArchiveUrl() {
		if(!json.has("archiveUrl"))
			throw new RuntimeException("INTERNAL-DATA-ERROR: " + 
					bookDir.toString() + " has no archiveUrl in its JSON");
		return(json.getString("archiveUrl"));
	}

	public void setTitle(String newTitle) throws IOException {
		updateJson((json) -> json.put("description", newTitle));
	}

	public void setDate(long newDate) throws IOException {
		updateJson((json) -> json.put("date", newDate));
	}

	public void setArchiveUrl(String newArchiveUrl) throws IOException {
		updateJson((json) -> json.put("archiveUrl", newArchiveUrl));
	}

	private void updateJson(Consumer<JSONObject> consumer) throws IOException {
		// TODO: Some kind of locking would be nice...
		readJson();
		consumer.accept(json);
		Files.write(bookDir.getNewInfoPath(), json.toString().getBytes("utf-8"));
		Files.move(bookDir.getNewInfoPath(), bookDir.getInfoPath(), 
				StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	public boolean hasPlainOCR() {
		return(bookDir.hasPlainOCR());
	}

	public String getPlainOCR() throws IOException {
		return(bookDir.getPlainOCR());
	}

	public boolean hasOCR() {
		return(bookDir.hasOCR());
	}

	public void replaceOCR(Path newOCR) throws IOException {
		bookDir.replaceOCR(newOCR);
	}

	public static void iterateAllBooks(Consumer<BookInfo> consumer) {
		Path booksDirectory = MicroserviceConfig.getDurableRoot().resolve("books");

		try(DirectoryStream<Path> stream = Files.newDirectoryStream(booksDirectory)) {
			for(Path bookDir : stream) {
				consumer.accept(new BookInfo(bookDir));
			}
		} catch(IOException | DirectoryIteratorException e) {
			System.out.println("Got an error in iterating all books\n");
			System.out.println(e.getMessage());
			throw new RuntimeException("Could not iterate the directories in " + booksDirectory.toString());
		}
	}

}
