package http.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ServerMain {
	
	private static int DEFAULT_PORT = 8080;
	private static String DEFAULT_DIR = System.getProperty("user.dir");

	public static void main(String[] aArgs) {
		Option lVerboseOption = new Option("v", "verbose");
		Option lPortOption = Option.builder("p").argName("port number").hasArg().build();
		Option lDirectoryOption = Option.builder("d").argName("server directory").hasArg().build();
		Option lUDPOption = new Option("udp", "udp mode");
		Options lOptions = new Options();
		lOptions.addOption(lVerboseOption);
		lOptions.addOption(lPortOption);
		lOptions.addOption(lDirectoryOption);
		lOptions.addOption(lUDPOption);
		
		// create the parser
	    CommandLineParser lParser = new DefaultParser();
	    CommandLine lCommandLine = null;
	    
	    try {
	        // parse the command line arguments
	    	lCommandLine = lParser.parse( lOptions, aArgs );
	    }
	    catch( ParseException aE ) {
	        // oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + aE.getMessage() );
	        System.exit(1);
	    }
	    
	    int lServerPort = lCommandLine.hasOption('p')? Integer.parseInt(lCommandLine.getOptionValue('p')): DEFAULT_PORT;
	    String lServerDir = lCommandLine.hasOption('d')? DEFAULT_DIR+ "/" + lCommandLine.getOptionValue('d'): DEFAULT_DIR;
	    if (lCommandLine.hasOption("udp")) {
	    	Server server = new Server(lServerPort, lServerDir, true);
	    	server.runUDP();
	    } else {
	    	Server server = new Server(lServerPort, lServerDir, false);
	    	server.run();		    	
	    }
	}

}
