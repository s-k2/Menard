package manager;

import java.nio.file.*;

import org.apache.commons.cli.*;

class Config {
	static Path binDir = Paths.get("bin").toAbsolutePath();
	static Path volatileRoot = Paths.get("volatile").toAbsolutePath();
	static Path durableRoot = Paths.get("webspace").toAbsolutePath();

	public static void initialize(String[] args) {
		Options options = new Options();
		options.addOption("b", "bin-dir", true,
						"directory of the binary-files");
		options.addOption("d", "durable", true,
						"path to the root-directory of the durable/permanent storage");
		options.addOption("v", "volatile", true,
						"path to the root-directory of the volatile/private storage");
		
		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd;
		
		try {
			cmd = parser.parse(options, args);
			if(cmd.hasOption("b"))
				binDir = Paths.get(cmd.getOptionValue("b"));
			if(cmd.hasOption("d"))
				durableRoot = Paths.get(cmd.getOptionValue("d"));
			if(cmd.hasOption("v"))
				volatileRoot = Paths.get(cmd.getOptionValue("v"));
		} catch(ParseException e) {
			formatter.printHelp("manager", options);
			System.exit(1);
		}		
	}

	public static Path getBinDir() {
		return(binDir);
	}

	public static Path getVolatileRoot() {
		return(volatileRoot);
	}

	public static Path getDurableRoot() {
		return(durableRoot);
	}

	public static String getConfigText() {
		return(String.format("Configuration:\n\tbin-dir: %s\n\tdurable: %s\n\tvolatile: %s\n", 
				getBinDir().toString(),
				getDurableRoot().toString(),
				getVolatileRoot().toString()));
	}

}
