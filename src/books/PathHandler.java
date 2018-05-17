package books;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.json.JSONObject;

public interface PathHandler {
	public void setInputStream(InputStream input) throws IOException;
	public Response getResponse() throws IOException;
}
