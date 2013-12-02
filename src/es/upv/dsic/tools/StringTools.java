/*
 * Created on 9-mar-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package es.upv.dsic.tools;

import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.EnglishStemmer;

/**
 * @author davide
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class StringTools {
	private static String [] specIT = {"di", "del", "della", "degli", "delle", "d'"};
	private static String [] specFR = {"de", "des", "du", "d'"};
	private static String [] specES = {"de", "del"};
	public static String spaceChars = " \t\n\r;:'^\"+#@¡¿?()[]{}";
	
	public static boolean startsWithCapital(String s){
		String s1=s.toLowerCase();
		if(s.equals(s1)) return false;
		else return true;
	}
	
	public static String leftZeroPad(String s, int size){
		while(s.length() < size){
			s=0+s;
		}
		return s;
	}
	
	public static boolean isSpecPrep(String input, char lang){
		switch (lang) {
			case 'i': 
				for(int i=0; i < specIT.length; i++){
					if(input.equals(specIT[i])) return true;
				}
				break;
			case 'f':
				for(int i=0; i < specFR.length; i++){
					if(input.equals(specFR[i])) return true;
				}
				break;
			case 's': 
				for(int i=0; i < specES.length; i++){
					if(input.equals(specES[i])) return true;
				}
				break;
			default: if(input.equals("of")) return true;
		}
		return false;
	}
	/**
	 * ritorna una rappresentazione "normalizzata" del testo, per il TextCrawler
	 * @param text
	 * @return
	 */
	public static String normalizePassage(String text){
		String normalizedText=text.replace('.', '!');
		normalizedText=normalizedText.replace('-', ' ');
		normalizedText=normalizedText.replace('\n', ' '); //per Lucene!
		normalizedText=normalizedText.replace('?', ' ');
		normalizedText=normalizedText.replaceAll("'", "' ");
		normalizedText=normalizedText.replaceAll(".+\\(ats.+?\\)", "");
		normalizedText=normalizedText.replaceAll(".+\\(EFE\\)", "");
		normalizedText=normalizedText.replaceAll("!", " ! ");
		normalizedText=normalizedText.replaceAll("(\\(|\\))", ",");
		normalizedText=normalizedText.replaceAll(",", " , ");
		normalizedText=normalizedText.replaceAll("\"", " \" ");
		normalizedText=normalizedText.replaceAll(" +"," ");
		return normalizedText;
	}
	
	   
	private static int minimum(int a, int b, int c){
        int mi = a;
        if (b < mi)
            mi = b;
        if (c < mi)
            mi = c;
        return mi;
    }
	/**
	 * calcola la distanza di levenshtein tra s e t
	 * @param s
	 * @param t
	 * @return
	 */
	public static int levenshtein(String s, String t){
        int d[][]; // matrix
        int n; // length of s
        int m; // length of t
        int i; // iterates through s
        int j; // iterates through t
        char s_i; // ith character of s
        char t_j; // jth character of t
        int cost; // cost
	        
        // Step 1
        n = s.length();
        m = t.length();
        if (n == 0) return m;
        if (m == 0) return n;
        d = new int[n+1][m+1];
	        
        // Step 2
        for (i = 0; i <= n; i++) d[i][0] = i;
        for (j = 0; j <= m; j++) d[0][j] = j;

        // Step 3
        for (i = 1; i <= n; i++){
            s_i = s.charAt (i - 1);

	        // Step 4
            for (j = 1; j <= m; j++){
                t_j = t.charAt(j - 1);

                // Step 5
                if (s_i == t_j) cost = 0;
                else cost = 1;

	            // Step 6
                d[i][j] = minimum(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1] + cost);
            }
        }
	    
        // Step 7
        return d[n][m];
	}
	
	/**
	 * Returns a set of String containings all the possible n-grams (with n > 1)
	 * obtainable from String s
	 * @param s
	 * @return
	 */
	public static String [] getNgrams(String s){
		String [] elems = null;
		try{
			elems=s.split(" "); //1-grams
		} catch (NullPointerException e) {
			elems="".split(" ");
		}
		int maxL=elems.length;
		int retsize=0;
		for(int i=(maxL-1); i>0; i--){
			retsize=retsize+i;
		}
		try{
			String [] ret = new String[retsize];
			ret[0]=s;
			
			StringBuffer tmp=new StringBuffer();
			int idx=1;
			for(int l=maxL-1; l > 1; l--){
				for(int k=0; k<(maxL-l+1); k++){
					tmp.delete(0, tmp.length());
					for(int j=0; j<l; j++ ){
						tmp.append(elems[k+j]);
						tmp.append(" ");
					}
					ret[idx]=(tmp.toString().trim());
					idx++;
				}
			}
			return ret;
		} catch (ArrayIndexOutOfBoundsException aioobe){
			return elems;
		}
	}
	
	public static String stemEnglish(String input){
		SnowballProgram stemmer = new EnglishStemmer();
		stemmer.setCurrent(input);
		stemmer.stem();
		return stemmer.getCurrent(); //NOTA: fatto cosi' funziona solo per target formati da una sola parola
	}
}
