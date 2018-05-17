package books;

import java.io.IOException;
import java.io.OutputStream;

public interface Response {
	public String getContentType();
	public String getContentDisposition();
	public int getLength();
	public int getCode();
	public void send(OutputStream output) throws IOException;
}

