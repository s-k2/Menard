package books;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.json.*;

public class JSONResponse extends JSONObject implements Response {
	private byte jsonString[];
	private int httpCode;

	public JSONResponse() {
		httpCode = 200;
	}

	public JSONResponse(int httpCode) {
		this.httpCode = httpCode;
	}

	@Override
	public String getContentType() {
		return("application/json");
	}

	@Override
	public String getContentDisposition() {
		return(null);
	}

	@Override
	public int getLength() {
		jsonString = toString().getBytes(StandardCharsets.UTF_8);
		return(jsonString.length);
	}

	@Override
	public int getCode() {
		return(httpCode);
	}

	@Override
	public void send(OutputStream output) throws IOException {
		output.write(jsonString);
	}
}
