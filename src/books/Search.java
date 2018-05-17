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

// lucene
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
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
//
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.analysis.tokenattributes.*;

import java.util.Scanner;
import java.util.*;

class BookSearch {
	Analyzer analyzer;
	Directory index;
	SpellChecker spellChecker;

	private BookSearch() {
		try {
			Path indexDir = MicroserviceConfig.getDurableRoot().resolve("index").resolve("current");
			analyzer = new GermanAnalyzer();
			index = FSDirectory.open(indexDir); //new RAMDirectory();
		} catch(IOException e) {
			System.out.println("Exception in IndexSearcher-constructor");
			//
		}
	}

	public JSONArray search(String querystr) throws IOException, ParseException, InvalidTokenOffsetsException {
		long startTime = System.currentTimeMillis();

		JSONArray results = new JSONArray();

		Query q = MultiFieldQueryParser.parse(querystr,
				new String[]{"title", "tags", "fulltext"}, 
				new BooleanClause.Occur[]{BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD},
				analyzer);

		// 3. search
		int hitsPerPage = 100;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		TopDocs docs = searcher.search(q, hitsPerPage);
		ScoreDoc[] hits = docs.scoreDocs;

		// 4. display results
		int hitCount = hits.length;
		for(int i = 0;i < hits.length;++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);

			JSONObject thisResult = new JSONObject();
			thisResult.put("bibLine", d.get("bibLine"));

			String scanIdStr = d.get("scanId");
			if(scanIdStr != null && !scanIdStr.equals("")) {
				int scanId = Integer.parseInt(scanIdStr);
				thisResult.put("scanId", scanId);
			}

			results.put(thisResult);
		}

		// reader can only be closed when there
		// is no need to access the documents any more.
		reader.close();

		long stopTime = System.currentTimeMillis();
		System.out.println("Found " + hits.length + "hits in " + (stopTime - startTime)  + "ms.");

		return(results);
	}

	private static final class InstanceHolder {
		static final BookSearch INSTANCE = new BookSearch();
	}

	public static BookSearch getInstance() {
		return InstanceHolder.INSTANCE;
	}
}


public class Search implements PathHandler {
	String database = "current";
	String query = "";

	public Search(HttpParameters parameters) throws IllegalArgumentException {
		String userInputDb = parameters.getString("database");
		if(userInputDb.equals("current"))
			database = "current";
		query = parameters.getString("query");
	}

	public void setInputStream(InputStream input) throws IOException {
	}

	public Response getResponse() throws IOException {
		JSONResponse response = new JSONResponse();
		response.put("query", query);

		try {
			response.put("books", BookSearch.getInstance().search(query));
//			response.put("extrasTesting", BookSearch.getInstance().testHiglighting(query));
//			BookSearch.getInstance().doSomething(query);
		} catch(Exception e) { // TODO: Was ParseException and should be restored
			System.out.println("Some exception happened with Lucene");
			e.printStackTrace();
			throw new RuntimeException("Could not do query due to ParseException");
		}
		
		return(response);
	}
}
