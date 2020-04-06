package http.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.ByteBuffer;
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

import http.GetRequest;
import http.Packet;
import http.Packet.Builder;


public class Server {
	
	private ServerSocket mServerSocket;
	private DatagramSocket mServerDatagramSocket;
	private String mDirectory;
	private int mCurrentSequenceNumber = 0;
	private final int CLIENT_PORT = 3000;
	private InetAddress mRecvAddress;
	private boolean mFirstDataPacket = true; 
	private int mLastSequenceNumber = 0;
	private long mFinSentTime;
	private boolean mClosing = false;
	private ArrayList<Packet> mInFlightPackets;
	
	private final int SOCKET_TIMEOUT_MS = 1000; 
	private final String STATUS_LINE_200 = "HTTP/1.0 200 OK \r\n";
	private final String STATUS_LINE_403 = "HTTP/1.0 403 Forbidden \r\n";
	private final String STATUS_LINE_404 = "HTTP/1.0 404 Not Found \r\n";
	
	// local port the server is listening on
	public Server(int aPort, String aDirectory, boolean aUDPMode) {
		if (!aUDPMode) {
			try {
				mServerSocket = new ServerSocket(aPort);			
			} catch(IOException e) {
				System.out.print(e.getMessage());
				e.printStackTrace();
			}
		} else {
			try {
				mServerDatagramSocket = new DatagramSocket(aPort);
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				mRecvAddress = InetAddress.getLocalHost();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mInFlightPackets = new ArrayList<Packet>();
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
	
	public void runUDP() {
		byte[] lPacket = new byte[Packet.MAX_LEN];
		DatagramPacket lDatagramPacket = new DatagramPacket(lPacket, Packet.MAX_LEN);
		try {
			mServerDatagramSocket.setSoTimeout(SOCKET_TIMEOUT_MS); 
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (!mClosing) {
			try {
				mServerDatagramSocket.receive(lDatagramPacket);
				int lReceivedPacketType = actUponPacketReceived(lDatagramPacket.getData());
				if (lReceivedPacketType == Packet.PACKET_TYPE_FIN_ACK) {
					mClosing = true;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Server timeout on socket receive");
				resendTimeoutPackets();
		
			}
		
		}
		
		System.out.println("closed at " + System.currentTimeMillis());
		
	}
	
	private void resendTimeoutPackets() {
		if (mFinSentTime > 0 && System.currentTimeMillis() > mFinSentTime + 5000) {
			// its been 5 seconds since we sent a fin and still no response,
			// client has shut down
			System.out.println("Did not get FIN-ACK within 5 seconds of sent Fin\nclosing...");
			mClosing = true;
		}
		Iterator<Packet> lIt = mInFlightPackets.iterator();
		while (lIt.hasNext()) {
			Packet lPacket = lIt.next();
			long lPacketStartTime = lPacket.getStartTime();
			if (lPacketStartTime + Packet.PACKET_MAX_WAIT_TIME < System.currentTimeMillis()) {
				// send it back and restart its timer
				sendPacket(lPacket);
			}
		}
	}

	private int actUponPacketReceived(byte[] aPacket) {
		
		ByteBuffer lBuffer = ByteBuffer.wrap(aPacket);
		int lPacketType = lBuffer.get();
		int lReceivedSequenceNumber = lBuffer.getInt();
		// advancing the buffer position for the payload
		lBuffer.getInt();
		lBuffer.getShort();
		

		if (lPacketType == Packet.PACKET_TYPE_SYN) {
			// send SYN ACK
			Packet.Builder lPacketBuilder = new Packet.Builder();
			lPacketBuilder.setType(Packet.PACKET_TYPE_SYN_ACK);
			lPacketBuilder.setSequenceNumber(lReceivedSequenceNumber+1);
			lPacketBuilder.setPeerAddress(mRecvAddress);
			lPacketBuilder.setPortNumber(CLIENT_PORT);
			lPacketBuilder.setPayload(new byte[0]);
			Packet lPacket = lPacketBuilder.create();
			byte[] lPacketContent = lPacket.toBytes();
			DatagramPacket lDatagramPacket = new DatagramPacket(lPacketContent, lPacketContent.length,
					lPacket.getPeerAddress(), 3001);
			try {
				mServerDatagramSocket.send(lDatagramPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			lPacket.setStartTime(System.currentTimeMillis());
			mInFlightPackets.add(lPacket);

		}
		else if (lPacketType == Packet.PACKET_TYPE_ACK) {
			
			ackSentPacket(lReceivedSequenceNumber);
			
			
			
		}
		else if (lPacketType == Packet.PACKET_TYPE_DATA) {
			if (mFirstDataPacket) {
				mInFlightPackets.remove(0); // removing the syn-ack packet
				mFirstDataPacket = false;
			}
			// first acknowledge what the client just sent
			Packet.Builder lPacketBuilder = new Packet.Builder();
			lPacketBuilder.setType(Packet.PACKET_TYPE_ACK);
			lPacketBuilder.setSequenceNumber(lReceivedSequenceNumber+Packet.MAX_PAYLOAD_SIZE);
			lPacketBuilder.setPeerAddress(mRecvAddress);
			lPacketBuilder.setPortNumber(CLIENT_PORT);
			lPacketBuilder.setPayload(new byte[0]);
			Packet lPacket = lPacketBuilder.create();
			sendPacket(lPacket);
			addInFlightList(lPacket);
			
			//mCurrentSequenceNumber = lReceivedSequenceNumber;
			
			// process what the user is requesting
			/* not adapted for post */
			byte[] lRcvPacketPayload = new byte[Packet.MAX_PAYLOAD_SIZE]; // 1013
			lBuffer.get(lRcvPacketPayload, 0, lRcvPacketPayload.length);
			String lPayloadInString = new String(lRcvPacketPayload, StandardCharsets.UTF_8);
			String lRequestLine = lPayloadInString.substring(0, lPayloadInString.indexOf("\r\n"));
			if (lRequestLine.startsWith("GET")) {
				String lURI = GetRequest.getURIfromStringRequestLine(lRequestLine);
				if (lURI.contentEquals("/")) {
					listFilesUDP();
				}
			}
			
			
		}
		
		return lPacketType;
	}
	
	private void ackSentPacket(int lReceivedSequenceNumber) {
	
		boolean lMustSendFin = false;
		Iterator<Packet> lIt = mInFlightPackets.iterator();
		while (lIt.hasNext()) {
			Packet lPacket = lIt.next();
			if (lPacket.getSequenceNumber() + Packet.MAX_PAYLOAD_SIZE == lReceivedSequenceNumber) {
				lIt.remove();
				if (mLastSequenceNumber == lReceivedSequenceNumber) {
					// client acknowledged last packet server sent
					lMustSendFin = true;
				}
			}
		}
		
		if (lMustSendFin) {
			System.out.println("Client acknowledged last packet, now sending fin");
			sendFin();
			mFinSentTime = System.currentTimeMillis();
		}
		
		
		
	}

	private void sendFin() {
		Packet.Builder lPB = new Packet.Builder();
		lPB.setType(Packet.PACKET_TYPE_FIN);
		lPB.setPeerAddress(mRecvAddress);
		lPB.setPortNumber(CLIENT_PORT);
		lPB.setSequenceNumber(mCurrentSequenceNumber);
		lPB.setPayload(new byte[0]);
		Packet lPacket = lPB.create();
		sendPacket(lPacket);
		addInFlightList(lPacket);
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
	
	private void listFilesUDP() {
		
		// mon packet payload max size 1013
		// i need to reserve some bytes for the http headers
		int lReservedHeaderSpace = 72; // empirically determined
		String lBody = "";
		/* making the body */
		File lFolder = new File(mDirectory);
		File[] lListOfFiles = lFolder.listFiles();

		for (File lFile : lListOfFiles) {
	        lBody += lFile.getName() + "\r\n";
		}
		byte[] lBodyBytes = lBody.getBytes();
		int lBodyLength = lBodyBytes.length; 
		ByteBuffer lBodyBuffer = ByteBuffer.wrap(lBodyBytes);
		long lBodyRead = 0;
		
		while (lBodyRead < lBodyLength) {
			int lToReadFromBuffer = lBodyBuffer.remaining() % (Packet.MAX_PAYLOAD_SIZE - lReservedHeaderSpace);
			Packet lPacket = makePacketListFiles(lBodyBuffer, lToReadFromBuffer);
			lBodyRead += lToReadFromBuffer;
			sendPacket(lPacket);
			addInFlightList(lPacket);
			
		}
		
		
		mLastSequenceNumber = mCurrentSequenceNumber;

	}
			
		
	private void addInFlightList(Packet aPacket) {
		mInFlightPackets.add(aPacket);
		
	}

	private Packet makePacketListFiles(ByteBuffer aSrc, int aToReadFromBuffer) {
		Packet.Builder lPacketBuilder = new Packet.Builder();
		lPacketBuilder.setType(Packet.PACKET_TYPE_DATA);
		lPacketBuilder.setSequenceNumber(mCurrentSequenceNumber); 
		mCurrentSequenceNumber = mCurrentSequenceNumber + Packet.MAX_PAYLOAD_SIZE;
		//mCurrentSequenceNumber = mCurrentSequenceNumber + Packet.MAX_LEN;
		
		lPacketBuilder.setPeerAddress(mRecvAddress);
		lPacketBuilder.setPortNumber(CLIENT_PORT);
		ByteBuffer lPayloadBuffer = ByteBuffer.allocate(Packet.MAX_PAYLOAD_SIZE);
		StringBuilder lHttpHeaders = new StringBuilder();
		lHttpHeaders.append(STATUS_LINE_200)
		.append("Content-Length:"+aToReadFromBuffer+"\r\n")
		.append("Content-Type:text/plain\r\n")
		.append("\r\n");
		lPayloadBuffer.put(lHttpHeaders.toString().getBytes());
		byte[] lPayload = new byte[aToReadFromBuffer];
		aSrc.get(lPayload);
		lPayloadBuffer.put(lPayload);
		lPacketBuilder.setPayload(lPayloadBuffer.array());
		return lPacketBuilder.create();
		
		
	}
	
	private void sendPacket(Packet aPacket) {
		DatagramPacket lDP = new DatagramPacket(aPacket.toBytes(), aPacket.toBytes().length
				, mRecvAddress, 3001);
		try {
			mServerDatagramSocket.send(lDP);
			// set start time of packet
			aPacket.setStartTime(System.currentTimeMillis());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
