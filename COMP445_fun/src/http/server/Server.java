package http.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.FileWriter;
import java.net.ServerSocket;
import java.net.Socket;


public class Server {
	
	private ServerSocket mServerSocket;
	private String mDirectory;
	
	private final String STATUS_LINE_200 = "HTTP/1.0 200 OK \r\n";
	private final String STATUS_LINE_403 = "HTTP/1.0 403 Forbidden \r\n";
	private final String STATUS_LINE_404 = "HTTP/1.0 404 Not Found \r\n";
	
	public Server(int aPort, String aDirectory) {
		try {
			mServerSocket = new ServerSocket(aPort);			
		} catch(IOException e) {
			System.out.print(e.getMessage());
			e.printStackTrace();
		}
		mDirectory = aDirectory;
		// does the specified directory exist
		File lDir = new File(mDirectory);
		if (!lDir.exists()) {
			lDir.mkdir();
		}
	}
	
	public void run() {
		while (true) {
			try {
				Socket lSocket = mServerSocket.accept();
				InputStream lInputstream = lSocket.getInputStream();
				OutputStream lOutputStream = lSocket.getOutputStream();
			    Writer lWriter = new BufferedWriter(new OutputStreamWriter(lOutputStream));
				// determine what is requested from the user
				BufferedReader lBuffReader = new BufferedReader(new InputStreamReader(lInputstream));
				String lRequestLine = lBuffReader.readLine();
				String lResourcePath = getPathfromRequestLine(lRequestLine);
				if (lRequestLine.contains("GET")) {
					if (lResourcePath.contentEquals("/")) {
						// write all the files in the response to be sent
						getFiles(lWriter);
					} else {
						getFile(lWriter, lResourcePath);
					}
				} else if (lRequestLine.contains("POST")) {
					writeFile(lWriter, lResourcePath, lBuffReader);
				}
				
				lSocket.close();
			} catch(IOException e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	// requirement 1 
	private void getFiles(Writer aWriter) {
		String lBody = "";
		try {
			/* making the body */
			File lFolder = new File(mDirectory);
			File[] lListOfFiles = lFolder.listFiles();

			for (File lFile : lListOfFiles) {
		        lBody += lFile.getName() + "\r\n";
			}
			int lContentLength = lBody.getBytes().length;
			
			// response writing
			
			aWriter.write(STATUS_LINE_200);
			// add headers here, each line ended with \r\n
			aWriter.write("Content-Length:"+lContentLength+"\r\n");
			aWriter.write("Content-Type:text/plain\r\n");
			aWriter.write("\r\n"); // end of headers 
			aWriter.write(lBody); // body
			aWriter.flush();
		} catch (IOException e) {
			System.out.print(e.getMessage());
			e.printStackTrace();
		}
	}
	
	// requirement 2
	private void getFile(Writer aWriter, String aResourcePath) {
		// does the client try to access files outside of current working dir?
		try {			
			if (aResourcePath.contains("..")) {
				forbiddenRequest(aWriter);
			} else {
				String lCanonicalName = mDirectory + aResourcePath;
				File lResource = null;
				try {
					lResource = new File(lCanonicalName);	
					int lFileLength = (int) lResource.length();
					char[] lBuffer = new char[lFileLength];
					FileReader fileReader = new FileReader(lResource);
					aWriter.write(STATUS_LINE_200);
					aWriter.write("Content-Length:"+lFileLength+"\r\n");
					aWriter.write("Content-Type:text/plain\r\n");
					aWriter.write("\r\n"); // end of headers
		            BufferedReader bufferedReader = new BufferedReader(fileReader);
		           
		            bufferedReader.read(lBuffer);
		            	
		            aWriter.write(lBuffer);
		            aWriter.flush();
		            bufferedReader.close();
				} catch (FileNotFoundException e) {
					aWriter.write(STATUS_LINE_404);
					aWriter.write("\r\n"); // end of headers 
					aWriter.flush();
				}

			}
		} catch (IOException e) {
			System.out.print(e.getMessage());
        	e.printStackTrace();
		}
	}
	
	// requirement 3
	private void writeFile(Writer aWriter, String aResourcePath, BufferedReader aBufferedReader) {
		try {
			if (aResourcePath.contains("..")) {
				forbiddenRequest(aWriter);
			}
			else {
				String lLine;
				do {
					lLine = aBufferedReader.readLine();
				}
				while (!lLine.startsWith("Content-Length") && lLine != null);
				if (lLine.startsWith("Content-Length")) {
					
					int lContentLength = Integer.parseInt(lLine.substring(15));
					do {
						lLine = aBufferedReader.readLine();
					}
					while (!lLine.contentEquals(""));
					FileWriter lFileWriter = new FileWriter(mDirectory + aResourcePath);
					if (lLine.contentEquals("")) {						
						char[] lBody = new char[lContentLength];
						int lRead = aBufferedReader.read(lBody);
						lFileWriter.write(lBody);
					}
					aWriter.write(STATUS_LINE_200);
					String lMessageBody = "file successfully written!";
					lContentLength = lMessageBody.getBytes().length; // reusing the var for another assign
					aWriter.write("Content-Length:"+lContentLength+"\r\n");
					aWriter.write("Content-Type:text/plain\r\n");
					aWriter.write("\r\n"); // end of headers
					aWriter.write(lMessageBody);
					aWriter.flush();
					lFileWriter.close();
					
					
				} else {
					// can't process request, missing content length header
					// -- > I automatically add Content-Length header on my client app
				}
			}
		} catch(IOException e) {
			System.out.print(e.getMessage());
        	e.printStackTrace();
		}
	}
			
		
	// helpers section
	private String getPathfromRequestLine(String aRequestLine) {
		int lStartingIndex = aRequestLine.indexOf(' ');
		int lEndIndex = aRequestLine.indexOf(' ', lStartingIndex + 1);
		String lPath = aRequestLine.substring(lStartingIndex+1, lEndIndex);
		return lPath;
		
	}
	
	private void forbiddenRequest(Writer aWriter) throws IOException {
		aWriter.write(STATUS_LINE_403);
		String lMessageBody = "You are not allowed to have '..' in the request URI";
		int lContentLength = lMessageBody.getBytes().length;
		aWriter.write("Content-Length:"+lContentLength+"\r\n");
		aWriter.write("Content-Type:text/plain\r\n");
		aWriter.write("\r\n"); // end of headers
		aWriter.write(lMessageBody);
		aWriter.flush();
	}

	
}
