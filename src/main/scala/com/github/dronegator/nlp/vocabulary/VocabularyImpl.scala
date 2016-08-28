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
      vocabulary.ngrams1,
      vocabulary.ngrams2,
      vocabulary.ngrams3,
      vocabulary.toToken,
      vocabulary.phraseCorrelationConsequent,
      vocabulary.phraseCorrelationInner
    )
}

class VocabularyImpl(phrases: List[List[Token]],
                     ngrams1: Map[List[Token], Int],
                     ngrams2: Map[List[Token], Int],
                     ngrams3: Map[List[Token], Int],
                     toToken: Map[Word, List[Token]],
                     phraseCorrelationConsequent: Map[List[Token], Int],
                     phraseCorrelationInner: Map[List[Token], Int])
  extends VocabularyRawImpl(phrases, ngrams1, ngrams2, ngrams3, toToken, phraseCorrelationConsequent, phraseCorrelationInner) with Vocabulary {
  override lazy val toWord: Map[Token, Word] =
    toToken.
      toIterator.
      flatMap {
        case (word, tokens) =>
          tokens.
            map(_ -> word).
            toIterator
      }.toMap

  private lazy val count1 = ngrams1.values.sum.toDouble

  override lazy val vtoken: Map[List[Token], Double] = {
    println("== 1")
    ngrams1.
      mapValues(_ / count1)
  }

  override lazy val vngrams2: Map[List[Token], Double] = {
    println("== 2")
    ngrams2 flatMap {
      case (key@(x :: y :: _), n) =>
        ngrams1.
          get(x :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val vngrams3: Map[List[Token], Double] = {
    println("== 3")
    ngrams3 flatMap {
      case (key@(x :: y :: z :: _), n) =>
        ngrams2.
          get(x :: y :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val vpgrams2: Map[List[Token], Double] = {
    println("== 2")
    ngrams2 flatMap {
      case (key@(x :: y :: _ ), n) =>
        ngrams1.
          get(y :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val vpgrams3: Map[List[Token], Double] = {
    println("== 3")
    ngrams3 flatMap {
      case (key@(x :: y :: z :: _), n) =>
        ngrams2.
          get(y :: z :: Nil).
          map { m => key -> (n / m.toDouble) }
    }
  }

  override lazy val vmiddle: Map[List[Token], List[(Double, Token)]] = {
    ngrams3.
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

  override lazy val vnext1: Map[List[Token], List[(Double, Token)]] = {
    println("=>4")
    vngrams2.
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

  override lazy val vnext2: Map[List[Token], List[(Double, Token)]] = {
    println("=>5")
    vngrams3.
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

  override lazy val vprev1: Map[List[Token], List[(Double, Token)]] = {
    println("=>4")
    vpgrams2.
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

  override lazy val vprev2: Map[List[Token], List[(Double, Token)]] = {
    println("=>5")
    vpgrams3.
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
      flatMap(toWord.get(_)).
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
        vtoken.get(token :: Nil).map(token -> _)
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
        vtoken.get(token :: Nil).map(token -> _)
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

  override lazy val vcnext: Map[List[Token], List[(Double, Token)]] =
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
