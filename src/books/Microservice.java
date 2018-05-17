package books;

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.*;

abstract class Microservice implements HttpHandler {
	Microservice(String[] args) throws IOException {
		MicroserviceConfig.initialize(args);
		startServer();

		sendSignOfLife();
	}
	
	private void startServer() throws IOException {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", MicroserviceConfig.getPort()), 0);
			server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

			server.createContext("/", this);
			System.out.print("Listening on port: ");
			System.out.println(server.getAddress().getPort());
			
			server.start();
	}

	private void sendSignOfLife() {
		System.out.println(MicroserviceConfig.getConfigText());

		final int SIGN_INTERVAL = 3000;
		while(true) {
			System.out.printf("Alive %d\n", Runtime.getRuntime().totalMemory());
			try {
				Thread.sleep(SIGN_INTERVAL);
			} catch(InterruptedException e) {
			}
		}
	}

	@Override
	public void handle(HttpExchange connection) throws IOException {
		try {
			URI uri = connection.getRequestURI();
			HttpParameters parameters = new HttpParameters(uri);

			PathHandler handler = constructHandler(uri.getPath(), parameters);

			handler.setInputStream(connection.getRequestBody());

			sendResponse(connection, handler.getResponse());
		} catch (Exception e) {
			System.out.println("An exception happened white handlung the connection: " + e.toString());
			sendErrorResponse(connection, e);
		} finally {
			connection.getRequestBody().close();
		}
	}

	abstract protected PathHandler constructHandler(String path, HttpParameters parameters);

	void sendErrorResponse(HttpExchange connection, Exception e) throws IOException {
		JSONResponse response = new JSONResponse(400);
		if(e instanceof IllegalArgumentException)
			response.put("errorMessage", "Invalid argument: \n" + e.getMessage());
		else if(e instanceof IOException)
			response.put("errorMessage", "Error while doing data-transfer: \n" + e.getMessage());
		else if(e.getMessage() != null)
			response.put("errorMessage", e.getMessage());
		else
			response.put("errorMessage", "Unknown error: \nAn unknown error has occuered");
		sendResponse(connection, response);

		System.out.println("Caught exception: " + e.toString());
		e.printStackTrace();
	}

	void sendResponse(HttpExchange connection, Response response) throws IOException {
		connection.getResponseHeaders().set("Content-Type", response.getContentType());
		if(response.getContentDisposition() != null)
			connection.getResponseHeaders().set("Content-Disposition", response.getContentDisposition());
		connection.sendResponseHeaders(response.getCode(), response.getLength());

		OutputStream output = connection.getResponseBody();
		response.send(output);
		output.close();
	}

}
