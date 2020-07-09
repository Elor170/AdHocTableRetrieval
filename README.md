# AdHocTableRetrieval
## Indexing the tables
- **Required files:** WP_tables.zip file in the resources directory, can be download from [here](http://iai.group/downloads/smart_table/WP_tables.zip).
- **Run**: [Indexer](https://github.com/Elor170/AdHocTableRetrieval/blob/master/src/main/java/org/example/Indexer.java) file.
  - [EntropyCalculator](https://github.com/Elor170/AdHocTableRetrieval/blob/master/src/main/java/org/example/EntropyCalculator.java) class is used for the calculation of entropy and interestingness of a table.
## Searching the queries
- **Required files:** 
  - [queries.txt](https://github.com/Elor170/AdHocTableRetrieval/blob/master/src/main/resources/queries.txt) file in the resources directory (included in the repository). 
  - index folder in the resources directory (created while indexing the tables).
- **Run:**
[Searcher](https://github.com/Elor170/AdHocTableRetrieval/blob/master/src/main/java/org/example/Searcher.java) file.
- **Evaluation:**
  - [trec_eval](https://github.com/Elor170/AdHocTableRetrieval/tree/master/trec_eval) directory is used for evaluation of the search.
  - The search results are written to [result.txt](https://github.com/Elor170/AdHocTableRetrieval/blob/master/trec_eval/results.txt) file (while searching the queries). 
  - [trec_ndcg.bat](https://github.com/Elor170/AdHocTableRetrieval/blob/master/trec_eval/trec_ndcg.bat) run by the [Searcher](https://github.com/Elor170/AdHocTableRetrieval/blob/master/src/main/java/org/example/Searcher.java), and the NDCG measure is print in the command line.  
