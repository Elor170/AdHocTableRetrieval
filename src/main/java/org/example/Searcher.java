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

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class Searcher {
    //TODO find the optimal weights
    static final float TITLE_WEIGHT = 0.2f;
    static final float CAPTION_WEIGHT = (1 - TITLE_WEIGHT) / 3;
    static final float HEADER_WEIGHT = (1 - TITLE_WEIGHT) / 3;
    static final float COLUMN_WEIGHT = (1 - TITLE_WEIGHT) / 3;


    public static void main(String[] args) throws IOException{
        LocalDateTime start = LocalDateTime.now();

        Analyzer enAnalyzer = new EnglishAnalyzer();
        Directory indexDirectory = FSDirectory.open(Paths.get("src\\main\\resources\\index"));
        IndexReader indexReader = DirectoryReader.open(indexDirectory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);

        try {
            searchQueries(indexSearcher, enAnalyzer);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        indexDirectory.close();
        indexReader.close();

        LocalDateTime end = LocalDateTime.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("Time taken: "+ timeElapsed.toMinutes() +" minutes " +
                timeElapsed.toSeconds() +" seconds");
    }

    private static void searchQueries(IndexSearcher indexSearcher, Analyzer analyzer) throws ParseException, IOException {
        // TODO add loop over the queries, and check the problem with table IDs...

        String queryString = "world interest rates table";
        String[] fieldsNames = new String[]{"title", "caption", "header", "column"};
        Map<String,Float> fieldsWeights = new HashMap<>();
        fieldsWeights.put("title", TITLE_WEIGHT);
        fieldsWeights.put("caption", CAPTION_WEIGHT);
        fieldsWeights.put("header", HEADER_WEIGHT);
        fieldsWeights.put("column", COLUMN_WEIGHT);

        QueryParser queryParser = new MultiFieldQueryParser(fieldsNames, analyzer, fieldsWeights);
        Query query = queryParser.parse(queryString);

        int hitsPerPage = 20;

        TopDocs topDocs = indexSearcher.search(query, hitsPerPage);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        System.out.println("Found " + scoreDocs.length + " hits:");
        for (ScoreDoc scoreDoc: scoreDocs){
            int docNum = scoreDoc.doc;
            String docExplanation = indexSearcher.explain(query, docNum).toString();
            String docScoring = docExplanation.substring(0, docExplanation.indexOf(" "));
            Document document = indexSearcher.doc(docNum);
            System.out.print("<query id>" + "   ");
            System.out.print("Q0" + "   ");
            System.out.print("table-" + document.get("id") + "    ");
            System.out.print(docScoring + "   ");
            System.out.println("Elor_Lior");
        }
    }
}
