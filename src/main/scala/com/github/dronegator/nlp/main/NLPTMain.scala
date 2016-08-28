import java.io.File

import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{TokenMap, Token}
import com.github.dronegator.nlp.component.tokenizer.{Tokenizer}
import com.github.dronegator.nlp.main.MainTools
import com.github.dronegator.nlp.main.NLPTMainStream._
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.vocabulary.VocabularyRawImpl

object NLPTMain
  extends App
  with MainTools {


  val Array(fileIn, fileOut) = args

  val source = io.Source.fromFile(new File(fileIn)).getLines()

  lazy val cfg = CFG()

  val (maps,tokenVariances) = source.
    component(splitterTool).
    // map(splitter).
    flatten.
    //scanLeft(tokenizer.init)(tokenizer).
    componentScan(tokenizerTool).
    map{
      case (x, y, z) => ((x, y), z)
    }.
    unzip

  val (phrases1, phrases2, phrases3, phrases4, phrases5) = tokenVariances.
    zipWithIndex.
   // log("qqq: ").
    map{
      case (tokens, n) =>
        //println(f"$n%-10d : ${tokens.mkString(" :: ")}")
        tokens
    }.
    scanLeft(accumulatorTool.init)(accumulatorTool).
    collect{
      case (_, Some(phrase)) => phrase
    }.fork5()

  val ngram1 = phrases1.
    //componentFold(ngramms1)
    foldLeft(nGram1Tool.init)(nGram1Tool)

  val ngram2 = phrases2.
    foldLeft(nGram2Tool.init)(nGram2Tool)

  val ngram3 = phrases3.
    foldLeft(nGram2Tool.init)(nGram3Tool)

  val Some((toToken, lastToken)) = maps.
    foldLeft(Option.empty[(TokenMap, Token)]) {
      case (_, x) =>
        Option(x)
    }

  println("== 1 gramm ==")
  dump(ngram1)

  println("== 2 gramm ==")
  dump(ngram2)

  println("== 2 gramm ==")
  dump(ngram3)

  println("== phrases ==")
  dump(phrases4.toList)

  dump(toToken, lastToken)

  save(new File(fileOut), VocabularyRawImpl(phrases5.toList, ngram1, ngram2, ngram3, toToken, ???, ???))
}