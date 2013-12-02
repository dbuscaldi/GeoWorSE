package es.upv.dsic.geoclef.stats;

public class StatisticData {
	private int numTopos;
	private int numWords;
	private int numMonoTopos;
	
	public StatisticData(){
		this.numTopos=0; this.numMonoTopos=0; this.numWords=0;
	}
	
	public void addTopo(){
		numTopos++;
	}
	
	public void addWord(){
		numWords++;
	}
	
	public void addMonoTopo(){
		numMonoTopos++;
	}
	
	public int getTopos(){
		return numTopos;
	}
	
	public int getWords(){
		return numWords;
	}
	
	public int getMonoTopos(){
		return numMonoTopos;
	}
}
