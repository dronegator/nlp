package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Phrase, Statement, Token, TokenPreDef}
import com.github.dronegator.nlp.main.NLPTReplMain._
import com.github.dronegator.nlp.utils.RandomUtils._
/**
 * Created by cray on 8/28/16.
 */
object VocabularyTools {

  case class Advice(token: Option[Token], tokens: List[(Token, Probability)], removal: Option[Probability])

  type Advices = List[Advice]

  class VocabularyTools(vocabulary: VocabularyImpl) {
    def generatePhrase(tokens: List[Token]): Option[Statement] =
      Iterator.
        iterate(tokens) {
          case tokens@_ :: Nil =>
            tokens :+ vocabulary.map1ToNext(tokens).
              choiceOption.getOrElse(TokenPreDef.PEnd.value)

          case tokens =>
            tokens :+ vocabulary.map2ToNext(tokens.takeRight(2)).choiceOption.getOrElse(TokenPreDef.PEnd.value)
        }.
        takeWhile(x => !(x.lastOption contains TokenPreDef.PEnd.value)).
        take(20).
        toList.
        lastOption.
        flatMap { tokens =>
          Iterator.
            iterate(tokens) {
              case tokens@_ :: Nil =>
                vocabulary.map1ToPrev(tokens).
                  choiceOption.getOrElse(TokenPreDef.PStart.value) :: tokens

              case tokens@x :: y :: _ =>
                vocabulary.map2ToPrev(x :: y :: Nil).choiceOption.getOrElse(TokenPreDef.PStart.value) :: tokens

            }.
            takeWhile(x => !(x.headOption contains TokenPreDef.PStart.value)).
            take(20).
            toList.
            lastOption
        }


    private def swap(value: (Probability, Token)) = value match {
      case (p, t) => (t, p)
    }

    def continueStatement(statement: Statement): List[(Token, Probability)] =  {
      println(statement)
      statement.takeRight(2) match {
        case token1 :: Nil =>
          vocabulary.map1ToNext.get(token1 :: Nil).getOrElse(List()).map(swap)

        case token1 :: token2 :: Nil =>
          vocabulary.map2ToNext.get(token1 :: token2 :: Nil).
            orElse(vocabulary.map1ToNext.get(token1 :: Nil)).
            getOrElse(List()).map(swap)
      }

    }

    def prependPhrase(statement: Statement): List[(Token, Probability)] = ???

    def advice(statement: Statement): Advices = ???

    @deprecated("Use advice instead", "v.0.2")
    def advicePlain(statement: Statement): Iterator[(List[(Statement, Double)], Int)] =
      statement.
        sliding(3).
        zipWithIndex.
        collect {
          case (x :: y :: z :: Nil, n) =>
            (x, y, z, n, statement)
        }.
        map {
          case (x, y, z, n, statement) =>

            val (start, token :: end) = statement.splitAt(n + 1)

            vocabulary.map2ToMiddle.get(x :: z :: Nil).
              toList.
              flatten.
              takeWhile(_._2 != token).
              take(4).
              map {
                case (d, advice) =>
                  (start ++ (advice :: end), d)
              } -> n
        }

    def suggestForNext(statement: Statement): List[(Token, Probability)] = {
      val advice = (for {
        (token1, _) <-
        vocabulary.filter(statement, 2, 10).
          toList.
          flatMap(_._2)
        (p, nextToken) <- vocabulary.map1ToNextPhrase.get(token1 :: Nil).toList.flatten
      } yield {
          nextToken -> p
        }).
        foldLeft(Map[Token, Double]()) {
          case (map, (token, p)) =>
            map + (token -> (p + map.getOrElse(token, 0.0)))
        }.
        toList

      vocabulary.filter1(advice.toMap, 2, 10).
        map(_._2).
        toList.
        flatten.
        sortBy(_._2)
    }

    def suggestForTheSame(statement: Statement): List[(Token, Probability)] = ???

    def probability(statement: Statement): Probability =
      statement.
        sliding(3).
        map {
          case Nil =>
            1.0
          case tokens@(_ :: Nil) =>
            vocabulary.pToken.get(tokens).getOrElse(0.0)

          case tokens@(_ :: _ :: Nil) =>
            vocabulary.pNGram2.get(tokens).getOrElse(0.0)

          case tokens =>
            vocabulary.pNGram3.get(tokens.take(3)).getOrElse(0.0)
        }.
        reduceOption(_ * _).
        getOrElse(1.0)

    def tokenize(s: String): Statement = {
      val tokens = splitterTool(s).
        scanLeft((vocabulary.tokenMap, 100000000, tokenizerTool.init._3))(tokenizerTool).
        map {
          case (_, _, tokens) => tokens
        }.
        toList :+ List(TokenPreDef.TEnd.value)

      val statement = tokens.
        toIterator.
        scanLeft(accumulatorTool.init)(accumulatorTool).
        collectFirst {
          case (_, Some(statement)) => statement
        }.
        toList.
        flatten

      statement
    }

    def tokenize(phrase: Phrase): Statement =
      tokenize(phrase.mkString(" "))

    def tokenizeShort(s: String): Statement =
      tokenize(s).drop(2).dropRight(1)

    def tokenizeShort(phrase: Phrase): Statement =
      tokenizeShort(phrase.mkString(" "))

    def untokenize(tokens: List[Token]) =
      tokens.flatMap(vocabulary.wordMap.get(_)).mkString(" ")

  }


}
