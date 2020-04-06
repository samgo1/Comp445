package http;

import java.io.BufferedWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.commons.cli.*;


public class Httpc {
	
	private static final int ROUTER_PORT = 3000;
	private static ArrayList<Packet> mInFlightPackets;
	
	public Httpc() {
		mInFlightPackets = new ArrayList<Packet>();
	}

	public static void main(String[] aArgs) {
		boolean lUDPSwitch = false;
		// getting the first argument
		String lArg0 = "";
		try {
			lArg0 = aArgs[0]; // get | post | help
		} catch(ArrayIndexOutOfBoundsException aE) {
			System.out.print(
					"ERROR -- no arguments given -- \n\n"+
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
						"Use \"httpc help [command]\" for more information about a command.\n"
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
						"'key:value'\n."
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
						"Either [-d] or [-f] can be used but not both.\n"
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
					"Use \"httpc help [command]\" for more information about a command.\n"
					);
			System.exit(1);
		}
		
		/// if we got here its because we have either httpc get or httpc post
		// now processing the rest of the command line
		
		Option lVerboseOption = new Option("v", "verbose");
		Option lUDPOption = new Option("UDP", "udp transport"); // ignores other args
		Option lHeadersOption = Option.builder("h").argName("k:v").hasArgs().valueSeparator(':').build();
		Option lPortOption = Option.builder("port").argName("port number").hasArg().build();
		Option lDataOption = Option.builder("d").argName("inline-data").hasArg().build();
		Option lFileOption = Option.builder("f").argName("file").hasArg().build();
		Options lOptions = new Options();
		lOptions.addOption(lVerboseOption);
		lOptions.addOption(lHeadersOption);
		lOptions.addOption(lDataOption);
		lOptions.addOption(lFileOption);
		lOptions.addOption(lPortOption);
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
	    
	    // as soon as we parsed let's find out if we are in the 'UDP' mode
	    if (lCommandLine.hasOption("UDP")) {
	    	lUDPSwitch = true;
	    }
	    
	    String lUrl = aArgs[aArgs.length-1]; // url is the last argument 
	    if (!lUrl.startsWith("http://")) {
			lUrl = "http://" + lUrl;
		}
	    
	    InetAddress lAddress = null;
	    try {
	    	lAddress = InetAddress.getByName(Request.getHostFromURL(lUrl));	    	
	    } catch (UnknownHostException aE) {
	    	System.out.println(aE.getMessage());
	    	System.exit(1);
	    }
	    
	    Socket lSocket = null;
	    if (lUDPSwitch == false) {
	    	// normal mode (TCP)
	    	try {
		    	
				if (lCommandLine.hasOption("port")) {
					lSocket = new Socket(lAddress,
					Integer.parseInt(lCommandLine.getOptionValue("port")));
	    		} else {
	    			lSocket = new Socket(lAddress, Request.PORT);
	    		}
	    	}
		    		
			 catch (IOException aE) {
				System.out.println(aE.getMessage());
				aE.printStackTrace();
				System.exit(1);
				
			}
		    
		    OutputStream lOut = null;
		    try {
				lOut = lSocket.getOutputStream();
			} catch (IOException aE) {
				System.out.println(aE.getMessage());
				aE.printStackTrace();
				System.exit(1);
			}
		    // OutputStreamWriter character -> bytes
		    // BufferedWriter (for efficiency)
		    Writer lWriter = new BufferedWriter(new OutputStreamWriter(lOut));
			
			if (lArg0.equals("get")) {
				//httpc get 'http://httpbin.org/get?course=networking&assignment=1'
				//usage: httpc get [-v] [-h key:value] URL
				GetRequest lReq = new GetRequest();
				lReq.setURI(Request.getPathFromUrl(lUrl));
				setHeadersOnRequest(lCommandLine, lReq);
				lReq.execute(lWriter);
				
				ResponseParser lResponse = null;
				try {
					lResponse = new ResponseParser(lSocket.getInputStream());
				} catch (IOException aE) {
					System.out.println(aE.getMessage());
					aE.printStackTrace();
					System.exit(1);
				}
				lResponse.print(lCommandLine.hasOption('v'));
				
			}
			
			else if (lArg0.equals("post")) {
				// do post
				PostRequest lReq = null;
				try {
					lReq = new PostRequest(lSocket.getOutputStream());
				} catch (IOException aE) {
					System.out.println(aE.getMessage());
					aE.printStackTrace();
					System.exit(1);
				}
				lReq.setURI(Request.getPathFromUrl(lUrl));
				setHeadersOnRequest(lCommandLine, lReq);
				if (lCommandLine.hasOption('d') && lCommandLine.hasOption('f')) {
					System.out.println("ERROR -- cannot have both '-d' and '-f' option"+
										"for a post request");
					System.exit(1);
				}
				// question to self: can we have a post request with empty response body? i think so
				
				if (lCommandLine.hasOption('d')) {
					String lValue = lCommandLine.getOptionValue('d');
					lReq.setHeader("Content-Length", Integer.toString(lValue.getBytes().length));
					lReq.setBody(lValue.getBytes());
					lReq.execute(lWriter);
				}
				else if (lCommandLine.hasOption('f')) {
					String lFilePath = lCommandLine.getOptionValue('f');
					File lFile = new File(lFilePath);
					if (lFile.exists()) {
						FileInputStream lReader = null;
						try {
							lReader = new FileInputStream(lFile);
						} catch (FileNotFoundException e) {
							System.out.println("ERROR -- file not found -- exiting");
							e.printStackTrace();
							System.exit(1);
						}
						try {
							byte[] lData = lReader.readAllBytes(); 
							lReq.setHeader("Content-Length", Integer.toString(lData.length));
							lReq.setBody(lData);
							lReq.execute(lWriter);

						} catch (IOException aE) {
							System.out.println(aE.getMessage());
							aE.printStackTrace();
							System.exit(1);
						}
					} else {
						System.out.println("ERROR -- file does not exist -- exiting");
						System.exit(1);
					}
					
				}
				
				ResponseParser lResponse = null;
				try {
					lResponse = new ResponseParser(lSocket.getInputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.exit(1);
				}
				lResponse.print(lCommandLine.hasOption('v'));	
			}
	    } else {
	    	// udp mode client side
	    	int lMyPort = 3000; // local
	    	int lServerPort = 4000;
	    	InetAddress lLocalHost = null;
			try {
				lLocalHost = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	UDPClient lClient = null;
	    	if (lArg0.equals("get")) {
	    		GetRequest lReq = new GetRequest();
				lReq.setURI(Request.getPathFromUrl(lUrl));
				setHeadersOnRequest(lCommandLine, lReq);
				lClient = new UDPClient(lMyPort, lServerPort, lLocalHost, lReq);
				if (lReq.getURI().equals("/")) {
					lClient.executeGetRoot();
				} else {
					// get file
				}
			
	    	}
	    	else if (lArg0.equals("post")) {
	    		
	    	}
	    	
	    	
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
	
//	private static void executeGetRootUDP(GetRequest aRequest, DatagramSocket aDatagramSocket) {
//		
//		// make a SYNC packet with 0 payload
//		// to start the communication
//		InetAddress lLocalhost = null;
//		try {
//			lLocalhost = InetAddress.getLocalHost();
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		Packet.Builder lPacketBuilder = new Packet.Builder();
//		lPacketBuilder.setType(Packet.PACKET_TYPE_SYN);
//		lPacketBuilder.setSequenceNumber(0);
//		lPacketBuilder.setPeerAddress(lLocalhost);
//		int lServerPort = 4000;
//		lPacketBuilder.setPortNumber(lServerPort);
//	
//		byte[] lEmpty = new byte[0];
//		lPacketBuilder.setPayload(lEmpty);
//		Packet lPacket = lPacketBuilder.create();
//		byte[] lPacketBytes = lPacket.toBytes();
//		DatagramPacket lDatagramToSend = new DatagramPacket(lPacketBytes, lPacketBytes.length,
//				lPacket.getPeerAddress(), lPacket.getPeerPort());
//		try {
//			aDatagramSocket.send(lDatagramToSend);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		/* glue */
//		lPacket.setStartTime(System.currentTimeMillis());
//		mInFlightPackets.add(lPacket);
//		/* end glue */
//		
//		byte[] lDatagramContainer = new byte[Packet.MAX_LEN];
//		DatagramPacket lDatagramToReceive = new DatagramPacket(lDatagramContainer, Packet.MAX_LEN);
//		int lSocketTimeout = 200; 
//		try {
//			aDatagramSocket.setSoTimeout(lSocketTimeout);
//		} catch (SocketException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		while (true) {
//			try {
//				aDatagramSocket.receive(lDatagramToReceive);
//			} catch (IOException e) {
//				
//				System.out.println("Receive timeout");
//				resendTimeOutPackets();
//				e.printStackTrace();
//			}
//			
//			ByteBuffer lByteBuffer = ByteBuffer.wrap(lDatagramContainer);
//			int lPacketType = lByteBuffer.get();
//			if (lPacketType == Packet.PACKET_TYPE_SYN_ACK) {
//				// ack the syn ack
//				lPacketBuilder.setType(Packet.PACKET_TYPE_ACK);
//				int lNextSequenceNumber = lByteBuffer.getInt(1) + 1; 
//				lPacketBuilder.setSequenceNumber(lNextSequenceNumber);
//				lPacket = lPacketBuilder.create();
//				// payload already empty
//				lPacketBytes = lPacket.toBytes();
//				lDatagramToSend = new DatagramPacket(lPacketBytes, lPacketBytes.length,
//						lPacket.getPeerAddress(), lPacket.getPeerPort());
//				try {
//					aDatagramSocket.send(lDatagramToSend);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//				// ask for what I need the list of files
//				lPacketBuilder.setType(Packet.PACKET_TYPE_DATA);
//				lNextSequenceNumber = lNextSequenceNumber + 1;
//				lPacketBuilder.setSequenceNumber(lNextSequenceNumber);
//				// constructing the payload
//				byte[] lPayload = aRequest.assembleRequest().getBytes();
//				if (lPayload.length > Packet.MAX_LEN) {
//					// chunk it
//				}
//				else {
//					lPacketBuilder.setPayload(lPayload);
//				}
//				lPacketBytes = lPacketBuilder.create().toBytes();
//				lDatagramToSend = new DatagramPacket(lPacketBytes, lPacketBytes.length);
//				
//				try {
//					aDatagramSocket.send(lDatagramToSend);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//			}
//		}
//	
//	}
////	
//	private void resentTimeoutPackets() {
//		ArrayList<Packet> lPacketsToReAdd = new ArrayList<Packet>();
//		Iterator<Packet> lIt = mInFlightPackets.iterator();
//		while (lIt.hasNext()) {
//			Packet lPacket = lIt.next();
//			long lPacketStartTime = lPacket.getStartTime();
//			if (lPacketStartTime + Packet.PACKET_MAX_WAIT_TIME < System.currentTimeMillis()) {
//				// packet timeout
//				// remove this packet from the list
//				lIt.remove();
//				// send it back and restart its timer
//				sendPacket(lPacket);
//				lPacketsToReAdd.add(lPacket);
//			}
//		}
//		mInFlightPackets.addAll(lPacketsToReAdd);
//	}
//	

	

}
