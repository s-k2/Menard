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



public class SearchInScan implements PathHandler {
	String query = "";
	int scanId;

	public SearchInScan(HttpParameters parameters) throws IllegalArgumentException {
		query = parameters.getString("query");
		scanId = parameters.getInt("scanId");
	}

	public void setInputStream(InputStream input) throws IOException {
	}

	public Response getResponse() throws IOException {
		long startTime = System.currentTimeMillis();

		JSONResponse response = new JSONResponse();
		response.put("query", query);
		response.put("scanId", scanId);

		try {
			response.put("books", getPagesOfQuery(query, scanId));
		} catch(Exception e) { // TODO: Was ParseException and should be restored
			System.out.println("Some exception happened with Lucene");
			e.printStackTrace();
			throw new RuntimeException("Could not do query due to ParseException");
		}

		long stopTime = System.currentTimeMillis();
		System.out.println("Searched in scan for " + (stopTime - startTime)  + "ms.");

	
		
		return(response);
	}

	public class OffsetInfos {
		private ArrayList<Integer> offsets;
		private ArrayList<Integer> pageNumbers;

		public OffsetInfos() {
			offsets = new ArrayList<Integer>();
			pageNumbers = new ArrayList<Integer>();
		}

		public void add(int offset, int pageNumber) {
			offsets.add(offset);
			pageNumbers.add(pageNumber);
		}

		public int findPage(int offset) {
			for(int i = 0; i < offsets.size(); i++) {
				if(offsets.get(i) > offset)
					return(pageNumbers.get(i - 1));
			}

			return(-1);
		}
	}

	public OffsetInfos getPageStartOffsets(String text) {
		OffsetInfos offsetInfos = new OffsetInfos();

		int lineStartPos = 0;
		String line = "";
		for(int i = 0; i < text.length(); i++) {
			if(text.charAt(i) == '\n') {
				line = text.substring(lineStartPos, i).trim();

				if(line.startsWith("----- ") && line.endsWith(" -----")) {
					Scanner scanner = new Scanner(line);
					scanner.skip("-----");
					try {
						int pageNum = scanner.nextInt();
						offsetInfos.add(i, pageNum);
					} catch(InputMismatchException e) {
						// skipping this
					}
				}

				lineStartPos = i + 1;
			}
		}

		return(offsetInfos);
	}

	public JSONArray getPagesOfQuery(String queryStr, int scanId)  throws IOException, ParseException, InvalidTokenOffsetsException {
		BookDir bookDir = new BookDir(scanId);

		long startTime = System.currentTimeMillis();

		String text = bookDir.getPlainOCR();
		OffsetInfos offsetInfos = getPageStartOffsets(text);

		long stopTime = System.currentTimeMillis();
		System.out.println("Got line numbers in " + (stopTime - startTime)  + "ms.");


		GermanAnalyzer analyzer = new GermanAnalyzer();

		QueryParser queryParser = new QueryParser("fulltext", analyzer);
		Query query = queryParser.parse(queryStr);
		QueryScorer queryScorer = new QueryScorer(query);
		queryScorer.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);

		TokenStream tokenStream = analyzer.tokenStream("fulltext", text);
		CharTermAttribute termAtt = 
			(CharTermAttribute) tokenStream.addAttribute(CharTermAttribute.class);
		OffsetAttribute offsetAtt = 
			(OffsetAttribute) tokenStream.addAttribute(OffsetAttribute.class);


		TokenStream newStream = queryScorer.init(tokenStream);
		if (newStream != null) {
			tokenStream = newStream;
		}
		queryScorer.startFragment(null);

		tokenStream.reset();

		JSONArray pageNumbersList = new JSONArray();
		int startOffset, endOffset;
		for (boolean next = tokenStream.incrementToken(); 
				next && (offsetAtt.startOffset() < Integer.MAX_VALUE); 
				next = tokenStream.incrementToken())
		{
			startOffset = offsetAtt.startOffset();
			endOffset = offsetAtt.endOffset();

			if ((endOffset > text.length()) || (startOffset > text.length()))
			{
				throw new InvalidTokenOffsetsException("Token " + 
						termAtt.toString() + " exceeds length of provided text sized " + text.length());
			}

			float res = queryScorer.getTokenScore();
			if (res > 0.0F && startOffset <= endOffset) {
			//	String tokenText = text.substring(startOffset, endOffset);
				/*System.out.println(tokenText + " is at page " + 
						offsetInfos.findPage(startOffset) + " " +
						startOffset + "-" + endOffset);

						*/
				pageNumbersList.put(offsetInfos.findPage(startOffset));
			}           
		} 

		return(pageNumbersList);
	}
}
