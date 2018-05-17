package books;

import java.io.IOException;
import java.util.Map;

public class ChunkedOCRUploader extends ChunkedUploader {
	int bookId;

	public ChunkedOCRUploader(HttpParameters parameters) throws IllegalArgumentException {
		super(parameters);

		bookId = parameters.getInt("bookId");
	}

	public Response getResponse() throws IOException {
		if(getUpload().isComplete()) {
			BookInfo info = new BookInfo(bookId);
			info.replaceOCR(getUpload().getPath());
			ExpiringFiles.getInstance().remove(getUserIdentifier());
		} 

		JSONResponse response = new JSONResponse();
		response.put("state", "ok");
		return(response);
	}
}
