package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.VocabularyRawImpl

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
      vocabulary.twoPhraseCorelator
    )
}

class VocabularyImpl(phrases: List[List[Token]],
                     ngrams1: Map[List[Token], Int],
                     ngrams2: Map[List[Token], Int],
                     ngrams3: Map[List[Token], Int],
                     toToken: Map[Word, List[Token]],
                     twoPhraseCorelator: Map[List[Token], Int]                      )
  extends VocabularyRawImpl(phrases, ngrams1, ngrams2, ngrams3, toToken, twoPhraseCorelator) with Vocabulary {
  override lazy val toWord: Map[Token, Word] =
    toToken.
      toIterator.
      flatMap{
        case (word, tokens) =>
          tokens.
            map( _ -> word).
            toIterator
      }.toMap

  private lazy val count1 = ngrams1.values.sum.toDouble

  override lazy val vngrams1: Map[List[Token], Double] = {
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
          map { m => key -> (n / m.toDouble)
          }
    }
  }

  override lazy val vnext1: Map[List[Token], List[(Double, Token)]] = {
    println("=>4")
    vngrams2.
      toList.
      map{
        case (w1 :: w2 :: _, p) =>
          w1 -> (p -> w2)
      }.
      groupBy(_._1).
      map{
        case (key, value) =>
          List(key) -> value.
            map(_._2).
            sortBy(_._1)
      }
  }

  override lazy val vnext2: Map[List[Token], List[(Double,Token)]] = {
    println("=>5")
    vngrams3.
      toList.
      map{
        case (w1 :: w2 :: w3 :: _, p) =>
          (w1 :: w2 :: Nil)-> (p -> w3)
      }.
      groupBy(_._1).
      map{
        case (key,value) =>
          key -> value.
            map(_._2).
            sortBy(_._1)
      }
  }

  override def vcor: Map[List[Token], Double] = ???

  override def vcnext: Map[List[Token], List[(Double, Token)]] = {
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
  }
}
