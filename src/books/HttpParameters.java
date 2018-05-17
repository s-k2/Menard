package books;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.net.URI;
import java.net.URLDecoder;

class HttpParameters {
	Map<String, String> parameters = new LinkedHashMap<String, String>();

	public HttpParameters(URI uri) throws UnsupportedEncodingException {
		if(uri.getQuery() == null || uri.getQuery().isEmpty())
			return;

		String[] pairs = uri.getQuery().split("&");
		for(String pair : pairs) {
			int index = pair.indexOf("=");
			String key = URLDecoder.decode(index != -1 ? pair.substring(0, index) : pair, "UTF-8");
			if(!parameters.containsKey(key)) {
				parameters.put(key, index > 0 && pair.length() > index + 1 ? 
						URLDecoder.decode(pair.substring(index + 1), "UTF-8") : "");
			}
		}
	}

	public String getString(String searchFor) throws IllegalArgumentException {
		if(!parameters.containsKey(searchFor))
			throw new IllegalArgumentException(searchFor + " is missing");
		return(parameters.get(searchFor));
	}
	

	public int getInt(String searchFor) throws IllegalArgumentException {
		try {
			return(Integer.parseInt(getString(searchFor)));
		} catch(NumberFormatException e) {
			throw new IllegalArgumentException(searchFor + " is not a number");
		}
	}



}
