package manager;

import java.io.*;
import java.nio.file.*;
import java.lang.IllegalArgumentException;
import java.util.regex.*;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class RunningService implements Runnable {
	private Thread readerThread;
	private Process serviceProcess;
	private BufferedWriter remoteStdin;
	private boolean isRunning;
	private ServiceInfo serviceInfo;

	static public class Line {
		private final long time;
		private final String line;

		public Line(String line) {
			this.line = line;
			time = System.currentTimeMillis();
		}

		public String getLine() {
			return(line);
		}

		public long getTime() {
			return(time);
		}
	}
	private Line lastLine = null;

	public RunningService(ServiceInfo info) {
		this.serviceInfo = info;
		System.out.printf("Start process %s\n", info.getCommandline());

		tryExecProcess(info.getCommandline());
		setRunning(true);
		lastLine = new Line(""); // assert that getLastLine() will never be null

		startReaderThread();
	}

	private void tryExecProcess(List<String> command) {
		try {
			execProcess(command);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void execProcess(List<String> command) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectErrorStream(true);

		serviceProcess = builder.start();
		remoteStdin = new BufferedWriter(new OutputStreamWriter(serviceProcess.getOutputStream()));
	}

	private void startReaderThread() {
		readerThread = new Thread(this);
		readerThread.start();
	}

	@Override
	public void run() {
		tryReadOutput();

		System.out.printf("Process %s ended\n", serviceInfo.getName());
	}

	private void tryReadOutput() {
		try {
			readOutput();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void readOutput() throws IOException {
		BufferedReader remoteStdout = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()));

		String line;
		while((line = remoteStdout.readLine()) != null) {
			if(!line.startsWith("Alive "))
				System.out.println(serviceInfo.getName() + ": " + line);
			storeLine(line);
		}

		// process ended, so we're done
		remoteStdout.close();
		remoteStdin.close();
		setRunning(false);
	}

	private synchronized void storeLine(String line) {
		lastLine = new Line(line);
	}

	public synchronized Line getLastLine() {
		return(lastLine);
	}

	private synchronized void setRunning(boolean newValue) {
		isRunning = newValue;
	}

	public synchronized boolean isRunning() {
		return(isRunning);
	}

	public void terminate() {
		if(!isRunning())
			return;

		serviceProcess.destroy();

		// TODO: What if the thread itself hangs? We cannot use Thread.stop() or interrupt()
	}


	public void sendLine(String line) throws IOException {
		remoteStdin.write(line, 0, line.length());
		remoteStdin.newLine();
		remoteStdin.flush();
	}

}

class JarVersion {
	Path path;
	int major;
	int minor;
	int build;

	public JarVersion(Path path) throws IllegalArgumentException {
		this.path = path;
		parseVersion();
	}

	private void parseVersion() throws IllegalArgumentException {
		Pattern pattern = Pattern.compile("-(\\d+)\\.(\\d+)-(\\d+)\\.jar$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(path.getFileName().toString());

		if(!matcher.find())
			throw new IllegalArgumentException("Invalid naming of jar-file");

		major = Integer.parseInt(matcher.group(1));
		minor = Integer.parseInt(matcher.group(2));
		build = Integer.parseInt(matcher.group(3));
	}

	public boolean isGreater(JarVersion other) {
		return(this.major > other.major || 
				(this.major == other.major && this.minor > other.minor) || 
				(this.major == other.major && this.minor == other.minor && this.build > other.build));
	}

	public Path getPath() {
		return(path);
	}
}

class ServiceInfo {
	String name;
	Path path;

	// assert that name only contains lower-case, alphanum-characters! NO DASHES!
	public ServiceInfo(String name) {
		this.name = name;
		this.path = findNewestJar();
		System.out.printf("Newest jar: %s\n", path.toString());
	}

	public List<String> getCommandline() {
		return(Arrays.asList("java", 
					"-jar", path.toString(), 
					"-p", "8080", 
					"-v", Config.getVolatileRoot().toString(),
				       	"-d", Config.getDurableRoot().toString()));
	}

	public String getName() {
		return(name);
	}

	private Path findNewestJar() {
		JarVersion highestVersion = null;
		String glob = name + "-*.*-*.jar";

		try(DirectoryStream<Path> stream = Files.newDirectoryStream(Config.getBinDir(), glob)) {
			for(Path file : stream) {
				highestVersion = getHighestVersion(highestVersion, file);
			}
		} catch(IOException | DirectoryIteratorException e) {
			throw new RuntimeException("Could not iterate the files in " + Config.getBinDir().toString());
		}

		if(highestVersion == null)
			throw new RuntimeException("Could not find service '" + name + 
					"' in bin-dir '" + Config.getBinDir().toString() + "'\n");

		return(highestVersion.getPath());
	}

	private JarVersion getHighestVersion(JarVersion highest, Path path) {
		JarVersion maybeHigher = new JarVersion(path);

		if(highest == null)
			return(maybeHigher);
		else if(maybeHigher.isGreater(highest))
			return(maybeHigher);
		else
			return(highest);
	}


}

class Communicator {
	RunningService service;

	public Communicator(RunningService service) {
		this.service = service;
	}

	public boolean isOk() {
		RunningService.Line line = service.getLastLine();

		if(!service.isRunning())
			return(false);
		if(System.currentTimeMillis() - line.getTime() > TimeUnit.SECONDS.toMillis(30)) {
			System.out.printf("Timeout of service, terminating it now\n");
			service.terminate();
			return(false);
		}
		return(true);
	}

	public void shutdown() {
		sendCommand("down");
	}

	private void sendCommand(String command) {
		try {
			service.sendLine(command);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}

public class Main {
	public static void main(String[] args) throws InterruptedException {
		Config.initialize(args);
		System.out.println(Config.getConfigText());

		while(true) {
			runSingleService("books");
		}
	}

	private static void runSingleService(String name) throws InterruptedException {
		System.out.printf("Starting service '%s'\n", name);

		ServiceInfo info = new ServiceInfo(name);
		RunningService service = new RunningService(info);
		Communicator communicator = new Communicator(service);

		while(communicator.isOk()) {
			Thread.sleep(5000);
		}

		System.out.printf("Service '%s' ended\n", name);
	}
}

