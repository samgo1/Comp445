package http;

import org.apache.commons.cli.CommandLine;

public class Httpc {

	public static void main(String[] args) {
		
		String lArg0 = args[0]; // get | post | help
		
		if (lArg0.equals("help")) {
			/* 
			 * 2 cases, either we have a command as
			 * a second argument OR
			 * we want general help
			 */
			String lArg1 = "";
			try {
				lArg1 = args[1];
			} catch (ArrayIndexOutOfBoundsException e) {
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
			
			else {
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
	
			}
			
			System.exit(0);
			
			
			
		}
		if (!lArg0.equals("get") || !lArg0.equals("post")){
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
		CommandLine line;
		
		
		if (lArg0.equals("get")) {
			// do get
			//httpc get 'http://httpbin.org/get?course=networking&assignment=1'
			// look if verbose is present
			//usage: httpc get [-v] [-h key:value] URL
			
			
		}
		
		if (lArg0.equals("post")) {
			// do post
		}
	

	}

}
