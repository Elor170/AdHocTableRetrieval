package org.example.Kmeans;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
public class DocumentDistance {

    public static final String CONTENT = "content";
    public static final int N = 2;//Total number of documents

    private static Set<String> terms = new HashSet<>();
//    private RealVector v1;
//    private RealVector v2;


    public static void main(String[] args) {
        Directory indexDirectory = null;
        try {
            indexDirectory = FSDirectory.open(Paths.get("src\\main\\resources\\index"));
            IndexReader reader = DirectoryReader.open(indexDirectory);

            Map<String, Double> a = getWeights(reader, reader.getTermVector(1,CONTENT));
            double q = getCosineSimilarity(reader, reader.getTermVector(1,CONTENT), reader.getTermVector(1,CONTENT));
            System.out.println(q);

            reader.close();


            int x = 4;


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static Map<String, Double> getWeights(IndexReader reader, Terms vector) throws IOException {

        Map<String, Integer> docFrequencies = new HashMap<>();
        Map<String, Integer> termFrequencies = new HashMap<>();
        Map<String, Double> tf_Idf_Weights = new HashMap<>();

        //Terms vector = reader.getTermVector(docNum, CONTENT);
        TermsEnum termsEnum = vector.iterator();
        BytesRef text = null;
        PostingsEnum postings = null;

        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();
            docFrequencies.put(term, reader.docFreq(new Term(CONTENT, term)));
            postings = termsEnum.postings(postings, PostingsEnum.FREQS);
            postings.nextDoc();
            int freq = postings.freq();
            termFrequencies.put(term, freq);
            terms.add(term);
        }

        for (String term : docFrequencies.keySet()) {
            long tf = termFrequencies.get(term);
            int df = docFrequencies.get(term);
            double idf = (1 + Math.log(N) - Math.log(df));
            double w = tf * idf;
            tf_Idf_Weights.put(term, w);
        }

        return tf_Idf_Weights;
    }

    public static double getCosineSimilarity(IndexReader reader, Terms terms1, Terms terms2) throws IOException {
        if(terms1 == null || terms2 == null) return -Double.MAX_VALUE;

        Map<String, Double> tfidf1 = (getWeights(reader, terms1));
        Map<String, Double> tfidf2 = (getWeights(reader, terms2));
        RealVector v1 = toRealVector(tfidf1);
        RealVector v2 = toRealVector(tfidf2);
        double dotProduct = v1.dotProduct(v2);
        //System.out.println("Dot: " + dotProduct);
        //System.out.println("V1_norm: " + v1.getNorm() + ", V2_norm: " + v2.getNorm());
        double normalization = (v1.getNorm() * v2.getNorm());
       // System.out.println("Norm: " + normalization);
        return dotProduct / normalization;
    }

    private static RealVector toRealVector(Map<String, Double> map) {
        RealVector vector = new ArrayRealVector(terms.size());
        int i = 0;
        double value;
        for (String term : terms) {

            if (map.containsKey(term)) {
                value = map.get(term);
            } else {
                value = (double) 1 / (terms.size());
            }
            vector.setEntry(i++, value);
        }

        return vector.mapDivide(vector.getL1Norm());
    }

//    public static void printMap(Map<String, Integer> map) {
//        for ( String key : map.keySet() ) {
//            System.out.println( "Term: " + key + ", value: " + map.get(key) );
//        }
//    }
//
//    public static void printMapDouble(Map<String, Double> map) {
//        for ( String key : map.keySet() ) {
//            System.out.println( "Term: " + key + ", value: " + map.get(key) );
//        }
//    }

}

