package com.github.dronegator.nlp.main.NNExample

/**
  * Created by cray on 12/1/16.
  */
case class NLPTAppForNNConfig(nKlassen: Int = 20,
                              nGram: Option[Int] = None,
                              nSample: Option[Int],
                              regularization: Double = 0.01,
                              maxIter: Int = 10)
