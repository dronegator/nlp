import java.io.File

import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenMap}
import com.github.dronegator.nlp.main._
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.vocabulary.{VocabularyHintImpl, VocabularyImpl, VocabularyRawImpl}
import configs.syntax._

case class NLPTMainIndexConfig()

object NLPTMainIndex
  extends App
  with NLPTAppPartial
    with MainConfig[NLPTMainIndexConfig]
  with MainTools
  with Combinators {
  val fileIn :: fileOut ::  OptFile(hints) = args.toList

  lazy val cfg = config.get[NLPTMainIndexConfig]("index")

  lazy val vocabularyHint = hints.map(load(_): VocabularyImpl).getOrElse{
    println("Hints have initialized")
    VocabularyHintImpl(Tokenizer.MapOfPredefs, Map())
  }

  val source = io.Source.fromFile(new File(fileIn)).getLines()

  val (tokenMapIterator, tokenIterator) = source.
    component(splitterTool).
    flatten.
    componentScan(tokenizerTool).
    map {
      case (x, y, z) => ((x, y), z)
    }.
    unzip

  val (statement1, statement2, statement3, statement4, statement5, statement6, statement7, statement8) = tokenIterator.
   // log("token: ").
    component(accumulatorTool).
    fork8()

  val nGram1 = statement1.
    component(nGram1Tool)

  val nGram2 = statement2.
    component(nGram2Tool)

  val nGram3 = statement3.
    component(nGram3Tool)

  val phraseCorrelationRepeated = statement4.
    component(phraseCorrelationRepeatedTool)

  val phraseCorrelationConsequent = statement5.
    component(phraseCorrelationConsequentTool)

  val phraseCorrelationInner = statement6.
    component(phraseCorrelationInnerTool)

  val Some((tokenMap, lastToken)) = tokenMapIterator.
    foldLeft(Option.empty[(TokenMap, Token)]) {
      case (_, x) =>
        Option(x)
    }

  println("== 1 gramm ==")
  dump(nGram1)

  println("== 2 gramm ==")
  dump(nGram2)

  println("== 3 gramm ==")
  dump(nGram3)

  println("== Pairs of words from consequent phrases ==")
  dump(phraseCorrelationConsequent)

  println("== Pairs of words from the same phrase ==")
  dump(phraseCorrelationInner)

  println("== phrases ==")
  dump(statement7.toList)

  dump(tokenMap, lastToken)

  save(new File(fileOut), VocabularyRawImpl(tokenMap, vocabularyHint.meaningMap, statement8.toList, nGram1, nGram2, nGram3, phraseCorrelationRepeated, phraseCorrelationConsequent, phraseCorrelationInner))
}