package http;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import http.Packet.Builder;

public class UDPClient {
	private DatagramSocket mSocket;
	private int mListeningPort;
	private ArrayList<Packet> mInFlightPackets;
	private int mSequenceNumber;
	private InetAddress mPeerAddress;
	private int mPeerPort;
	private boolean mFinAckSent = false;
	private final int SOCKET_TIMEOUT = 200;
	private Request mRequest;
	private boolean mConnectionEstablished;
	private HashMap<Integer, byte[]> mPacketPayload; // holds sequence number -> payload
	
 	
	public UDPClient(int aListeningPort, int aPeerPort, InetAddress aPeerAddress, Request aRequest) {
		mPeerPort = aPeerPort;
		mPeerAddress = aPeerAddress;
		mInFlightPackets = new ArrayList<Packet>();
		mPacketPayload = new HashMap<Integer, byte[]>();
		mListeningPort = aListeningPort;
		mSequenceNumber = 0;
		mRequest = aRequest;
		mConnectionEstablished = false;
		try {
			mSocket = new DatagramSocket(mListeningPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			mSocket.setSoTimeout(SOCKET_TIMEOUT);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	// get server file list
	public void executeGetRoot() {
		// establish communication
		// make the request
		// ack received packets and buffer them
		Packet lPacket = makePacket(Packet.PACKET_TYPE_SYN, null, mSequenceNumber);
		sendPacket(lPacket);
		addToInFlightPackets(lPacket);
		byte[] lDatagramContainer = new byte[Packet.MAX_LEN];
		DatagramPacket lDatagramToReceive = new DatagramPacket(lDatagramContainer, Packet.MAX_LEN);
		while (!mConnectionEstablished) {
			try {
				mSocket.receive(lDatagramToReceive);
				int lPacketType = actUponPacketReceived(lDatagramContainer);
			} catch (IOException e) {
				System.out.println("socket timeout");
				resendTimeoutPackets();
			}
		}
		sendRequest();
		while (!mFinAckSent) {
			try {
				mSocket.receive(lDatagramToReceive);
				actUponPacketReceived(lDatagramContainer);
			} catch (IOException e) {
				System.out.println("socket timeout");
				resendTimeoutPackets();
			}
			
		}
		
		reconstructPayload();
		
		System.out.println("Done.. closing");
		System.out.println(System.currentTimeMillis());
		
	}
	
	private void reconstructPayload() {
		System.out.println("Reconstructing the payload on clientside");
		// get all stored packets
		Iterator lIt = mPacketPayload.entrySet().iterator();
		while (lIt.hasNext()) {
			Map.Entry lME = (Map.Entry) lIt.next();
			byte[] lChunk = (byte[])lME.getValue();
			System.out.println(new String(lChunk));
			
		}
	}
	
	private int actUponPacketReceived(byte[] aPacket) {
		
		ByteBuffer lBuffer = ByteBuffer.wrap(aPacket);
		//int lPacketType = aPacket[0];
		int lPacketType = lBuffer.get();
		//int lReceivedSequenceNumber = lBuffer.getInt(1);
		int lReceivedSequenceNumber = lBuffer.getInt();
		
		if (lPacketType == Packet.PACKET_TYPE_SYN_ACK) {
			if (mConnectionEstablished) {
				return Packet.PACKET_TYPE_SYN_ACK;
			}
			mConnectionEstablished = true;
			mSequenceNumber = mSequenceNumber + 1;
			mInFlightPackets.remove(0); // remove the SYN packet
			return lPacketType;
		}
		
		else if (lPacketType == Packet.PACKET_TYPE_ACK) {
			ackSentPacket(lReceivedSequenceNumber);
		}
		
		else if (lPacketType == Packet.PACKET_TYPE_DATA) {
			// incoming data
			consumeIncomingPacket(lBuffer, lReceivedSequenceNumber);
			sendAcknowledgement(lReceivedSequenceNumber);
			
		}
		
		else if (lPacketType == Packet.PACKET_TYPE_FIN) {
			// make fin ack and send
			Packet.Builder lPacketBuilder = new Packet.Builder();
			lPacketBuilder.setType(Packet.PACKET_TYPE_FIN_ACK);
			lPacketBuilder.setSequenceNumber(lReceivedSequenceNumber+1);
			lPacketBuilder.setPeerAddress(mPeerAddress);
			lPacketBuilder.setPortNumber(mPeerPort);
			byte[] lEmpty = new byte[0];
			lPacketBuilder.setPayload(lEmpty);
			Packet lPacket = lPacketBuilder.create();
			sendPacket(lPacket);
			mFinAckSent = true;
			addToInFlightPackets(lPacket);
		}
		
		
		return lPacketType;
	}
	
	private void consumeIncomingPacket(ByteBuffer aBuffer, int aSequenceNumber) {
		if (!mPacketPayload.containsKey(aSequenceNumber)) {
			byte[] lPayload = new byte[Packet.MAX_PAYLOAD_SIZE];
			aBuffer.get(11, lPayload);
			mPacketPayload.put(aSequenceNumber, lPayload);			
		}
		
	}
	
	// my packets
	private void ackSentPacket(int lReceivedSequenceNumber) {
		
		Iterator<Packet> lIt = mInFlightPackets.iterator();
		while (lIt.hasNext()) {
			Packet lPacket = lIt.next();
			if (lPacket.getSequenceNumber() + Packet.MAX_PAYLOAD_SIZE == lReceivedSequenceNumber) {
				lIt.remove();
			}
		}
		
	}
	
	private void sendAcknowledgement(int aSequenceNumber) {
		Packet lPacket = makePacket(Packet.PACKET_TYPE_ACK, null, aSequenceNumber+Packet.MAX_PAYLOAD_SIZE);
		sendPacket(lPacket);
		addToInFlightPackets(lPacket);
	}

	private void sendRequest() {
		byte[] lRequestInBytes = mRequest.assembleRequest().getBytes();
		System.out.println(lRequestInBytes);
		ByteBuffer lRequestBuffer = ByteBuffer.wrap(lRequestInBytes);
		byte[] lChunk = new byte[Packet.MAX_PAYLOAD_SIZE];
		while (lRequestBuffer.position() != lRequestBuffer.limit()) {
			int lToRead = lRequestBuffer.remaining() % Packet.MAX_PAYLOAD_SIZE;
			lRequestBuffer.get(lChunk, 0, lToRead);
			Packet lPacket = makePacket(Packet.PACKET_TYPE_DATA, lChunk, mSequenceNumber );
			sendPacket(lPacket);
			addToInFlightPackets(lPacket);
		}
	}

	private Packet makePacket(int aPacketType, byte[] aPayload, int aSequenceNumber) {
		Packet.Builder lPacketBuilder = new Packet.Builder();
		switch (aPacketType) {
			case Packet.PACKET_TYPE_SYN: return makeSynPacket(lPacketBuilder);
			case Packet.PACKET_TYPE_DATA: return makeDataPacket(lPacketBuilder, aPayload);
			case Packet.PACKET_TYPE_ACK: return makeAckPacket(lPacketBuilder, aSequenceNumber);
			default : return null;
			}
	}
	
	
	private void resendTimeoutPackets() {
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
	
	private Packet makeSynPacket(Packet.Builder aPacketBuilder) {
		aPacketBuilder.setType(Packet.PACKET_TYPE_SYN);
		aPacketBuilder.setSequenceNumber(mSequenceNumber);
		aPacketBuilder.setPeerAddress(mPeerAddress);
		aPacketBuilder.setPortNumber(mPeerPort);
		byte[] lEmpty = new byte[0];
		aPacketBuilder.setPayload(lEmpty);
		return aPacketBuilder.create();
	}
	
	private Packet makeDataPacket(Packet.Builder aPacketBuilder, byte[] aPayload) {
		aPacketBuilder.setType(Packet.PACKET_TYPE_DATA);
		
		aPacketBuilder.setSequenceNumber(mSequenceNumber);
		aPacketBuilder.setPeerAddress(mPeerAddress);
		aPacketBuilder.setPortNumber(mPeerPort);
		aPacketBuilder.setPayload(aPayload);
		
		//increment for next time aroud
		mSequenceNumber = mSequenceNumber + Packet.MAX_PAYLOAD_SIZE;
		
		return aPacketBuilder.create();
	}
	
	private Packet makeAckPacket(Packet.Builder aPacketBuilder, int aSequenceNumber) {
		aPacketBuilder.setType(Packet.PACKET_TYPE_ACK);
		aPacketBuilder.setSequenceNumber(aSequenceNumber);
		aPacketBuilder.setPeerAddress(mPeerAddress);
		aPacketBuilder.setPortNumber(mPeerPort);
		byte[] lEmpty = new byte[0];
		aPacketBuilder.setPayload(lEmpty);
		return aPacketBuilder.create();
	}
	
	private void addToInFlightPackets(Packet aPacket) {
		mInFlightPackets.add(aPacket);
	}
	
	private void sendPacket(Packet aPacket) {
		byte[] lPacketToBytes = aPacket.toBytes();
		DatagramPacket lDP = new DatagramPacket(lPacketToBytes, lPacketToBytes.length
				, mPeerAddress, 3001);
		try {
			mSocket.send(lDP);
			// set start time of packet
			aPacket.setStartTime(System.currentTimeMillis());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
}
