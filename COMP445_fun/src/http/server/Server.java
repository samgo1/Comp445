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
					writeFile(lWriter, lResourcePath);
				}
				
				lSocket.close();
			} catch(IOException e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	private void getFiles(Writer aWriter) {
		String lBody = "";
		try {
			aWriter.write(STATUS_LINE_200);
			// add headers here, each line ended with \r\n
			aWriter.write("\r\n"); // end of headers 
			/* making the body */
			File lFolder = new File(mDirectory);
			File[] lListOfFiles = lFolder.listFiles();

			for (File lFile : lListOfFiles) {
		        lBody += lFile.getName() + "\r\n";
			}
			aWriter.write(lBody); // body
			aWriter.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getFile(Writer aWriter, String aResourcePath) {
		// does the client try to access files outside of current working dir?
		try {			
			if (aResourcePath.contains("..")) {
				aWriter.write(STATUS_LINE_403);
				aWriter.write("\r\n"); // end of headers 
				aWriter.flush();
			} else {
				String lCanonicalName = mDirectory + aResourcePath;
				File lResource = null;
				try {
					lResource = new File(lCanonicalName);	
					int lFileLength = (int) lResource.length();
					char[] lBuffer = new char[lFileLength];
					aWriter.write(STATUS_LINE_200);
					aWriter.write("\r\n"); // end of headers
					
		            FileReader fileReader = new FileReader(lResource);

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
	
	private void writeFile(Writer aWriter, String aFile) {
		
	}
	
	private String getPathfromRequestLine(String aRequestLine) {
		int lStartingIndex = aRequestLine.indexOf(' ');
		int lEndIndex = aRequestLine.indexOf(' ', lStartingIndex + 1);
		String lPath = aRequestLine.substring(lStartingIndex+1, lEndIndex);
		return lPath;
		
	}
	
	
}
