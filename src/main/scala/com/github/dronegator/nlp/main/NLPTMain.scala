import java.io.File

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenMap}
import com.github.dronegator.nlp.main.MainTools
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.vocabulary.VocabularyRawImpl

object NLPTMain
  extends App
  with MainTools {

  val Array(fileIn, fileOut) = args

  val source = io.Source.fromFile(new File(fileIn)).getLines()

  lazy val cfg = CFG()

  val (tokenMapIterator, tokenIterator) = source.
    component(splitterTool).
    flatten.
    componentScan(tokenizerTool).
    map {
      case (x, y, z) => ((x, y), z)
    }.
    unzip

  val (statement1, statement2, statement3, statement4, statement5) = tokenIterator.
   // log("token: ").
    componentScan(accumulatorTool).
    collect {
      case (_, Some(statement)) => statement
    }.fork5()

  val nGram1 = statement1.
    componentFold(nGram1Tool)

  val nGram2 = statement2.
    componentFold(nGram2Tool)

  val nGram3 = statement3.
    componentFold(nGram3Tool)

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

  println("== phrases ==")
  dump(statement4.toList)

  dump(tokenMap, lastToken)

  save(new File(fileOut), VocabularyRawImpl(statement5.toList, nGram1, nGram2, nGram3, tokenMap, ???, ???))
}