// Copyright (c) 2015 Antti Ajanki <antti.ajanki@iki.fi>
// The MIT License

import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

case class CommandLineArguments(acronyms: Traversable[String], wikiFiles: String)

object AcronymApp {
  def main(args: Array[String]): Unit = {
    val arguments = parseArguments(args)
    val numExplanation = 5
    val conf = new SparkConf().setAppName("wiki-acronyms")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    val sc = new SparkContext(conf)
    val wordsPerArticle = wikiArticles(sc, arguments.wikiFiles)
      .map(splitWords)
      .map(_.map(cleanupWord).filter(_.nonEmpty))
      .persist(StorageLevel.MEMORY_AND_DISK_SER)

    arguments.acronyms.foreach { acronym =>
      val cleanedAcronym = acronym.toLowerCase.replaceAll("[^a-z0-9]", "")
      val explanations = wordsPerArticle
        .flatMap(findAcronyms(_, cleanedAcronym))
        .map((_, 1))
        .reduceByKey(_ + _)
        .top(numExplanation)(Ordering[Int].on(_._2))
      printExplanations(explanations, acronym)
    }
  }

  def parseArguments(args: Array[String]) = {
    def usage() = {
      System.err.println("""Usage: acronymApp wiki_files acronyms""")
      sys.exit(1)
    }

    args.toList match {
      case path :: acronyms :: Nil =>
        CommandLineArguments(acronyms = acronyms.split(',').map(_.trim), wikiFiles = path)
      case _ =>
        usage()
    }
  }

  def wikiArticles(sc: SparkContext, wikiFiles: String) =
    sc.wholeTextFiles(wikiFiles, 8).map(_._2).flatMap(splitPages)

  def splitPages(s: String): Array[String] =
    s.replaceAll("(?m)^</doc>$", "").split("(?m)^<doc .+$").filter(_.nonEmpty)

  def splitWords(text: String): Array[String] =
    text.split("\\s").filter(_.nonEmpty)

  def cleanupWord(word: String): String =
    word.toLowerCase.replaceAll("^\\p{Punct}*|\\p{Punct}*$", "")

  def findAcronyms(words: Iterable[String], acronym: String): Iterator[String] =
    words
      .sliding(acronym.length)
      .filter(_.flatMap(_.headOption).mkString == acronym)
      .map(_.mkString(" "))

  def printExplanations(explanations: Array[(String, Int)], acronym: String) =
    if (explanations.isEmpty) {
      println(s"No explanation found for %s\n".format(acronym))
    } else {
      val prettyResult = explanations.map(x => x._1 + "\t" + x._2).mkString("\n")
      println("Top explanations for %s:\n%s\n".format(acronym, prettyResult))
    }

}
