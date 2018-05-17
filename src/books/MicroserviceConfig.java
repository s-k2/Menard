package books;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.nio.file.*;

public class MicroserviceConfig {
	private static int port = 0;
	private static int numOfThreads = 10;
	private static Path durableRoot = Paths.get(".");
	private static Path volatileRoot = Paths.get(".");

	public static void initialize(String[] args) {
		Options options = new Options();
		options.addOption("n", "number-of-threads", true,
						"number of worker-threads of the internal HTTP server");
		options.addOption("p", "port", true,
						"port to bind the server-socket to (default: choose any)");
		options.addOption("d", "durable", true,
						"path to the root-directory of the durable/permanent storage");
		options.addOption("v", "volatile", true,
						"path to the root-directory of the volatile/private storage");
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
			if(cmd.hasOption("n"))
				numOfThreads = Integer.parseInt(cmd.getOptionValue("n"));
			if(cmd.hasOption("p"))
				port = Integer.parseInt(cmd.getOptionValue("p"));
			if(cmd.hasOption("d"))
				durableRoot = Paths.get(cmd.getOptionValue("d"));
			if(cmd.hasOption("v"))
				volatileRoot = Paths.get(cmd.getOptionValue("v"));
		} catch(ParseException e) {
			formatter.printHelp("books", options);
			System.exit(1);
		}		
	}

	public static int getPort() {
		return(port);
	}

	public static int getNumOfThreads() {
		return(numOfThreads);
	}

	public static Path getDurableRoot() {
		return(durableRoot);
	}

	public static Path getVolatileRoot() {
		return(volatileRoot);
	}

	public static String getConfigText() {
		return(String.format("Configuration:\nnumber-of-threads: %d\n\tport: %d\n\tdurable: %s\n\tvolatile: %s", 
				getNumOfThreads(), getPort(), 
				getDurableRoot().toString(), getVolatileRoot().toString()));
	}
}
