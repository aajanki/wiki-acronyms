# Introduction

This script mines Wikipedia for
[backronyms](https://en.wikipedia.org/wiki/Backronym) or word
sequences where the consecutive words start with the given letters.
Often the results are quite funny.

For example, backronyms for LOL are word triplets with words starting
with the letters L, O and L, such as "loss of life" and "Lady of
Lourdes".

# Installing

## Required software

* [sbt](http://www.scala-sbt.org/)
* [Apache Spark](https://spark.apache.org/)
* [wikipedia-extractor](https://github.com/bwbaugh/wikipedia-extractor)

## Downloading and preprocessing the Wikipedia corpus

```sh
wget https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2
git clone git@github.com:bwbaugh/wikipedia-extractor.git
python wikipedia-extractor/WikiExtractor.py -b 10M -c --threads 4 -o wikitexts enwiki-latest-pages-articles.xml.bz2
```

A sample of the final preprocessed format is included in the directory
wikitexts.sample.

## Compiling

```sh
sbt package
```

# Usage

Submit the compiled JAR file to a Spark instance. The application
expects two arguments: a path to the preprocessed Wikipedia files and
a comma-separated list of acronyms.

```sh
$SPARKDIR/bin/spark-submit --class AcronymApp wiki-acronyms.jar wiki_files acronyms
```

## Example

To find explanations for the acronyms LOL, WTF and FAQ on a local
Spark instance, run:

```sh
$SPARKDIR/bin/spark-submit --class AcronymApp --master local[4] --driver-memory 1G --conf spark.storage.memoryFraction=0.05 target/scala-2.11/wiki-acronyms_2.11-1.0.jar 'wikitexts/*/wiki*.bz2' 'LOL,WTF,FAQ' > results
```
