package tac.kbp.entitylinking.ranking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import tac.kbp.entitylinking.queries.GoldQuery;
import tac.kbp.entitylinking.queries.ELQuery;
import tac.kbp.entitylinking.queries.ELQueryComparator;
import tac.kbp.entitylinking.queries.candidates.Candidate;
import tac.kbp.entitylinking.queries.features.Features;

public class SVMRank {
	
	/*
	# query 1
	3 qid:1 1:1 2:1 3:0 4:0.2 5:0
	2 qid:1 1:0 2:0 3:1 4:0.1 5:1
	1 qid:1 1:0 2:1 3:0 4:0.4 5:0
	1 qid:1 1:0 2:0 3:1 4:0.3 5:0
	*/
	
	public void svmRankFormat(List<ELQuery> queries, HashMap<String, GoldQuery> queries_answers, String outputfile) throws IOException {
		
		FileWriter fstream = new FileWriter(outputfile);
		BufferedWriter out = new BufferedWriter(fstream);

		// to avoid: "ERROR: Query ID's in data file have to be in increasing order" from SVMRank
		// sort queries according to id  in increasing order
		Collections.sort(queries, new ELQueryComparator());
		
		// find max for each feature to be used in normalization: simply divide by the max value
		double[] max = new double[24];
		
		for (int i = 0; i < max.length; i++) {
			max[i] = 0;
		}
		
		for (ELQuery q : queries) {
			for (Candidate c : q.candidates) {
				Features f = c.features;
				double[] features = f.featuresVector();
				for (int i = 0; i < features.length; i++) {
					if (features[i]>max[i])
						max[i] = features[i];
				}
			}
		}

		// use SVMRank in a simple way, correct answer ranked 1, all others ranked 0
		// this reflects in a training procedure with less comparisons
		
		for (ELQuery q : queries) {

			out.write("#" + q.query_id + " " + q.gold_answer + "\n");

			for (Candidate c : q.candidates) {
				double[] vector = c.features.featuresVector();

				// correct entity ranked 1, all candidates ranked 0
				if (queries_answers.get(q.query_id).answer.equalsIgnoreCase(c.entity.id))
					out.write("1"+" ");
				else out.write("0"+" ");
				
				String[] query_parts;
				
				// query identifier
				if (q.query_id.startsWith("EL_"))
					query_parts = q.query_id.split("EL_");
				else
					query_parts = q.query_id.split("EL");			
				
				out.write("qid:"+Integer.parseInt(query_parts[1])+" ");
				
				for (int i = 0; i < vector.length; i++) {
					if (max[i]==0)
						out.write((i+1)+":"+vector[i]+" ");
					else out.write((i+1)+":"+vector[i]/max[i]+" ");
				}
				out.write("#" + c.entity.id);
				out.write("\n");
			}
		}
		out.close();
	}
	
	public void svmRankFormat(String queriesFilesDir, HashMap<String, GoldQuery> queries_answers, String outputfile) throws IOException {
		
		File dir = new File(queriesFilesDir);
		String[] files = dir.list();
		
		FileWriter fstream = new FileWriter(outputfile);
		BufferedWriter out = new BufferedWriter(fstream);
		
		if (files == null) {
		} else {
			System.out.println("found " + files.length + " files");
			
			java.util.Arrays.sort(files);
			
		    for (int i=0; i < files.length; i++) {
		        String filename = files[i];
		        readFile(filename,queries_answers,out);
		    }
		}
	}
	
	public void readFile(String filename,HashMap<String, GoldQuery> queries_answers, BufferedWriter out) throws IOException{
		
		//filename = queryID
		// each line candidate:
		//	 E0393717:0.1166139856018351,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,8763.0,0.0,0
		
		String query_id = filename.split("\\.")[0];
		
		FileInputStream fstream = new FileInputStream(filename);
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String strLine;

		System.out.println("Processing " + query_id);
		
		out.write("#" + query_id + " " +  queries_answers.get(query_id).answer+"\n");
		
		while ((strLine = br.readLine()) != null)   {
			
			String[] line_parts = strLine.split(":");
			String candidate_id = line_parts[0];
			String[] features = line_parts[1].split(",");
			
			if (features[features.length-1].equalsIgnoreCase("1"))
				out.write("1 ");
			else out.write("0 ");
			
			out.write("qid:"+filename.split("EL")[1]+' ');
						
			for (int z = 0; z < features.length-1; z++) {
				out.write((z+1)+":"+features[z]+' ');	
			}
			out.write("#"+candidate_id+"\n");
		}
		br.close();
		in.close();
		fstream.close();
	}

}