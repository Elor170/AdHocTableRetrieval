package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//TODO add alpha and delta to table entropy

public class EntropyCalculator {
    private Map<String, Integer> wordsColumnCount;
    private double tableEntropy;

    public EntropyCalculator() {
        this.wordsColumnCount = new HashMap<>();
        this.tableEntropy = 0;
    }

    public void calcColumnEntropy(){
        double entropyOfColumn = 0;
        double wordEntropy;
        int sumOfCount = 0;
        for(Map.Entry<String, Integer> singleWordCount: wordsColumnCount.entrySet())
            sumOfCount += singleWordCount.getValue();

        for(Map.Entry<String, Integer> singleWordCount: wordsColumnCount.entrySet()){
            double prOfWord = ((double)singleWordCount.getValue())/sumOfCount;
            wordEntropy = prOfWord * (Math.log(prOfWord) / Math.log(2));
            entropyOfColumn -= wordEntropy;
        }
        tableEntropy += entropyOfColumn;
    }

    public void updateCount(String cellString){
        List<String> cellWords = string2Words(cellString, new EnglishAnalyzer());
        for (String word: cellWords){
            if(wordsColumnCount.containsKey(word)) {
                int oldCount = wordsColumnCount.get(word);
                wordsColumnCount.replace(word, oldCount + 1);
            }
            else
                wordsColumnCount.put(word, 1);
        }
    }


    static private List<String> string2Words(String text, Analyzer analyzer){
        List<String> result = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream("---", text);
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                result.add(attr.toString());
            }
            tokenStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public double getTableEntropy() {
        return tableEntropy;
    }
}
