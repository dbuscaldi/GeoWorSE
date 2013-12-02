package es.upv.dsic.geoclef.ranking;

public interface Ranker {
	public Entry getNext();
	public boolean hasMoreEntries();
}
