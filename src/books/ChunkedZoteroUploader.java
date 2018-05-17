package books;

import java.io.IOException;
import java.util.Map;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.util.*;
import org.apache.commons.csv.*;

// lucene
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.de.*;
import org.apache.lucene.analysis.en.*;
import org.apache.lucene.analysis.fr.*;
import org.apache.lucene.*;
import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.document.*;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.LuceneDictionary;
//

class BibEntry {
	protected String key = "";
	protected String author = "o. A.";
	protected String title = "o. T.";
	protected String archiveUrl = "";
	protected String[] tags;

	protected BibEntry(CSVRecord record) {
		key = record.get(0);
		title = getRecordOrDefault(record, "Title", "o. T.");
		author = getRecordOrDefault(record, "Author", "o. A.");
		archiveUrl = getRecordOrDefault(record, "Archive Location", "");
		String tagsAsString = record.get("Manual Tags");
		tags = tagsAsString.split("; ");
	}

	public String[] getTags() {
		return(tags);
	}

	public String getTitle() {
		return(title);
	}

	public String getAuthor() {
		return(author);
	}

	public String getArchiveUrl() {
		return(archiveUrl);
	}

	protected String getRecordOrDefault(CSVRecord record, String field, String def) {
		String value = record.get(field);
		if(value.equals(""))
			return(def);
		else
			return(value);
	}
}


class Book extends BibEntry {
	private String address = "o. O.";
	private String year = "o. J.";
	private String series = "";
	private String volume = "";

	public Book(CSVRecord record) {
		super(record);
		series = getRecordOrDefault(record, "Series", "");
		year = getRecordOrDefault(record, "Publication Year", "o. J.");
		address = getRecordOrDefault(record, "Place", "o. O.");
		volume = getRecordOrDefault(record, "Volume", "");
	}

	@Override
	public String toString() {
		return(String.format("%s: %s%s. %s %s%s.", 
					author,
					title,
					!volume.equals("") ? String.format(". Band %s", volume) : "",
					address,
					year,
					!series.equals("") ? String.format(" (= %s)", series) : ""));
	}
}

class Journal extends BibEntry {
	private String issue = "";
	private String year = "";
	private String pages = "";
	private String journalTitle = "";

	public Journal(CSVRecord record) {
		super(record);
		issue = getRecordOrDefault(record, "Issue", "");
		year = getRecordOrDefault(record, "Publication Year", "o. J.");
		pages = getRecordOrDefault(record, "Pages", "");
		journalTitle = getRecordOrDefault(record, "Publication Title", "");

	}

	@Override
	public String toString() {
		return(String.format("%s: %s. In: %s %s (%s), %s.", 
					author,
					title,
					journalTitle,
					issue,
					year,
					pages));
	}
}

class IndexCreator {
	Analyzer analyzer;
	Directory index;
	SpellChecker spellChecker;

	IndexWriterConfig config; 
	IndexWriter w;
	public IndexCreator() throws IOException {
		Path indexDir = MicroserviceConfig.getDurableRoot().resolve("index").resolve("current");
		analyzer = new GermanAnalyzer();
		index = FSDirectory.open(indexDir); //new RAMDirectory();

		config = new IndexWriterConfig(analyzer);

		w = new IndexWriter(index, config);
	}

	public void add(BibEntry entry, BookInfo scanInfo) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("title", entry.getTitle(), Field.Store.NO));
		doc.add(new StringField("author", entry.getAuthor(), Field.Store.NO));
		doc.add(new TextField("tags", Arrays.toString(entry.getTags()), Field.Store.NO));
		doc.add(new TextField("fulltext", getFulltextForScan(scanInfo), Field.Store.NO));
		doc.add(new StringField("bibLine", entry.toString(), Field.Store.YES));
		if(scanInfo != null)
			doc.add(new StoredField("scanId", scanInfo.getId()));
		w.addDocument(doc);
	}

	private String getFulltextForScan(BookInfo scanInfo) throws IOException {
		if(scanInfo == null || !scanInfo.hasPlainOCR())
			return("");

		return(scanInfo.getPlainOCR());
	}


	public void finish() throws IOException {
		w.close();
		w = null;
		config = null;

		index.close();
	}


}

class ZoteroImporter {
	IndexCreator indexCreator;
	Map<String, BookInfo> scansMap = new HashMap<String, BookInfo>();

	public ZoteroImporter(Path path) {
		BookInfo.iterateAllBooks((bookInfo) -> processScan(bookInfo));

		try {
			ReadCSV(path);
		} catch(FileNotFoundException e) {
			System.out.printf("Exception: %s", e.getMessage());
		} catch(IOException e) {
			System.out.printf("Exception: %s", e.getMessage());
		}

	}

	private void processScan(BookInfo info) {
		if(scansMap.containsKey(info.getArchiveUrl())) {
			putImportWarning("Scan " + Integer.toString(info.getId()) + " has been uploaded more than once!");
			return;
		}
		try {
			info.setTitle(info.getArchiveUrl());
		} catch(IOException e) {
			putImportWarning("Could not reset title of scan " + Integer.toString(info.getId()));
		}
		scansMap.put(info.getArchiveUrl(), info);
	}

	private void putImportWarning(String warning) {
		System.out.println(warning);
		// TODO: Do something with the warning
	}

	public void ReadCSV(Path path) throws IOException, FileNotFoundException {
		indexCreator = new IndexCreator();

		BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);

		Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(reader);
		for (CSVRecord record : records) {
			BibEntry entry = null;

			if(record.get("Item Type").equals("book"))
				entry = new Book(record);
			else if(record.get("Item Type").equals("journalArticle"))
				entry = new Journal(record);
			else
				System.out.printf("Unknown item type: %s\n", record.get("Item Type"));
			if(entry != null)
				processValidEntry(entry);
		}
		indexCreator.finish();
	}

	private void processValidEntry(BibEntry entry) throws IOException {
		BookInfo info = null;

		if(scansMap.containsKey(entry.getArchiveUrl())) {
//			System.out.println("I found one scan in the books list!!! It is: " + 
//					Integer.toString(scansMap.get(entry.getArchiveUrl()).getId())));
			info = scansMap.get(entry.getArchiveUrl());
			info.setTitle(entry.toString());
		}
		indexCreator.add(entry, info);
	}
}

public class ChunkedZoteroUploader extends ChunkedUploader {
	public ChunkedZoteroUploader(HttpParameters parameters) throws IllegalArgumentException {
		super(parameters);
	}

	public Response getResponse() throws IOException {
		if(getUpload().isComplete()) {
			ZoteroImporter importer = new ZoteroImporter(getUpload().getPath());
			//getUpload().getPath();
		} 

		JSONResponse response = new JSONResponse();
		response.put("state", "ok");
		return(response);
	}
}

