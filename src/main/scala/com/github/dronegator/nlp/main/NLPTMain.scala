import java.io.File

import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.utils.CFG

object NPLTMain extends App {

  val Array(file) = args

  val cfg = CFG(

  )

  val source = io.Source.fromFile(new File(file)).getLines()

  val tokenizer = new Tokenizer(cfg, None)

  val map = Tokenizer.MapOfPredefs

  val n = map.valuesIterator.flatten.max

  (source.scanLeft((map, n, List[Tokenizer.Token]()))(tokenizer(_, _)) map {
    case (map, _, x) =>
      println(x)
      map
  } toList).lastOption foreach { map =>
    println(map)
  }
}