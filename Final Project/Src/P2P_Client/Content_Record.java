
public class Content_Record {

	String content, DHTServer, ipAddr;

	public Content_Record() {
		content = "";
		DHTServer = "";
		ipAddr = "";
	}
	public Content_Record(String cont, String ipa){
		content = cont;
		DHTServer = "";
		ipAddr = ipa;
	}
	public Content_Record(String cont, String DHTnum, String ipa){
		content = cont;
		DHTServer = DHTnum;
		ipAddr = ipa;
	} 

}
