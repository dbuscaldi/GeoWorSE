package es.upv.dsic.geoclef.indexing.wsd;

import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import es.upv.dsic.geoclef.GeoWorSE;
import es.upv.dsic.geoclef.indexing.DocumentVector;

import java.io.*;
import java.util.Date;


class IndexNUSUBCGeoCLEF {
  public static int MODE=0; //0=standard; 2=MFS; 1=Random; 3=LFS;
  public static boolean PASSAGE_INDEXING_ON = false;
	
  public static void indexDir(String dir, String lang) 	{
  	Date start = new Date();
    try {
      String extension="";
      switch(MODE){
	      case 0 : extension="Method"; break;
	      case 1 : extension="Random"; break;
	      case 2 : extension="MFS"; break;
	      case 3 : extension="LFS";
	  }
      IndexWriterConfig iwc = new IndexWriterConfig(GeoWorSE.LVERSION, new SnowballAnalyzer(GeoWorSE.LVERSION, lang));
      iwc.setOpenMode(OpenMode.CREATE);
      IndexWriter writer;
      if(!PASSAGE_INDEXING_ON)
    	writer = new IndexWriter(FSDirectory.open(new File("index"+lang+"-WSD_"+extension)), iwc);
      else
    	writer = new IndexWriter(FSDirectory.open(new File("p_index"+lang+"-WSD_"+extension)), iwc);
      
      indexDocs(writer, new File(dir));

      writer.close();

      Date end = new Date();

      System.out.print(end.getTime() - start.getTime());
      System.out.println(" total milliseconds");

    } catch (IOException e) {
      System.out.println(" caught a " + e.getClass() +
       "\n with message: " + e.getMessage());
    }
  }
	
  public static void main(String[] args) throws IOException {
    String usage = "java " + IndexNUSUBCGeoCLEF.class + " <root_directory>";
//    if (args.length == 0) {
//      System.err.println("Usage: " + usage);
//      System.exit(1);
//    }
    
    GeoWorSE.init();
	
    indexDir("/home/datasets/CLEF-wsd/UBC", "English");
    
    GeoWorSE.close();
    
  }

  public static void indexDocs(IndexWriter writer, File file)
    throws IOException {
  	// do not try to index files that cannot be read
  	if (file.canRead()) {
      if (file.isDirectory()) {
      	String[] files = file.list();
        // an IO error could occur
        if (files != null) {
          for (int i = 0; i < files.length; i++) {
            indexDocs(writer, new File(file, files[i]));
          }
        }
      } else {
        System.out.println("adding " + file);
        try {
       		GeoCLEFWSDDocumentHandler hdlr = new GeoCLEFWSDDocumentHandler(file);
       		DocumentVector docs=hdlr.getDocuments();
       		for(int i=0; i< docs.size(); i++){
       			writer.addDocument(docs.getDocumentAt(i));
     		}
        }
        catch (Exception e) {
        	e.printStackTrace();
        } 
      }
    }
  }
}
