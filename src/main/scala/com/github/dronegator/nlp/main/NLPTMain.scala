import java.io.File

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

  tokenVariances.
    toIterator.
    zipWithIndex.
    foreach{
      case (tokens, n) =>
        println(f"$n%-10d : ${tokens.mkString(" :: ")}")
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

        println(s"Lastt token = $token")
    }
}