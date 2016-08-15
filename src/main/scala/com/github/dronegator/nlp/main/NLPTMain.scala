import java.io.File

import com.github.dronegator.nlp.component.splitter.Splitter
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

  source.
    map(splitter(_)).
    flatten.
    scanLeft((map, n, List[Tokenizer.Token]()))(tokenizer(_, _)).
    zipWithIndex.
    map{
      case ((map, _, x), n) =>
        println(f"$n%-10d : ${x.mkString(" :: ")}")
        map
    }.
    foldLeft(Option.empty[Tokenizer.TokenMap]) {
      case (_, x) =>
        Option(x)
    }.
    foreach { map =>
      map.
        toList.
        sortBy(_._1).
        foreach {
        case (key, value :: _) =>
          println(f"$key%-60s:$value%010d")
      }
    }
}