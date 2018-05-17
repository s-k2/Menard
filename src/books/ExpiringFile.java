package books;

import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

class ExpiringFile {
	private long lastActivity = System.currentTimeMillis();
	private final Path path;

	ExpiringFile(Path path) {
		this.path = path;
	}

	protected synchronized void registerActivity() {
		lastActivity = System.currentTimeMillis();
	}

	public synchronized boolean isLongInactive() {
		final long TooOldInMillis = TimeUnit.HOURS.toMillis(3);
		long now = System.currentTimeMillis();

		return((now - lastActivity) > TooOldInMillis);
	}

	public String getFileId() {
		return(path.getFileName().toString());
	}

	public Path getPath() {
		return(path);
	}

	public synchronized void deleteFile() {
		try {
			Files.delete(getPath());
		} catch(IOException e) {
			System.out.printf("Could not delete %s\n", getFileId());
		}
	}
}
