package http;

import org.apache.commons.cli.*;

public class TestCmd {

	public static void main(String[] args) {
		Option lHeader = Option.builder("h").argName("k:v").hasArg().build();
		Options options = new Options(); 
		options.addOption(lHeader);
		// create the parser
	    CommandLineParser parser = new DefaultParser();
	    CommandLine line = null;
	    try {
	        // parse the command line arguments
	        line = parser.parse( options, args );
	    }
	    catch( ParseException exp ) {
	        // oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
	    }
		String[] values =line.getOptionValues("h");
	}

}
