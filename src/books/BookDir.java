package books;

import java.util.function.*;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.BufferedReader;
import java.nio.file.*;
import java.nio.charset.*;
import static java.nio.file.StandardCopyOption.*;
import java.util.UUID;

public class BookDir {
	Path bookDir;

	public BookDir(int bookId) {
		bookDir = MicroserviceConfig.getDurableRoot().resolve("books").resolve(Integer.toString(bookId));
	}

	public BookDir(Path path) {
		bookDir = path;
	}

	// creates a new temporary path
	public BookDir() throws IOException {
		bookDir = MicroserviceConfig.getVolatileRoot().resolve("saving").resolve(UUID.randomUUID().toString());
		Files.createDirectories(bookDir);
	}

	public int getId() {
		return(Integer.parseInt(bookDir.getFileName().toString()));
	}

	public Path getPath() {
		return(bookDir);
	}

	public Path getInfoPath() {
		return(bookDir.resolve("info.json"));
	}

	public Path getNewInfoPath() {
		return(bookDir.resolve("info.json.new"));
	}

	public Path getReaderInfoPath() {
		return(bookDir.resolve("readerInfo.json"));
	}

	public boolean hasReaderInfo() {
		return(Files.exists(getReaderInfoPath()));
	}
	
	public boolean hasPlainOCR() {
		return(Files.exists(bookDir.resolve("ocr.txt")));
	}

	public String getPlainOCR() throws IOException {
		return(new String(Files.readAllBytes(bookDir.resolve("ocr.txt")), StandardCharsets.UTF_8));
	}

	public boolean hasOCR() {
		return(Files.exists(bookDir.resolve("ocr.xml")));
	}

	public void replaceOCR(Path newOCR) throws IOException {
		Files.move(newOCR, bookDir.resolve("ocr.xml"), 
				StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	}

	public void moveToPermanentStorage() throws IOException {
		IOException rethrowException = null;

		for(int i = 0; i < 3; i++) {
			try {
				tryMoveToPermanentStorage();
				return;
			} catch(IOException e) {
				rethrowException = e;
			}
		}

		throw rethrowException;
	}

	private void tryMoveToPermanentStorage() throws IOException {
		Path finalDestination = findNewPath(MicroserviceConfig.getDurableRoot().resolve("books"));

		Files.move(bookDir, finalDestination, ATOMIC_MOVE);

		bookDir = finalDestination;
	}

	private Path findNewPath(Path parent) {
		int greatestDirectoryId = -1;

		try(DirectoryStream<Path> stream = Files.newDirectoryStream(parent)) {
			for(Path file : stream) {
				greatestDirectoryId = getGreaterOfTwo(file.getFileName(), greatestDirectoryId);
			}
		} catch(IOException | DirectoryIteratorException e) {
			throw new RuntimeException("Could not iterate the directories in " + parent.toString());
		}

		greatestDirectoryId++;
		return(parent.resolve(Integer.toString(greatestDirectoryId)));
	}

	private int getGreaterOfTwo(Path maybeGreaterPath, int greatestFound) {
		try {
			int maybeGreater = Integer.parseInt(maybeGreaterPath.toString());
			if(maybeGreater > greatestFound)
				return(maybeGreater);
			else
				return(greatestFound);
		} catch(NumberFormatException e) {
			return(greatestFound);
		}
	}

	public String toString() {
		return(bookDir.toString());
	}

	public void iterateAllScanImages(Consumer<Path> consumer) {
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir, "page*.jpg")) {
			for(Path scanFile : stream) {
				consumer.accept(scanFile);
			}
		} catch(IOException | DirectoryIteratorException e) {
			throw new RuntimeException("Could not iterate the directories in " + bookDir.toString());
		}
	}

	public String getPublicUrl(Path path) {
		if(!path.getParent().equals(bookDir))
			throw new RuntimeException("Could not create public url in book " + 
					bookDir.toString() + " for file " + path.toString());
		return("/books/" + bookDir.getFileName() + "/" + path.getFileName());
	}
}


