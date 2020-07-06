package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

public class Searcher {

    public static void main(String[] args) throws IOException, ParseException {
        LocalDateTime start = LocalDateTime.now();

        Analyzer enAnalyzer = new EnglishAnalyzer();
        Directory index = FSDirectory.open(Paths.get("src\\main\\resources\\index"));

        String querystr = "phases of the moon";
        //QueryParser queryParser = new QueryParser("caption", enAnalyzer);
        QueryParser queryParser = new MultiFieldQueryParser(new String[]{"title", "caption", "header", "column"}
        , enAnalyzer);
        Query query = queryParser.parse(querystr);

        int hitsPerPage = 25;
        IndexReader reader = DirectoryReader.open(index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(query, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document document = searcher.doc(docId);
            System.out.println(document.get("id"));
        }

        LocalDateTime end = LocalDateTime.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("Time taken: "+ timeElapsed.toMinutes() +" minutes " +
                timeElapsed.toSeconds() +" seconds");
    }
}
