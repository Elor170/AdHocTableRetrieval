package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Searcher {
    //TODO find the optimal weights

    static float TITLE_WEIGHT = 0.432f;
    static float CAPTION_WEIGHT = 0.13f;
    static float HEADER_WEIGHT = 0.218f ;
    static float COLUMN_WEIGHT = 1 - TITLE_WEIGHT -CAPTION_WEIGHT - HEADER_WEIGHT;
    static final String TEAM_NAME = "Elor_Lior";
    static final String TAB = " ";
    static final int RETRIEVE_DOCS_NUM = 20;
    static final String OUT_FILE = "trec_eval\\results.txt";
    private static final boolean RUN_AUTO_EVALUATION = true;



    public static void main(String[] args) {
        LocalDateTime start = LocalDateTime.now();

            try {
                searchQueries();
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }

            if(RUN_AUTO_EVALUATION)


            runEvaluation();

            LocalDateTime end = LocalDateTime.now();

            try {
                Thread.sleep(2500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Duration timeElapsed = Duration.between(start, end);
            System.out.println("Time taken: " + timeElapsed.toMinutes() + " minutes " +
                    timeElapsed.toSeconds() + " seconds");
    }

    private static void searchQueries() throws ParseException, IOException {
        // TODO check the problem with table IDs
        Analyzer enAnalyzer = new EnglishAnalyzer();
        Directory indexDirectory = FSDirectory.open(Paths.get("src\\main\\resources\\index"));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        FileWriter fw = new FileWriter(OUT_FILE);

        File queriesFile = new File("src\\main\\resources\\queries.txt");
        Scanner fileReader = new Scanner(queriesFile);
        while (fileReader.hasNextLine()) {
            String queryLine = fileReader.nextLine();
            int idEndIndex = queryLine.indexOf(" ");
            String queryId;
            String queryString;

            if (idEndIndex != -1) {
                queryId = queryLine.substring(0, idEndIndex);
                queryString = queryLine.substring(idEndIndex + 1);
            }
            else {
                queryId = queryLine;
                queryString = " ";
            }
            System.out.print(queryId + ". ");
            System.out.println(queryString);
            // TODO check the problem with query #5
            if (!queryId.equals("5"))
                searchSingleQuery(indexSearcher, enAnalyzer, queryString, queryId,fw);
            System.out.println("    ");
        }

        indexDirectory.close();
        indexReader.close();
        fw.close();
        String queryString = "world interest rates table";
    }

    private static void searchSingleQuery(IndexSearcher indexSearcher, Analyzer analyzer,
                                   String queryString, String queryId, FileWriter fw) throws ParseException, IOException {

        String[] fieldsNames = new String[]{"title", "caption", "header", "column"};
        Map<String,Float> fieldsWeights = new HashMap<>();
        fieldsWeights.put("title", TITLE_WEIGHT);
        fieldsWeights.put("caption", CAPTION_WEIGHT);
        fieldsWeights.put("header", HEADER_WEIGHT);
        fieldsWeights.put("column", COLUMN_WEIGHT);

        QueryParser queryParser = new MultiFieldQueryParser(fieldsNames, analyzer, fieldsWeights);
        Query query = queryParser.parse(queryString);


        TopDocs topDocs = indexSearcher.search(query, RETRIEVE_DOCS_NUM);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        for (ScoreDoc scoreDoc: scoreDocs){
            int docNum = scoreDoc.doc;
            String docExplanation = indexSearcher.explain(query, docNum).toString();

            String docScoring = docExplanation.substring(0, docExplanation.indexOf(" "));
            Document document = indexSearcher.doc(docNum);
            String iteration = "Q0";
            String table_id =  document.get("id");
            //todo need to implement
            int rank = 1;

            System.out.print(queryId + TAB);
            System.out.print(iteration + TAB);
            System.out.print(table_id + TAB);
            System.out.print(docScoring + TAB);
            System.out.println(TEAM_NAME);


            String result = String.format("%s %s %s %s %s %s",
                                           queryId,
                                           iteration,
                                           table_id,
                                           rank,
                                           docScoring,
                                           TEAM_NAME);

            fw.write(result + System.lineSeparator());
        }
    }


    private static void runEvaluation(){
        try {
            String[] command = {"cmd.exe", "/C", "Start", "trec_batch_ndcg_cut.bat"};
            File dir = new File("trec_eval");
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir);
            pb.start();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
