import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.ListIterator;


public class Connect_P2P {

	private ArrayList<DHT_Record> DHTList;
	private int DHTServerIdx;	
	private ArrayList<Content_Record> contentList;

	public DatagramSocket P2PSSocket;
	public DatagramPacket P2PSPacket;
	public BufferedReader inP2P;
	public byte[] sendBuf;
	
	public Connect_P2P(ArrayList<DHT_Record> records1, ArrayList<Content_Record> records2, int serverNum){
		this.DHTList = records1;
		this.DHTServerIdx = serverNum;
		this.contentList = records2;
		this.sendBuf = new byte[512];
	}

	public void Go() {
		
		try {
			P2PSSocket = new DatagramSocket(40410);
			P2PSPacket = new DatagramPacket(sendBuf, sendBuf.length);
			while(true){
				P2PSSocket.receive(P2PSPacket);
				System.out.println("DHTServer "+this.DHTServerIdx+": ready to take P2P requests...");
				ProcessP2PRequest();
			}
		} catch (SocketException e) {
			System.out.println("P2P Server Socket creation failed.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("P2P Server Socket receive failed.");
			e.printStackTrace();
		}
		
	}

	private void ProcessP2PRequest () throws IOException {
		InetAddress address = P2PSPacket.getAddress();
		int port = P2PSPacket.getPort();
		String rcvString = new String(P2PSPacket.getData(), 0, P2PSPacket.getLength());
		String messageCode = "", dataString = "", message = "";
		if( rcvString.contains("GETDHT")){
			System.out.println("DHTServer "+this.DHTServerIdx+": GETDHT received from " + address.getHostAddress() + ", port " + port);
			messageCode = "SENDDHT";
			dataString = createDHTList();
			message = messageCode + "\n" + dataString;
			sendBuf = message.getBytes();
			P2PSPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
			P2PSSocket.send(P2PSPacket);
			System.out.println("DHTServer "+this.DHTServerIdx+": SENDDHT sent to " + address.getHostAddress() + ", port " + port);
		}
		else if ( rcvString.contains("INFORM")){
			System.out.println("DHTServer "+this.DHTServerIdx+": INFORM received from " + address.getHostAddress() + ", port " + port);
			Content_Record record = new Content_Record(); 
			String delims = "\n";
			String[] tokens = rcvString.split(delims);
			
			//removeRecords(address, contentList);	//removes all previous records to prevent duplicates
			record.set_IPAddr( address.getHostAddress() );
			record.set_content( tokens[1] );
			if(!InContentList(contentList, record))
				contentList.add(record);
		}
		else if ( rcvString.contains("QUIT")){
			System.out.println("DHTServer "+this.DHTServerIdx+": QUIT received from " + address.getHostAddress() + ", port " + port);
			removeRecords(address, contentList);
		}
		else if ( rcvString.contains("REQINFO")){
			System.out.println("DHTServer "+this.DHTServerIdx+": REQINFO received from " + address.getHostAddress() + ", port " + port);
			String recs = requestInfo(contentList, rcvString);
			if(recs.equals("")){
				System.out.println("DHTServer "+this.DHTServerIdx+": CODE404 sent to " + address.getHostAddress() + ", port " + port);
				messageCode = "CODE404";
				sendBuf = message.getBytes();
				P2PSPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
				P2PSSocket.send(P2PSPacket);				
			}
			else {
				System.out.println("DHTServer "+this.DHTServerIdx+": INFORM sent to " + address.getHostAddress() + ", port " + port);
				messageCode = "INFORM";
				message = messageCode + "\n" + recs;
				sendBuf = message.getBytes();
				P2PSPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);
				P2PSSocket.send(P2PSPacket);				
			}
		}
	}

	private boolean InContentList(ArrayList<Content_Record> list, Content_Record record) {
		ListIterator<Content_Record> li = list.listIterator();
		while(li.hasNext()){
			Content_Record cr = li.next();
			if(cr.IPAddr.equals(record.IPAddr) && cr.content.equals(record.content))
				return true;
		}		
		
		return false;
	}

	private String requestInfo(ArrayList<Content_Record> list, String data) {
		String contentFile = "";
		String delims = "\n";
		String[] tokens = data.split(delims);
		String result = "";
		
		contentFile = tokens[1];
		ListIterator<Content_Record> li = list.listIterator();
		while(li.hasNext()){
			Content_Record cr = li.next();
			if(cr.content.equals(contentFile))
				result += contentFile + "," + cr.IPAddr + "\n";
		}		

		return result;
	}

	private void removeRecords(InetAddress addr, ArrayList<Content_Record> list) {
		String hostAddress = addr.getHostAddress();
		ListIterator<Content_Record> li = list.listIterator();
		while(li.hasNext()){
			Content_Record cr = li.next();
			if(cr.IPAddr.equals(hostAddress))
				li.remove();
		}		
	}

	private String createDHTList() {
		String dString = "";
		for( int i=1; i<=3; i++){
			DHT_Record rec = this.DHTList.get(i);
			//String[] tokens = rec.IPAddr.split("/");
			dString += Integer.toString(rec.serverNum) + "," + rec.IPAddr + "," + Integer.toString(rec.status) + "\n";			
		}
		return dString;
	}
	
	
}
