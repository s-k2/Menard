package books;

import java.lang.IllegalArgumentException;
import java.lang.RuntimeException;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;
import java.util.regex.*;
import java.util.Enumeration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class UnassignedZip extends ExpiringFile {
	protected TreeMap<String, Integer> entryNamesToPages;
	protected String ocrEntry;

	public UnassignedZip(ChunkedUpload chunked) throws IOException {
		super(chunked.getPath());

		entryNamesToPages = new TreeMap<String, Integer>();
		ocrEntry = new String();

		checkZip();
	}

	protected UnassignedZip(Path path) {
		super(path);
	}

	private synchronized void checkZip() throws IOException {
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(getPath().toString());
			Enumeration zipEntries = zipFile.entries();

			while(zipEntries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) zipEntries.nextElement();
				if(!entry.isDirectory())
					checkZipEntry(entry.getName());
			}

			if(entryNamesToPages.isEmpty())
				throw new IllegalArgumentException("You uploaded a zip-file that did not contain any scans");
		} catch(ZipException e) {
			throw new IllegalArgumentException("You uploaded a file which is not a zip-file");
		}finally {
			if(zipFile != null)
				zipFile.close();
		}
	}

	private synchronized void checkZipEntry(String entry) {
		if(!checkZipEntryForScan(entry))
			checkZipEntryForOCR(entry);
	}

	private synchronized boolean checkZipEntryForScan(String entry) {
		// find the last number in the filename and use it as page-number
		Pattern pattern = Pattern.compile("(\\d+)\\D*\\.jpg$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(entry);

		if(matcher.find()) {
			int pageNum = Integer.parseInt(matcher.group(1));
			if(entryNamesToPages.containsValue(pageNum))
				throw new IllegalArgumentException("Your uploaded zip-file contained files with the same page number");
			entryNamesToPages.put(entry, pageNum);

			return(true);
		}
		return(false);
	}

	private synchronized void checkZipEntryForOCR(String entry) {
		Pattern pattern = Pattern.compile(".+\\.txt$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(entry);

		if(matcher.matches() && !hasOCR()) {
			ocrEntry = entry;
		}
	}

	public synchronized boolean hasOCR() {
		return(!ocrEntry.isEmpty());
	}

	public synchronized List<Integer> getPages() {
		List<Integer> allPages = new ArrayList<Integer>();

		for(Integer page : entryNamesToPages.values())
			allPages.add(page);

		return(allPages);
	}
}
