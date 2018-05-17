package books;

import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.zip.*;
import java.util.Map;

import books.MicroserviceConfig;

public class DownloadBook implements PathHandler {
	private int bookId;

	public DownloadBook(HttpParameters parameters) throws IllegalArgumentException {
		bookId = parameters.getInt("bookId");
	}

	public void setInputStream(InputStream input) throws IOException {
	}

	public Response getResponse() {
		return(new ZipResponse(bookId));
	}
}

class ZipResponse implements Response {
	private int bookId;
	public ZipResponse(int bookId) {
		this.bookId = bookId;
	}

	@Override
	public String getContentType() {
		return("application/zip");
	}

	@Override
	public String getContentDisposition() {
		return(String.format("attachment; filename=\"book-%05d.zip\"", bookId));
	}

	@Override
	public int getLength() {
		return(0);
	}

	@Override
	public int getCode() {
		return(200);
	}

	@Override
	public void send(OutputStream output) throws IOException {
		ZipOutputStream zip = new ZipOutputStream(output);
		zip.setMethod(ZipOutputStream.DEFLATED);
		zip.setLevel(Deflater.NO_COMPRESSION);

		putAllFiles(zip);

		zip.close();
	}

	private void putAllFiles(ZipOutputStream zip) throws IOException {
		Path bookDir = MicroserviceConfig.getDurableRoot().resolve("books").resolve(Integer.toString(bookId));

		try(DirectoryStream<Path> stream = Files.newDirectoryStream(bookDir)) {
			for(Path file : stream) {
				putFile(file, zip);
			}
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	private void putFile(Path file, ZipOutputStream zip) throws IOException {
		ZipEntry entry = new ZipEntry(file.getFileName().toString());

		zip.putNextEntry(entry);
		putFileContents(file, zip);

		zip.closeEntry();
	}

	private void putFileContents(Path source, OutputStream out) throws IOException {
		try(InputStream in = Files.newInputStream(source)) {
			byte[] buffer = new byte[4096];
			int read = 0;
			while((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}
	}
}
