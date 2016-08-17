import java.io.File

import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{TokenMap, Token}
import com.github.dronegator.nlp.component.tokenizer.{Tokenizer}
import com.github.dronegator.nlp.utils.CFG

object NLPTMain extends App {

  val Array(file) = args

  val cfg = CFG()

  val source = io.Source.fromFile(new File(file)).getLines()

  val splitter = new Splitter(cfg)

  val tokenizer = new Tokenizer(cfg, None)

  val phraseDetector = new PhraseDetector(cfg)

  val accumulator = new Accumulator(cfg, phraseDetector)

  val ngramms1 = new NGramsCounter(cfg, 1)

  val ngramms2 = new NGramsCounter(cfg, 2)

  val ngramms3 = new NGramsCounter(cfg, 3)

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
    toStream

  implicit val ordering = Ordering.
    fromLessThan((x: List[Int], y: List[Int]) => (x zip y).find(x => x._1 != x._2).map(x => x._1 < x._2).getOrElse(false))

  phrases.
    toIterator.
    foldLeft(Map[List[Token], Int]())(ngramms1(_, _)) match {
      case map =>
        println("== 1 gramm ==")
        map.toList.sortBy(_._1).foreach {
          case (key, value) =>
            println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
        }
      }

  phrases.
    toIterator.
    foldLeft(Map[List[Token], Int]())(ngramms2(_, _)) match {
    case map =>
      println("== 2 gramm ==")
      map.toList.sortBy(_._1).foreach {
        case (key, value) =>
          println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
      }
  }

  phrases.
    toIterator.
    foldLeft(Map[List[Token], Int]())(ngramms3(_, _)) match {
    case map =>
      println("== 3 gramm ==")
      map.toList.sortBy(_._1).foreach {
        case (key, value) =>
          println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
      }
  }

  maps.
    toIterator.
    foldLeft(Option.empty[(TokenMap, Token)]) {
      case (_, x) =>
        Option(x)
    }.
    foreach {
      case (map, token) =>
        map.
          toList.
          sortBy(_._1).
          foreach {
            case (key, value :: _) =>
              println(f"$key%-60s:$value%010d")
          }

        println(s"Last token = $token")
    }
}