package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._

/**
 * Created by cray on 8/17/16.
 */

object VocabularyImpl {
  implicit def apply(vocabulary: VocabularyRaw) =
    new VocabularyImpl(
      vocabulary.phrases,
      vocabulary.nGram1,
      vocabulary.nGram2,
      vocabulary.nGram3,
      vocabulary.tokenMap,
      vocabulary.phraseCorrelationConsequent,
      vocabulary.phraseCorrelationInner
    )
}

class VocabularyImpl(phrases: List[List[Token]],
                     nGram1: Map[List[Token], Int],
                     nGram2: Map[List[Token], Int],
                     nGram3: Map[List[Token], Int],
                     tokenMap: Map[Word, List[Token]],
                     phraseCorrelationConsequent: Map[List[Token], Int],
                     phraseCorrelationInner: Map[List[Token], Int])
  extends VocabularyRawImpl(phrases, nGram1, nGram2, nGram3, tokenMap, phraseCorrelationConsequent, phraseCorrelationInner) with Vocabulary {
  override lazy val wordMap: Map[Token, Word] =
    tokenMap.
      toIterator.
      flatMap {
        case (word, tokens) =>
          tokens.
            map(_ -> word).
            toIterator
      }.toMap

  private lazy val count1 = nGram1.values.sum.toDouble

  override lazy val pToken: Map[List[Token], Double] = {
    println("== 1")
    nGram1.
      mapValues(_ / count1)
  }

  override lazy val pNGram2: Map[List[Token], Double] = {
    println("== 2")
    nGram2 flatMap {
      case (key@(x :: y :: _), n) =>
        nGram1.
          get(x :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val pNGram3: Map[List[Token], Double] = {
    println("== 3")
    nGram3 flatMap {
      case (key@(x :: y :: z :: _), n) =>
        nGram2.
          get(x :: y :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val pNGram2Prev: Map[List[Token], Double] = {
    println("== 2")
    nGram2 flatMap {
      case (key@(x :: y :: _ ), n) =>
        nGram1.
          get(y :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val pNGram3Prev: Map[List[Token], Double] = {
    println("== 3")
    nGram3 flatMap {
      case (key@(x :: y :: z :: _), n) =>
        nGram2.
          get(y :: z :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val map2ToMiddle: Map[List[Token], List[(Double, Token)]] = {
    nGram3.
      groupBy {
        case (x :: _ :: z :: _, n) =>
          x :: z :: Nil
      }.
      map{
        case (key, values) =>
          val delimiter = values.map(_._2).sum.toDouble

          key -> values.
            map{
              case (_ :: y :: _, count ) =>
                count / delimiter -> y
            }.
            toList.
            sortBy(_._1).
            reverse
      }
  }

  override lazy val map1ToNext: Map[List[Token], List[(Double, Token)]] = {
    println("=>4")
    pNGram2.
      toList.
      map {
        case (w1 :: w2 :: _, p) =>
          w1 -> (p -> w2)
      }.
      groupBy(_._1).
      map {
        case (key, value) =>
          List(key) -> value.
            map(_._2).
            sortBy(_._1)
      }
  }

  override lazy val map2ToNext: Map[List[Token], List[(Double, Token)]] = {
    println("=>5")
    pNGram3.
      toList.
      map {
        case (w1 :: w2 :: w3 :: _, p) =>
          (w1 :: w2 :: Nil) -> (p -> w3)
      }.
      groupBy(_._1).
      map {
        case (key, value) =>
          key -> value.
            map(_._2).
            sortBy(_._1)
      }
  }

  override lazy val map1ToPrev: Map[List[Token], List[(Double, Token)]] = {
    println("=>4")
    pNGram2Prev.
      toList.
      map {
        case (w1 :: w2 :: _, p) =>
          w2 -> (p -> w1)
      }.
      groupBy(_._1).
      map {
        case (key, value) =>
          List(key) -> value.
            map(_._2).
            sortBy(_._1)
      }
  }

  override lazy val map2ToPrev: Map[List[Token], List[(Double, Token)]] = {
    println("=>5")
    pNGram3Prev.
      toList.
      map {
        case (w1 :: w2 :: w3 :: _, p) =>
          (w2 :: w3 :: Nil) -> (p -> w1)
      }.
      groupBy(_._1).
      map {
        case (key, value) =>
          key -> value.
            map(_._2).
            sortBy(_._1)
      }
  }

  private def restorePhrase(phrase: List[Token]) =
    phrase.
      flatMap(wordMap.get(_)).
      mkString(" ")

  private def restoreSequence(phrase: List[Token], sequence: List[Token]) = {
    val order = phrase.
      zipWithIndex.
      toMap

    restorePhrase(sequence.sortBy(order(_)))
  }

  def filter(phrase: List[Token], requiredSignificance: Double, amount: Int = 2 ) = {
    val projection = phrase.
      flatMap { token =>
        pToken.get(token :: Nil).map(token -> _)
      }.
      foldLeft((0.0, Map[Token, Double]())) {
        case ((d, map), item@(_, p)) =>
          (d + p, map + item)
      } match {
      case (d, map) =>
        map.map {
          case (token, p) =>
            token -> (p / d)
        }
    }

    val vector = phrase.
      groupBy(identity).
      map {
        case (key, seq) =>
          key -> (seq.length / phrase.length.toDouble)
      }

    val sourceSignificance = (projection.keySet & vector.keySet).
      foldLeft(0.0) {
        case (production, token) =>
          production + vector(token) * projection(token)
      }

    // println(s"source significance = $sourceSignificance")

    if (requiredSignificance > sourceSignificance) {
      Some(
        (sourceSignificance, vector.
          map {
            case (token, probability) =>
              token -> (probability - projection.getOrElse(token, 0.0))
          }.
          toList.
//          map{x =>
//            println(x)
//            x
//          }.
          filter(_._2 > 0).
          sortBy(_._2).
          takeRight(amount))
      )
    } else None
  }

  def filter1(vector: Map[Token, Double], requiredSignificance: Double, amount: Int = 2 ) = {
    val projection = vector.
      map(_._1).
      flatMap { token =>
        pToken.get(token :: Nil).map(token -> _)
      }.
      foldLeft((0.0, Map[Token, Double]())) {
        case ((d, map), item@(_, p)) =>
          (d + p, map + item)
      } match {
      case (d, map) =>
        map.map {
          case (token, p) =>
            token -> (p / d)
        }
    }

    val sourceSignificance = (projection.keySet & vector.keySet).
      foldLeft(0.0) {
        case (production, token) =>
          production + vector(token) * projection(token)
      }

    // println(s"source significance = $sourceSignificance")

    if (requiredSignificance > sourceSignificance) {
      Some(
        (sourceSignificance, vector.
          map {
            case (token, probability) =>
              token -> (probability - projection.getOrElse(token, 0.0))
          }.
          toList.
//          map{x =>
//            println(x)
//            x
//          }.
          filter(_._2 > 0).
          sortBy(_._2).
          takeRight(amount))
      )
    } else None
  }

  override lazy val map1ToNextPhrase: Map[List[Token], List[(Double, Token)]] =
    phrases.
      map { phrase =>
        filter(phrase, 0.1) map (phrase -> _._2)
      }.
      sliding(2).
      collect{
        case Some(x) :: Some(y) :: _ =>
          x :: y :: Nil
      }.
      flatMap {
        case (p1, v1) :: (p2, v2) :: _ =>

          val tokens1 = v1.map(_._1)
          val tokens2 = v2.map(_._1)

//          println(
//            s""" --
//               | ${restorePhrase(p1)}
//               |   ${restoreSequence(p1, tokens1)}
//               |   ${v1.map{case (t, p) => toWord(t) -> p}.sortBy(_._2)}
//               |
//               | ${restorePhrase(p2)}
//               |   ${restoreSequence(p2, tokens2)}
//               |   ${v2.map { case (t, p) => toWord(t) -> p}.sortBy(_._2)}
//           """.stripMargin)
          for {
            token1 <- tokens1
            token2 <- tokens2
          } yield {
            token1 :: token2 :: Nil
          }
      }.foldLeft(Map[List[Token], Int]()) {
      case (map, key) =>
        map + (key -> (map.getOrElse(key, 0) + 1))
    } match {
      case map =>
        map.
          groupBy{
            case (key :: _, _) => key :: Nil
          }.
          map {
            case (key, map) =>
              val denominator = map.map(_._2).sum.toDouble
              key -> map.
                toList.
                map {
                  case (_ :: token :: _, count) =>
                    (count / denominator) -> token
                }
          }
    }


  /*{
    twoPhraseCorelator.
      toList.
      groupBy{
        case (x :: _, _) =>
          x :: Nil
      }.
      map{
        case (x, value) =>
          val entire = value.
            map(_._2).
            sum.
            toDouble

          x -> value.
            map{
              case (_ :: y :: _, n) =>
                n / entire -> y
            }.
            sortBy(_._1)
      }
  }*/
}
