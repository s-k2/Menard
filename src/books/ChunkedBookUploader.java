package books;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

// ways to attack this microservice: DoS: put a lot of files with 1gb-size and just put the first and last chunk
public class ChunkedBookUploader extends ChunkedUploader {
	public ChunkedBookUploader(HttpParameters parameters) throws IllegalArgumentException {
		super(parameters);
	}

	public Response getResponse() throws IOException {
		if(getUpload().isComplete()) {
			return(completeUpload());
		} else {
			return(prepareDefaultResponse(getUpload()));
		}
	}

	public Response completeUpload() throws IOException {
		UnassignedZip unassigned = new UnassignedZip(getUpload());
		// TODO: If the constructor above throws an exception, the file will remain
		// on the disk, until normal tidy-up takes place, as it is registered as
		// an ExpiringFile... Is this okay? Or should we do this earlier?
		ExpiringFiles.getInstance().remove(getUserIdentifier());
		ExpiringFiles.getInstance().put(getUpload().getFileId(), unassigned);

		System.out.printf("Completed file '%s'\n", unassigned.getFileId());
		return(prepareContentsResponse(unassigned));
	}

	private JSONResponse prepareContentsResponse(UnassignedZip unassigned) {
		JSONResponse response = new JSONResponse();

		response.put("hasText", unassigned.hasOCR());

		JSONArray range = new JSONArray();
		for(int page : unassigned.getPages()) {
			JSONArray entry = new JSONArray();
			entry.put(page);
			range.put(entry);
		}
		response.put("range", range);
		response.put("id", unassigned.getFileId());

		return(response);
	}

	private JSONResponse prepareDefaultResponse(ChunkedUpload incomplete) {
		JSONResponse response = new JSONResponse();
		response.put("id", incomplete.getFileId());
		return(response);
	}

}
