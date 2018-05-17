package books;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AssignedZip extends UnassignedZip {
	public AssignedZip(UnassignedZip unassigned, BookDir bookDir) throws IOException {
		super(bookDir.getPath().resolve(unassigned.getFileId()));

		this.entryNamesToPages = unassigned.entryNamesToPages;
		this.ocrEntry = unassigned.ocrEntry;

		System.out.printf("Assigning '%s' to %s\n", getFileId(), bookDir.toString());
		Files.move(unassigned.getPath(), getPath(), ATOMIC_MOVE);
	}

	public void extract() {
		try(ZipInputStream zip = new ZipInputStream(Files.newInputStream(getPath()))) {
			extractZip(zip);
			zip.close();
			deleteFile();
		} catch(IOException e) {
			System.out.printf("Problems while extracting '%s'\n", getFileId());
			e.printStackTrace();
		}
	}

	private void extractZip(ZipInputStream zip) throws IOException {
		ZipEntry entry = zip.getNextEntry();

		while(entry != null) {
			if(!entry.isDirectory()) {
				Path destinationPath = getPathForZipped(entry.getName());
				if(destinationPath == null && entry.getName() == ocrEntry)
					destinationPath = getPath().getParent().resolve("ocr.txt");

				if(destinationPath != null)
					extractSingleFile(zip, destinationPath);
			}

			zip.closeEntry();
			entry = zip.getNextEntry();
		}
	}

	private Path getPathForZipped(String zippedName) {
		if(zippedName.equals(ocrEntry))
			return(getPath().getParent().resolve("ocr.txt"));

		Integer pageNum = entryNamesToPages.get(zippedName);
		if(pageNum == null)
			return(null);

		return(getPath().getParent().resolve(String.format("page%05d.jpg", pageNum)));
	}

	private void extractSingleFile(ZipInputStream zip, Path path) throws IOException {
		final int BufferSize = 1024 * 128; // 128k

		OutputStream output = Files.newOutputStream(path);
		try {
			byte[] buffer = new byte[BufferSize];
			int bytesRead;

			while((bytesRead = zip.read(buffer)) != -1)
				output.write(buffer, 0, bytesRead);
		} finally {
			output.close();
		}
	}
}
