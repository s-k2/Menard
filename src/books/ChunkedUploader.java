package books;

import java.lang.IllegalArgumentException;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

// ways to attack this microservice: DoS: put a lot of files with 1gb-size and just put the first and last chunk
public abstract class ChunkedUploader implements PathHandler {
	private String identifier;
	private int chunkNumber;
	private int chunkSize;
	private int totalSize;
	private ChunkedUpload upload;

	public ChunkedUploader(HttpParameters parameters) throws IllegalArgumentException {
		identifier = parameters.getString("resumableIdentifier");

		chunkNumber = parameters.getInt("resumableChunkNumber") - 1;
		if(chunkNumber < 0)
			throw new IllegalArgumentException("resumableChunkNumber out of bounds");

		totalSize = parameters.getInt("resumableTotalSize");
		// normal size of a chunk, but this acutal chunk may differ
		chunkSize = parameters.getInt("resumableChunkSize"); 
		if(chunkSize <= 0)
			throw new IllegalArgumentException("resumableChunkSize out of bounds");
	}

	public void setInputStream(InputStream input) throws IOException {
		upload = findUpload();
		upload.writeChunk(chunkNumber, input);
	}

	private ChunkedUpload findUpload() {
		ExpiringFile upload = ExpiringFiles.getInstance().find(identifier);

		if(upload == null)
			return(createNewUpload());
		else if(upload instanceof ChunkedUpload)
			return((ChunkedUpload) upload);
		else
			throw new IllegalArgumentException(identifier + " does not reference a chunked-upload");
	}

	private ChunkedUpload createNewUpload() {
		ChunkedUpload upload = new ChunkedUpload(identifier, totalSize, chunkSize);
		ExpiringFiles.getInstance().put(identifier, upload);

		System.out.printf("Got new upload '%s', saved as %s\n", identifier, upload.getFileId());

		return(upload);
	}

	protected ChunkedUpload getUpload() {
		return(upload);
	}

	protected String getUserIdentifier() {
		return(identifier);
	}
}
