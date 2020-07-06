package org.example;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class Indexer {

    public static void main( String[] args ) throws IOException {
        LocalDateTime start = LocalDateTime.now();

        // create index writer
        Analyzer enAnalyzer = new EnglishAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(enAnalyzer);
        Directory indexDirectory = FSDirectory.open(Paths.get("src\\main\\resources\\index"));
        IndexWriter indexWriter = new IndexWriter(indexDirectory, indexWriterConfig);

        int indexedTablesNum = 0;
        try {
            indexedTablesNum = readTables(indexWriter);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        System.out.println("There were indexed "+ indexedTablesNum + " tables.");
        indexWriter.close();

        LocalDateTime end = LocalDateTime.now();
        Duration timeElapsed = Duration.between(start, end);
        System.out.println("Time taken: "+ timeElapsed.toMinutes() +" minutes " +
                timeElapsed.toSeconds() +" seconds");
    }

    private static int readTables(IndexWriter indexWriter) throws IOException, JSONException {

        GZIPInputStream inputStream = new GZIPInputStream(
                new FileInputStream("src\\main\\resources\\tables.json.gz"));
        Reader reader = new InputStreamReader(inputStream);
        BufferedReader buffer = new BufferedReader(reader);

        int indexingCounter = 0;
        String tableString;
        tableString = buffer.readLine();
        while ((tableString = buffer.readLine()) != null) {

            // extract table data
            JSONObject table = new JSONObject(tableString);
            String idString = (String) table.get("_id");
            String titleString = (String) table.get("pgTitle");
            String captionString;
            try {
                captionString = (String) table.get("tableCaption");
            } catch (JSONException e) {
                captionString = "";
            }

            // extract headers data
            JSONArray headersArray = (JSONArray) ((JSONArray) table.get("tableHeaders")).get(0);
            int numberOfColumns = headersArray.length();
            List<String> headersStrings = new ArrayList<>();
            for (int columnNum = 0; columnNum < numberOfColumns; columnNum++) {
                JSONObject header = (JSONObject) headersArray.get(columnNum);
                headersStrings.add((String) header.get("text"));
            }

            // extract cells data
            JSONArray tableData = ((JSONArray)table.get("tableData"));
            List<String> columnsStringsList = new ArrayList<>();
            for (int columnNum = 0; columnNum < numberOfColumns; columnNum++)
                columnsStringsList.add("");

            int numberOfRows = tableData.length();
            for(int columnNum = 0; columnNum < numberOfColumns; columnNum++){
                for(int rowNum = 0; rowNum < numberOfRows; rowNum++){
                    JSONArray tableRowArray = (JSONArray) tableData.get(rowNum);
                    String cellString = (String) ((JSONObject)tableRowArray.get(columnNum)).get("text");
                    String oldColumnString = columnsStringsList.get(columnNum);
                    columnsStringsList.set(columnNum, oldColumnString + "\n" +  cellString);
                }
            }


            // save as document
            Document tableDocument = new Document();

            StringField idField = new StringField("id", idString, Field.Store.YES);
            TextField titleField = new TextField("title", titleString, Field.Store.NO);
            TextField captionField = new TextField("caption", captionString, Field.Store.NO);
            tableDocument.add(idField);
            tableDocument.add(titleField);
            tableDocument.add(captionField);
            for (String headerString: headersStrings){
                TextField headerField = new TextField("header", headerString, Field.Store.NO);
                tableDocument.add(headerField);
            }
            for (String columnString: columnsStringsList){
                TextField columnField = new TextField("column", columnString, Field.Store.NO);
                tableDocument.add(columnField);
            }

            indexWriter.addDocument(tableDocument);


            // table printing
//            System.out.println("ID: " + idString + "\n"
//                    + "Title: " + titleString + "\n"
//                    + "Caption: " + captionString);
//            int columnNum = 0;
//            for (String headerString: headersStrings){
//                System.out.println("----------------------");
//                System.out.print( "Header " + columnNum + ": " + headerString);
//                System.out.println(columnsStringsList.get(columnNum));
//                columnNum++;
//            }
            System.out.println("Index Num: " + indexingCounter++);
        }
        return indexingCounter;
    }
}
