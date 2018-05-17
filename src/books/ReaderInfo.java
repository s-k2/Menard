package books;

import java.util.function.*;
import java.util.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import javax.imageio.*;
import org.json.*;

public class ReaderInfo {
	JSONObject readerInfo;
	BookDir bookDir;
	int maxPageNum = 0;

	public ReaderInfo(BookDir bookDir) {
		this.bookDir = bookDir;
		readerInfo = new JSONObject();
		JSONArray pagesInfo = new JSONArray();

		bookDir.iterateAllScanImages((Path path) -> pagesInfo.put(addPage(path)));

		readerInfo.put("pages", pagesInfo);
		readerInfo.put("maxPageNum", maxPageNum);
	}

	public void write() throws IOException {
		Files.write(bookDir.getReaderInfoPath(), readerInfo.toString().getBytes("utf-8"));
	}

	private JSONObject addPage(Path imagePath) {
		JSONObject pageInfo = new JSONObject();

		try {
			BufferedImage img = ImageIO.read(imagePath.toFile());
			if(img == null) 
				throw new RuntimeException("Could not determine size of image " + imagePath.toString());

			pageInfo.put("width", img.getWidth());
			pageInfo.put("height", img.getHeight());
			pageInfo.put("file", bookDir.getPublicUrl(imagePath));
			pageInfo.put("pageNumber", getPageNumber(imagePath));
		} catch(IOException e) {
			throw new RuntimeException("Could not determine size of image " + imagePath.getFileName());
		} 

		return(pageInfo);
	}

	private int getPageNumber(Path imagePath) {
		String filename = imagePath.getFileName().toString();

		int pageNum = 0;
		try {
			pageNum = Integer.parseInt(filename.substring(4, 9)); // 4 = sizeof("page"), 9=sizeof("00012")-4
		} catch(NumberFormatException e) {
			throw new RuntimeException("Filename of image " + imagePath.toString() + " is in wrong format...\n");
		}

		if(pageNum > maxPageNum)
			maxPageNum = pageNum;

		return(pageNum);
	}
}
