package books;

import java.lang.IllegalArgumentException;
import java.lang.RuntimeException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Timer;
import java.util.TimerTask;

class ExpiringFiles {
	private ConcurrentHashMap<String, ExpiringFile> files;
	private Timer tidyUpTimer;
	private TidyUpTask tidyUpTask;

	private static class LazyHolder {
		static final ExpiringFiles instance = new ExpiringFiles();
	}

	public static ExpiringFiles getInstance() {
		return(LazyHolder.instance);
	}

	private ExpiringFiles() {
		files = new ConcurrentHashMap<String, ExpiringFile>();
		tidyUpTimer = new Timer();
		tidyUpTask = new TidyUpTask();
		tidyUpTimer.schedule(tidyUpTask, 2000, 2000);
	}

	public synchronized ExpiringFile find(String userIdentifier) {
		return(files.get(userIdentifier));
	}

	public synchronized void put(String userIdentifier, ExpiringFile file) {
		files.put(userIdentifier, file);
	}

	public synchronized void remove(String userIdentifier) {
		files.remove(userIdentifier);
	}

	private class TidyUpTask extends TimerTask {
		public void run() {
			try {
				tidyUp();
			} catch(Exception e) {
				System.out.printf("An exception was thrown while tidying up");
			}
		}
	}

	private synchronized void tidyUp() {
		for(Map.Entry<String, ExpiringFile> pair : files.entrySet()) {
			if(pair.getValue().isLongInactive()) {
				deleteAndRemoveFile(pair.getKey(), pair.getValue());
			}
		}
	}

	private synchronized void deleteAndRemoveFile(String identifier, ExpiringFile file) {
		System.out.printf("Deleting long unused file %s\n", identifier);

		files.remove(identifier);
		file.deleteFile();
	}
}

