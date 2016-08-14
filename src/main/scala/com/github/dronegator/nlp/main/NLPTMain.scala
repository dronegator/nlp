import java.io.File

import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.utils.CFG

object NPLTMain extends App {

  val Array(file) = args

  val cfg = CFG(

  )

  val source = io.Source.fromFile(new File(file)).getLines()

  val tokenizer = new Tokenizer(cfg, None)

  source.map(tokenizer.apply _).flatten foreach {
    case (_, x) =>
      println(x)
      x
  }

}