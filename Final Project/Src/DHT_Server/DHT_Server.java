import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class DHT_Server {

	/**
	 * @param args
	 */
	public static ArrayList<Content_Record> recordList;
	public Content_Record requestedRecord;
	public static ArrayList<DHT_Record> DHTServerList;
	
	public static void main(String[] args) throws InterruptedException {
		Connect_DHT cDHT;
		Connect_P2P cP2P;
		int DHTServerNum;
		String filename = "DHT_Server.txt";
		String hostname[] = new String[4];
		InetAddress hostAddress[] = new InetAddress[4];
		Scanner reader = new Scanner(System.in);
		
		//DHT to DHT
		System.out.println("Enter the DHT server number (1-4): ");
		DHTServerNum = reader.nextInt();
/*		if(DHTServerNum != 1){
			System.out.println("Enter the host name: ");
			hostname[DHTServerNum-1] = reader.next();
			try {
				hostAddress[DHTServerNum-1] = InetAddress.getByName(hostname[DHTServerNum-1]);
			} catch (UnknownHostException e) {
				System.out.println("Exception: Unknown hostname");
				e.printStackTrace();
				System.exit(-1);
			}
		}
*/
		reader.close();

		//if DHT Server 1, obtain host names of other 3 DHT Servers
//		if(DHTServerNum == 1){
			try {
				File fin = new File(filename);
				BufferedReader br = new BufferedReader(new FileReader(fin));
				String line = null;
				String[] token;
				while((line = br.readLine()) != null){
					System.out.println(line);
					token = line.split(",");
					hostname[Integer.parseInt(token[0])-1] = token[1];
					InetAddress addresses[] = InetAddress.getAllByName(token[1]);
					int i=0;
					while(i< addresses.length){
						System.out.println(addresses[i].getHostAddress());
						String addy = addresses[i].getHostAddress();
						if(addy.contains("141.117.232.") || addy.contains("192.168.0.")){
							hostAddress[Integer.parseInt(token[0])-1] = addresses[i];
							break;
						}
						i++;
					}
				}
				br.close();
			} 
			catch (UnknownHostException e) {
				System.out.println("Exception: Unknown hostnames");
				e.printStackTrace();
				System.exit(-1);
			}
			catch (IOException e){
				System.out.println("Exception: File read error");
				e.printStackTrace();	
				System.exit(-1);
			}
			
			//if( DHTServerNum == 1 && hostAddress[0].isSiteLocalAddress())
				Init_Vars(hostname, hostAddress, DHTServerNum);
//		}	//end if
		cDHT = new Connect_DHT("Connect_DHT"+DHTServerNum, DHTServerList, DHTServerNum);
		//Runtime.getRuntime().addShutdownHook(cDHT_Thread);
		cDHT.start();
		
		reader = new Scanner(System.in);
		String answer;
/*		while(true){

			System.out.println("Do you want to check DHT Status? (Y/*)");
			answer = reader.next();
			if( !answer.equals( "Y"))
				listDHTServers(DHTServerList);

				System.out.println("Listing DHT Servers status ");
				listDHTServers(DHTServerList);
			TimeUnit.MILLISECONDS.sleep(8000);
		}
*/
		//DHT to P2P
		recordList = new ArrayList<Content_Record>();
		cP2P = new Connect_P2P(DHTServerList, recordList, DHTServerNum);
		cP2P.Go();
	
	}

	private static void listDHTServers(ArrayList<DHT_Record> list) {
		DHT_Record rec;
		for(int i=0; i<=3; i++){
			rec = list.get(i);
			System.out.println("Server " + rec.serverNum + ", IP: " + rec.IPAddr + ", Status: " + rec.status);
		}
	}

	private static void Init_Vars(String[] name, InetAddress[] address, int num) {
		DHTServerList = new ArrayList<DHT_Record>();
		int status;
		for(int i=1; i<=4; i++){
			if(i == num) status=1;
			else status=0;
			DHTServerList.add(new DHT_Record(i, address[i-1].getHostAddress(), status));
		}
		//DHTServerList.add(new DHT_Record(1, address[0].getHostAddress(), 1));
		//DHTServerList.add(new DHT_Record(2, address[1].getHostAddress(), 0));
		//DHTServerList.add(new DHT_Record(3, address[2].getHostAddress(), 0));
		//DHTServerList.add(new DHT_Record(4, address[3].getHostAddress(), 0));

	}


	
	
}
