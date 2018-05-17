package books;

import java.util.UUID;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ChunkedUpload extends ExpiringFile {
	private final String userIdentifier;
	private final int totalSize;
	private final int chunkSize;
	private int chunksReceived[];

	public ChunkedUpload(String userIdentifier, int totalSize, int chunkSize) {
		// use an random uuid as filename to prevent collisions
		// this also prevents us from any attacks that use the identifier to manipulate
		// data on the filesystem somehow
		// ATTENTION: This does not save us from several people uploading to the same file
		super(MicroserviceConfig.getVolatileRoot().resolve(UUID.randomUUID().toString()));

		this.userIdentifier = userIdentifier;
		this.totalSize = totalSize;
		this.chunkSize = chunkSize;
		int expectedNumberOfChunks = (totalSize + chunkSize - 1) / chunkSize; // round up
		chunksReceived = new int[expectedNumberOfChunks];
	}

	public String getUserIdentifier() {
		return(userIdentifier);
	}

	public int getTotalSize() {
		return(totalSize);
	}

	public int getChunkSize() {
		return(chunkSize);
	}

	public synchronized void writeChunk(int chunkNumber, InputStream input) throws IOException {
		int bytesCopied = writeToFile(chunkNumber, input);
		registerChunk(chunkNumber, bytesCopied);
	}

	private synchronized int writeToFile(int chunkNumber, InputStream input) throws IOException {
		try(RandomAccessFile file = new RandomAccessFile(getPath().toString(), "rw")) {
			file.seek(getChunkSize() * chunkNumber);

			final int BufferSize = 1024 * 128; // use 128k blocks
			byte[] buffer = new byte[BufferSize];

			int totalBytesCopied = 0;
			int bytesReadToBuffer;
			while((bytesReadToBuffer = input.read(buffer)) != -1) {
				file.write(buffer, 0, bytesReadToBuffer);
				totalBytesCopied += bytesReadToBuffer;
			}

			return(totalBytesCopied);
		}
	}

	private synchronized void registerChunk(int chunkNumber, int bytesRead) {
		registerActivity();
		chunksReceived[chunkNumber] = bytesRead;
	}

	public synchronized boolean isComplete() {
		int sum = 0;
		for(int current : chunksReceived)
			sum += current;

		return(sum == totalSize);
	}
}


