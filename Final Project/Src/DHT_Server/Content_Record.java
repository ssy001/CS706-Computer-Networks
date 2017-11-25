
public class Content_Record {

	public String content;
	public String IPAddr;
	
	public Content_Record() {
		content = "";
		IPAddr = "";
	}

	public Content_Record(String content, String IPAddr){
		this.content = content;
		this.IPAddr = IPAddr;		
	}
	
	public String get_content(){ return this.content; }
	public void set_content(String c){ this.content = c; }

	public String get_IPAddr(){ return this.IPAddr; }
	public void set_IPAddr(String ip){ this.IPAddr = ip; }

	
	
}
