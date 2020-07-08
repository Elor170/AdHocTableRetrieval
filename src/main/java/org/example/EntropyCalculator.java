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

public class EntropyCalculator {
    private Map<String, Integer> wordsColumnCount;
    private float tableEntropy;
    private float interestingness;
    private final double DELTA = 0.9;
    private final double ALPHA = 0.1;

    public EntropyCalculator() {

        this.wordsColumnCount = new HashMap<>();
        this.tableEntropy = 0;
    }

    public void calcColumnEntropy(int columnNum){
        float entropyOfColumn = 0;
        float wordEntropy;
        int sumOfCount = 0;
        for(Map.Entry<String, Integer> singleWordCount: wordsColumnCount.entrySet())
            sumOfCount += singleWordCount.getValue();

        for(Map.Entry<String, Integer> singleWordCount: wordsColumnCount.entrySet()){
            float prOfWord = ((float)singleWordCount.getValue())/(float) sumOfCount;
            wordEntropy = (float) (prOfWord * (Math.log(prOfWord) / Math.log(2)));
            entropyOfColumn -= wordEntropy;
        }
        tableEntropy += entropyOfColumn * Math.pow(DELTA, columnNum);
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


    static public List<String> string2Words(String text, Analyzer analyzer){
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

    public void clacInteresting(){
        this.interestingness = (float) (ALPHA + Math.pow(tableEntropy, ALPHA));
    }

    public float getInterestingness() {
        return interestingness;
    }

}
