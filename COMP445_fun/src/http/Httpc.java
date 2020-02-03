package http;

import java.io.BufferedWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.cli.*;

public class Httpc {

	public static void main(String[] aArgs) {
		// getting the first argument
		String lArg0 = "";
		try {
			lArg0 = aArgs[0]; // get | post | help
		} catch(ArrayIndexOutOfBoundsException aE) {
			System.out.print(
					"ERROR -- no arguments give -- \n\n"+
					"httpc is a curl-like application but supports HTTP protocol only.\n" + 
					"Usage:\n" + 
					" httpc command [arguments]\n" + 
					"The commands are:\n" + 
					" get executes a HTTP GET request and prints the response.\n" + 
					" post executes a HTTP POST request and prints the response.\n" + 
					" help prints this screen.\n" + 
					"Use \"httpc help [command]\" for more information about a command."
					);
			System.exit(1);
		}
		
		if (lArg0.equals("help")) {
			/* 
			 * 2 cases, either we have a command as
			 * a second argument OR
			 * we want general help
			 */
			String lArg1 = "";
			try {
				lArg1 = aArgs[1];
			} catch (ArrayIndexOutOfBoundsException aE) {
				// we only have one argument print general message and exit
				System.out.print(
						"httpc is a curl-like application but supports HTTP protocol only.\n" + 
						"Usage:\n" + 
						" httpc command [arguments]\n" + 
						"The commands are:\n" + 
						" get executes a HTTP GET request and prints the response.\n" + 
						" post executes a HTTP POST request and prints the response.\n" + 
						" help prints this screen.\n" + 
						"Use \"httpc help [command]\" for more information about a command."
						);
				System.exit(0);
			}
			
			if (lArg1.equals("get")) {
				System.out.print(
						"usage: httpc get [-v] [-h key:value] URL\n" + 
						"Get executes a HTTP GET request for a given URL.\n" + 
						" -v Prints the detail of the response such as protocol, status,\n" + 
						"and headers.\n" + 
						"-h key:value Associates headers to HTTP Request with the format\n" + 
						"'key:value'."
						);
			}
			
			else if (lArg1.equals("post")) {
				System.out.print(
						"usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL\n" + 
						"Post executes a HTTP POST request for a given URL with inline data or from\n" + 
						"file.\n" + 
						"-v Prints the detail of the response such as protocol, status,\n" + 
						"and headers.\n" + 
						"-h key:value Associates headers to HTTP Request with the format\n" + 
						"'key:value'.\n" + 
						"-d string Associates an inline data to the body HTTP POST request.\n" + 
						"-f file Associates the content of a file to the body HTTP POST\n" + 
						"request.\n" + 
						"Either [-d] or [-f] can be used but not both."
						);
			}
			
			System.exit(0);
		}
		
		// no help
		if (!lArg0.equals("get") && !lArg0.equals("post")){
			// print help and exit
			System.out.print(
					"ERROR -- invalid command --\n\n" +
					"httpc is a curl-like application but supports HTTP protocol only.\n" + 
					"Usage:\n" + 
					" httpc command [arguments]\n" + 
					"The commands are:\n" + 
					" get executes a HTTP GET request and prints the response.\n" + 
					" post executes a HTTP POST request and prints the response.\n" + 
					" help prints this screen.\n" + 
					"Use \"httpc help [command]\" for more information about a command."
					);
			System.exit(1);
		}
		
		/// if we got here its because we have either httpc get or httpc post
		// now processing the rest of the command line
		
		Option lVerboseOption = new Option("v", "verbose");
		Option lHeadersOption = Option.builder("h").argName("k:v").hasArgs().valueSeparator(':').build();
		Option lDataOption = Option.builder("d").argName("inline-data").hasArg().build();
		Option lFileOption = Option.builder("f").argName("file").hasArg().build();
		Options lOptions = new Options();
		lOptions.addOption(lVerboseOption);
		lOptions.addOption(lHeadersOption);
		lOptions.addOption(lDataOption);
		lOptions.addOption(lFileOption);
		// create the parser
	    CommandLineParser lParser = new DefaultParser();
	    CommandLine lCommandLine = null;
	    
	    String lUrl = aArgs[aArgs.length-1]; // url is the last argument 
	    
	    InetAddress lAddress = null;
	    try {
	    	lAddress = InetAddress.getByName(Request.getHostFromURL(lUrl));	    	
	    } catch (UnknownHostException aE) {
	    	System.out.println(aE.getMessage());
	    	System.exit(1);
	    }
	    
	    Socket lSocket = null;
	    try {
			lSocket = new Socket(lAddress, Request.PORT);
		} catch (IOException aE) {
			// TODO Auto-generated catch block
			aE.printStackTrace();
		}
	    
	    OutputStream lOut = null;
	    try {
			lOut = lSocket.getOutputStream();
		} catch (IOException aE) {
			// TODO Auto-generated catch block
			aE.printStackTrace();
		}
	    
	    Writer lWriter = new BufferedWriter(new OutputStreamWriter(lOut));
	    
	    try {
	        // parse the command line arguments
	    	lCommandLine = lParser.parse( lOptions, aArgs );
	    }
	    catch( ParseException aE ) {
	        // oops, something went wrong
	        System.err.println( "Parsing failed.  Reason: " + aE.getMessage() );
	        System.exit(1);
	    }
		
		
		if (lArg0.equals("get")) {
			//httpc get 'http://httpbin.org/get?course=networking&assignment=1'
			//usage: httpc get [-v] [-h key:value] URL
			GetRequest lReq = new GetRequest();
			lReq.setURI(Request.getPathFromUrl(lUrl));
			setHeadersOnRequest(lCommandLine, lReq);
			lReq.execute(lWriter, false);
			
			Response lResponse = null;
			try {
				lResponse = new Response(lSocket.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
			lResponse.print(lCommandLine.hasOption('v'));
			
		}
		
		else if (lArg0.equals("post")) {
			// do post
			PostRequest lReq = new PostRequest();
			lReq.setURI(Request.getPathFromUrl(lUrl));
			setHeadersOnRequest(lCommandLine, lReq);
			if (lCommandLine.hasOption('d') && lCommandLine.hasOption('f')) {
				System.out.println("ERROR -- cannot have both '-d' and '-f' option"+
									"for a post request");
				System.exit(1);
			}
			// question to self: can we have a post request with empty response body? i think so
			
			if (lCommandLine.hasOption('d')) {
				lReq.setBody(lCommandLine.getOptionValue('d'));
			}
			else if (lCommandLine.hasOption('f')) {
				// todo
			}
			lReq.execute(lWriter, false);
			
			Response lResponse = null;
			try {
				lResponse = new Response(lSocket.getInputStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
			lResponse.print(lCommandLine.hasOption('v'));
			
			
		}
	

	}
	
	// helper
	private static void setHeadersOnRequest(CommandLine aCommandLine, Request aRequest) {
		String[] lHeadersKeyVal = aCommandLine.getOptionValues('h');
		if (lHeadersKeyVal != null) {				
			for (int i = 0; i < lHeadersKeyVal.length; i = i + 2) {
				aRequest.setHeader(lHeadersKeyVal[i], lHeadersKeyVal[i+1]);
			}
		}
	}

}
