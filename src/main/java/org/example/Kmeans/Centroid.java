package org.example.Kmeans;

import org.apache.lucene.index.Terms;

public class Centroid {
    Terms terms;
    int centroidId;
    int docId;

    public Centroid(int id,int docId, Terms terms) {
        this.terms = terms;
        this.centroidId = id;
        this.docId = docId;
    }

}
