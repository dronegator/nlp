import java.io.File

import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{TokenMap, Token}
import com.github.dronegator.nlp.component.tokenizer.{Tokenizer}
import com.github.dronegator.nlp.main.MainTools
import com.github.dronegator.nlp.main.NLTPMainStream._
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.VocabularyRawImpl

object NLPTMain
  extends App
  with MainTools {

  val Array(fileIn, fileOut) = args

  val source = io.Source.fromFile(new File(fileIn)).getLines()

  val cfg = CFG()

  val map = Tokenizer.MapOfPredefs

  val n = map.valuesIterator.flatten.max

  val (maps,tokenVariances) = source.
    map(splitter(_)).
    flatten.
    scanLeft((map, n, List[Tokenizer.Token]()))(tokenizer(_, _)).
    map{
      case (x, y, z) => ((x, y), z)
    }.toStream.
    unzip

  val phrases = tokenVariances.
    toIterator.
    zipWithIndex.
    map{
      case (tokens, n) =>
        //println(f"$n%-10d : ${tokens.mkString(" :: ")}")
        tokens
    }.
    scanLeft((List.empty[List[Token]], Option.empty[List[Token]]))(accumulator(_, _)).
    collect{
      case (_, Some(phrase)) => phrase
    }.
    toList

  val ngram1 = phrases.
    toIterator.
    foldLeft(Map[List[Token], Int]())(ngramms1(_, _))

  val ngram2 = phrases.
    toIterator.
    foldLeft(Map[List[Token], Int]())(ngramms2(_, _))

  val ngram3 = phrases.
    toIterator.
    foldLeft(Map[List[Token], Int]())(ngramms3(_, _))

  val Some((toToken, lastToken)) = maps.
    toIterator.
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
  dump(phrases)

  dump(toToken, lastToken)

  save(new File(fileOut), VocabularyRawImpl(phrases, ngram1, ngram2, ngram3, toToken))
}