package tac.kbp.bin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

import redis.clients.jedis.BinaryJedis;
import tac.kbp.kb.index.spellchecker.SpellChecker;
import tac.kbp.queries.GoldQuery;
import tac.kbp.queries.KBPQuery;
import tac.kbp.queries.xml.ParseQueriesXMLFile;
import tac.kbp.utils.misc.BigFile;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class Definitions {
	
	/* Settings for Retrieval from Lucene */
	public static int max_candidates = 5;
	
	/* named-entities type */
	public enum NERType {
	    PERSON, ORGANIZATION, PLACE, UNK
	}
	
	/* queries */
	public static String queriesPath = "/collections/TAC-2011/queries/ivo/";
	public static List<KBPQuery> queriesTrain = null;
	public static List<KBPQuery> queriesTest = null;	
	public static String test_queries = new String();
	public static String test_queries_answers = new String();
	public static HashMap<String, GoldQuery> queriesAnswersTrain = null;
	public static HashMap<String, GoldQuery> queriesAnswersTest = null;
	
	/* indexes locations */
	public static String KB_location = "/collections/TAC-2011/index";
	public static String SpellChecker_location = "/collections/TAC-2011/spellchecker_index";
	public static String DocumentCollection_location = "/collections/TAC-2011/document_collection_index";

	/* support doc and named-entities recognizer */
	public static String named_entities_supportDoc = "/collections/TAC-2011/named-entities-Stanford-CRF-XML";
	public static String serializedClassifier = "/collections/TAC-2011/resources/all.3class.distsim.crf.ser.gz";
	
	/* stopwords */
	public static String stop_words_location = "/collections/TAC-2011/resources/stopwords.txt";
	public static Set<String> stop_words = new HashSet<String>();
	
	/* LDA topics */
	public static String kb_lda_topics = "/collections/TAC-2011/LDA/model/model-final.theta";
	public static String queries_lda_path = "/collections/TAC-2011/LDA/queries/";
	public static HashMap<Integer, String> queries_topics = new HashMap<Integer, String>();
	public static HashMap<Integer, String> kb_topics = new HashMap<Integer, String>();
	
	/* Lucene indexes */
	public static IndexSearcher searcher = null;
	public static SpellChecker spellchecker = null;
	public static IndexSearcher documents = null;
	public static IndexReader docs_reader = null;

	/* StanfordNER CRF classifier */
	@SuppressWarnings("rawtypes")
	public static AbstractSequenceClassifier classifier = null;
	
	/* REDIS server */
	public static int redis_port = 6379;
	public static String redis_host = "agatha";
	public static BinaryJedis binaryjedis = null;
	
	/* 3rd party software */
	public static String SVMRankPath = "/collections/TAC-2011/SVMRank/";
	public static String SVMRanklLearn = "svm_rank_learn";
	public static String SVMRanklClassify = "svm_rank_classify";
	
	public static void loaddRecall(String queriesFile, String n_candidates) throws CorruptIndexException, IOException {
		
		/* Lucene Index */
		System.out.println("Knowledge Base index: " + KB_location);
		searcher = new IndexSearcher(FSDirectory.open(new File(KB_location)));
		
		/* SpellChecker Index */
		System.out.println("SpellChecker index: " + SpellChecker_location);
		FSDirectory spellDirectory = FSDirectory.open(new File(SpellChecker_location));
		spellchecker = new SpellChecker(spellDirectory, "name", "id");
		
		/* Document Collection Index */
		System.out.println("Document Collection (IndexReader): " + DocumentCollection_location);
		documents = new IndexSearcher(FSDirectory.open(new File(DocumentCollection_location)));
		docs_reader = documents.getIndexReader();
		
		/* Number of candidates to retrieve per sense */
		Definitions.max_candidates = Integer.parseInt(n_candidates);
		System.out.println("Number of candidates to retrieve per sense: " + Definitions.max_candidates);
		
		//Stop-Words
		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
		
		/*
		if (queriesFile.contains("train_queries_2009")) {
			lda_topics_train = queries_lda_path+"train_queries_2009.txt.theta";
			answers = gold_standard_path+"train_results_2009.tab"; 
			
		}
		else if (queriesFile.contains("train_queries_2010")) {
			lda_topics_train = queries_lda_path+"train_queries_2010.txt.theta";
			answers = gold_standard_path+"train_results_2010.tab";
		}
		
		else if (queriesFile.contains("train_queries_2011")) {
			lda_topics_train = queries_lda_path+"train_queries_2011.txt.theta";
			answers = gold_standard_path+"train_results_2011.tab";
			
		}
		
		else if (queriesFile.contains("test_queries_2009")) {
			lda_topics_train = queries_lda_path+"test_queries_2009.txt.theta";
			answers = gold_standard_path+"test_results_2009.tab";
		}
		
		else if (queriesFile.contains("test_queries_2010")) {
			lda_topics_train = queries_lda_path+"test_queries_2010.txt.theta";
			answers = gold_standard_path+"test_results_2010.tab";
		}
		
		else if (queriesFile.contains("test_queries_2011")) {
			lda_topics_train = queries_lda_path+"test_queries_2011.txt.theta";
			answers = gold_standard_path+"test_results_2011.tab";
		}
		
		//Queries answer file
		System.out.println("Loading queries answers from: " + answers);
		queriesAnswersTrain = loadQueriesAnswers(answers);
		*/
		
		//Queries XML file
		System.out.println("Loading queries from: " + queriesFile);
		queriesTrain = tac.kbp.queries.xml.ParseQueriesXMLFile.loadQueries(queriesFile);
		
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);
	}
	
	public static void loadKBIndex() throws CorruptIndexException, IOException {
		
		System.out.println("Knowledge Base index: " + KB_location);
		searcher = new IndexSearcher(FSDirectory.open(new File(KB_location)));
		
	}

	/* SpellChecker Index */
	
	public static void loadSpellCheckerIndex() throws CorruptIndexException, IOException {
		
		System.out.println("SpellChecker index: " + SpellChecker_location);
		FSDirectory spellDirectory = FSDirectory.open(new File(SpellChecker_location));
		spellchecker = new SpellChecker(spellDirectory, "name", "id");
	}
	
	/* Document Collection Index */	
	
	public static void loadDocumentCollecion() throws CorruptIndexException, IOException {
		
		System.out.println("Document Collection index: " + DocumentCollection_location);
		documents = new IndexSearcher(FSDirectory.open(new File(DocumentCollection_location)));	
	}

	/* Stop-Words */

	public static void loadStopWords() {

		System.out.println("Loading stopwords from: " + stop_words_location);
		loadStopWords(stop_words_location);
	}
	
	/* REDIS Connection */

	public static void connectionREDIS() {
		System.out.println("Connecting to REDIS server.. ");
		binaryjedis = new BinaryJedis(redis_host, redis_port);
	}

	/* Stanford NER-CRF Classifier */
	
	public static void loadClassifier(String filename) {
		classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
	}
	
	/* load StopWords list */
	
	public static void loadStopWords(String file) { 
		
		try{
			  FileInputStream fstream = new FileInputStream(file);
			  DataInputStream in = new DataInputStream(fstream);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  
			  while ((strLine = br.readLine()) != null)   {				  
				  stop_words.add(strLine.trim());
			  }
			  
			  in.close();
			  
			}
		
			catch (Exception e) {
				System.err.println("Error: " + e.getMessage());
			}
	}
	
	/* load LDATopics file */
	
	/* load LDA topics file */ 
	public static void loadLDATopics(String filename, HashMap<Integer, String> hashtable) throws Exception {

		BigFile file = new BigFile(filename);
		int i=0;
		
		for (String line : file) {
			hashtable.put(i, line);
			i++;
		}
	}
	
	/* load queries answers */
	public static HashMap<String, GoldQuery> loadQueriesAnswers(String queries) throws IOException {
		
		/* determine answers file based on input file */
		
		String answers = null;
		
		if (queries.contains("train_queries_2009")) answers = queriesPath+"train_results_2009.tab";
		else if (queries.contains("train_queries_2010")) answers = queriesPath+"train_results_2010.tab";
		else if (queries.contains("train_queries_2011")) answers = queriesPath+"train_results_2011.tab";
		else if (queries.contains("test_queries_2009")) answers = queriesPath+"test_results_2009.tab";
		else if (queries.contains("test_queries_2010")) answers = queriesPath+"test_results_2010.tab";
		else if (queries.contains("test_queries_2011")) answers = queriesPath+"test_results_2011.tab";
		
		System.out.println("Loading queries answers from: " + answers);
		
		BufferedReader input;
		HashMap<String, GoldQuery> queriesGoldTrain = new HashMap<String, GoldQuery>();
		
		if (queries.contains("2009") || queries.contains("2010/trainning") || queries.contains("train_results_2010")) {
			
			try {
				
				input = new BufferedReader(new FileReader(answers));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldQuery gold = new GoldQuery(contents[0], contents[1], contents[2]);
		          queriesGoldTrain.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();    	
			}
		}
		
		else {
			
			try {
				
				input = new BufferedReader(new FileReader(answers));
				String line = null;
		        
				while (( line = input.readLine()) != null){
		          String[] contents = line.split("\t");	          
		          GoldQuery gold = new GoldQuery(contents[0], contents[1], contents[2], contents[3], contents[4]);
		          queriesGoldTrain.put(contents[0], gold);
		        }
		        
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();    	
			}
		}
		return queriesGoldTrain;		
	}
	
	public static void determineLDAFile(String queries) throws Exception {
		
		String lda_topics = null;
		
		if (queries.contains("train_queries_2009")) lda_topics = queries_lda_path+"train_queries_2009.txt.theta";
		else if (queries.contains("train_queries_2010")) lda_topics = queries_lda_path+"train_queries_2010.txt.theta";
		else if (queries.contains("train_queries_2011")) lda_topics = queries_lda_path+"train_queries_2011.txt.theta";
		else if (queries.contains("test_queries_2009")) lda_topics = queries_lda_path+"test_queries_2009.txt.theta";
		else if (queries.contains("test_queries_2010")) lda_topics = queries_lda_path+"test_queries_2010.txt.theta";
		else if (queries.contains("test_queries_2011")) lda_topics = queries_lda_path+"test_queries_2011.txt.theta";
			
		//LDA topics for queries
		System.out.print("Loading queries LDA topics from: " + lda_topics + "..." );
		loadLDATopics(lda_topics,queries_topics);
		
	}	
}