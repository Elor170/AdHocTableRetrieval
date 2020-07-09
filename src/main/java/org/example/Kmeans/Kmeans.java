package org.example.Kmeans;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class Kmeans {
    public static final String CONTENT = "content";

    private static IndexReader reader;


    private static final Random random = new Random();

    public static void main(String[] args) {

        try {

            LocalDateTime start = LocalDateTime.now();
            Directory indexDirectory = FSDirectory.open(Paths.get("src\\main\\resources\\index"));
            IndexReader indexReader = DirectoryReader.open(indexDirectory);
            reader = indexReader;

         //   List<Centroid> centroids = randomCentroids(10000);

            Map<Centroid, List<Integer>> listMap = fit(1000, 15);

            LocalDateTime end = LocalDateTime.now();
            Duration timeElapsed = Duration.between(start, end);
            System.out.println("Time taken: " + timeElapsed.toMinutes() + " minutes " +
                    timeElapsed.toSeconds() % 60 + " seconds");


        } catch (IOException e) {
        }
    }


    public static Map<Centroid, List<Integer>> fit(int k, int maxIterations) throws IOException {


        List<Centroid> centroids = randomCentroids(k);
        Map<Centroid, List<Integer>> clusters = new HashMap<>();
        Map<Centroid, List<Integer>> lastState = new HashMap<>();

        // iterate for a pre-defined number of times
        for (int itr = 0; itr < maxIterations; itr++) {
            boolean isLastIteration = itr == maxIterations - 1;

            // in each iteration we should find the nearest centroid for each record
            for (int i=0; i<reader.maxDoc(); i++) {
                System.out.println("docId: "+ i);
                Centroid closest = nearestCentroid(i, centroids);
                assignToCluster(clusters, i, closest);
            }

            // if the assignments do not change, then the algorithm terminates
            boolean shouldTerminate = isLastIteration || clusters.equals(lastState);
            lastState = clusters;

            if (shouldTerminate) {
                break;
            }

            // at the end of each iteration we should relocate the centroids
            centroids = clusters.entrySet().stream().map(e-> chooseNewCentroid(e.getKey(), e.getValue())).collect(toList());
            clusters = new HashMap<>();
        }

        return lastState;
    }



    private static List<Centroid> randomCentroids(int k) {

        List<Centroid> centroids = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger();

        ThreadLocalRandom.current().ints(0, reader.maxDoc()).distinct()
                .limit(k).forEach(x -> {
            try {
                centroids.add(new Centroid(counter.incrementAndGet(),x, reader.getTermVector(x, CONTENT)));
            } catch (IOException e) {
            }
        });

        return centroids;
    }

    private static Centroid nearestCentroid(int docNum, List<Centroid> centroids) throws IOException {
        double maxSim = -Double.MAX_VALUE;
        Centroid nearest = null;

        for (Centroid centroid : centroids) {
            try {
                double currentDistance = DocumentDistance.getCosineSimilarity(reader, reader.getTermVector(docNum, CONTENT),
                        centroid.terms);

                if (currentDistance > maxSim) {
                    maxSim = currentDistance;
                    nearest = centroid;
                }
            }
            catch (NullPointerException e){
                System.out.println(centroid.centroidId);
            }

        }

        return nearest;
    }

    private static void assignToCluster(Map<Centroid, List<Integer>> clusters,
                                        int docNum,
                                        Centroid centroid) {

        clusters.compute(centroid, (key, list) -> {
            if (list == null) {
                list = new ArrayList<>();
            }

            list.add(docNum);
            return list;
        });
    }

    private static Centroid chooseNewCentroid(Centroid centroid, List<Integer> docs) {
        if (docs == null || docs.isEmpty()) {
            return centroid;
        }

        int randomId = random.nextInt(docs.size());

        try {
            return new Centroid(centroid.centroidId,
                                docs.get(randomId),
                                reader.getTermVector(docs.get(randomId), CONTENT));
        } catch (IOException e) {
            return null;
        }
    }

}
