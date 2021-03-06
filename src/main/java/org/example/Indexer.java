package org.example;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;


public class Indexer {


    public static final FieldType TYPE_STORED = new FieldType();
    static {
        TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.freeze();
    }

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
                timeElapsed.toSeconds() % 60 +" seconds");
    }
    static final int NUMBER_OF_TABLES = 1653432;

    @SuppressWarnings("unchecked")
    private static int readTables(IndexWriter indexWriter) throws IOException, JSONException {
        ZipFile zipFile = new ZipFile("src\\main\\resources\\WP_tables.zip");
        ZipInputStream inputStream = new ZipInputStream(
                new FileInputStream("src\\main\\resources\\WP_tables.zip"));
        Reader zipReader = new InputStreamReader(inputStream);
        ZipEntry zipEntry ;
        inputStream.getNextEntry(); // skip the folder
        int indexingCounter = 0;

        FieldType fieldType = new FieldType();
        fieldType.setStoreTermVectors(true);
        fieldType.setStoreTermVectorPositions(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        fieldType.setStored(true);


        while ((zipEntry = inputStream.getNextEntry()) != null) {
            InputStreamReader tableListReader = new InputStreamReader(zipFile.getInputStream(zipEntry),StandardCharsets.UTF_8);
            BufferedReader streamReader = new BufferedReader(tableListReader);
            StringBuilder tableListStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                tableListStrBuilder.append(inputStr);
            JSONObject tableList = new JSONObject(tableListStrBuilder.toString());
            Iterator<String> idsIterator = tableList.sortedKeys();
            while(idsIterator.hasNext()) {
                String tableId = idsIterator.next();
                JSONObject table = (JSONObject) tableList.get(tableId);
                Document tableDocument = table2Doc(table, tableId);

                indexWriter.addDocument(tableDocument);
                double indexPercents = (double)indexingCounter/(double)NUMBER_OF_TABLES;
                System.out.println("Index Num: " + indexingCounter++ +
                        "      " + indexPercents + "%");
            }
        }

        return indexingCounter;
    }

    private static Document table2Doc(JSONObject table, String idString) throws JSONException {

        // extract table data
        String titleString = (String) table.get("pgTitle");
        String captionString;
        try {
            captionString = (String) table.get("caption");
        } catch (JSONException e) {
            captionString = "";
        }

        // extract headers data
        JSONArray headersArray = (JSONArray) table.get("title");
        int numberOfColumns = headersArray.length();
        List<String> headersStrings = new ArrayList<>();
        for (int columnNum = 0; columnNum < numberOfColumns; columnNum++)
            headersStrings.add((String) headersArray.get(columnNum));

        // extract cells data
        JSONArray tableData = ((JSONArray)table.get("data"));
        List<String> columnsStringsList = new ArrayList<>();
        for (int columnNum = 0; columnNum < numberOfColumns; columnNum++)
            columnsStringsList.add("");

        int numberOfRows = tableData.length();
        EntropyCalculator entropyCalc = new EntropyCalculator();

        for(int columnNum = 0; columnNum < numberOfColumns; columnNum++){

            for(int rowNum = 0; rowNum < numberOfRows; rowNum++){
                JSONArray tableRowArray = (JSONArray) tableData.get(rowNum);
                String cellString = (String)tableRowArray.get(columnNum);
                String oldColumnString = columnsStringsList.get(columnNum);
                columnsStringsList.set(columnNum, oldColumnString + "\n" +  cellString);

                entropyCalc.updateCount(cellString);
            }
            entropyCalc.calcColumnEntropy(columnNum);
        }
        entropyCalc.clacInteresting();

        return creatDocument(idString, titleString, captionString,headersStrings, columnsStringsList,
                entropyCalc.getInterestingness());
    }

    private static Document creatDocument(String idString, String titleString, String captionString,
                         List<String> headersStrings, List<String> columnsStringsList, float tableInterestingness){
        Document tableDocument = new Document();
        StringField idField = new StringField("id", idString, Field.Store.YES);
        TextField titleField = new TextField( "title", titleString, Field.Store.NO);

        TextField captionField = new TextField("caption", captionString, Field.Store.NO);
        tableDocument.add(idField);
        tableDocument.add(titleField);
        tableDocument.add(captionField);

        tableDocument.add(new FloatDocValuesField("interestingness", tableInterestingness));

        for (String headerString: headersStrings){
            TextField headerField = new TextField("header", headerString, Field.Store.NO);
            tableDocument.add(headerField);
        }
        for (String columnString: columnsStringsList){
            TextField columnField = new TextField("column", columnString, Field.Store.NO);
            Field content = new Field("content", columnString, TYPE_STORED);
            columnField.fieldType().storeTermVectors();
            tableDocument.add(columnField);
            tableDocument.add(content );

        }



        // table printing
//        printTable(idString, titleString, captionString,headersStrings, columnsStringsList,
//                tableInterestingness);

        return tableDocument;
    }

    private static void printTable(String idString, String titleString, String captionString,
                        List<String> headersStrings, List<String> columnsStringsList, float tableInterestingness){
        System.out.println("ID: " + idString + "\n"
                + "Title: " + titleString + "\n"
                + "Caption: " + captionString);
        int columnNum = 0;
        for (String headerString: headersStrings){
            System.out.println("----------------------");
            System.out.print( "Header " + columnNum + ": " + headerString);
            System.out.println(columnsStringsList.get(columnNum));
            columnNum++;
        }
    }


}
