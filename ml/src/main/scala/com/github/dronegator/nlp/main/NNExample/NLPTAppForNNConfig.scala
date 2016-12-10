package com.github.dronegator.nlp.main.NNExample

/**
  * Created by cray on 12/1/16.
  */
case class NLPTAppForNNConfig(nKlassen: Int,
                              nGram: Option[Int],
                              nSample: Option[Int],
                              regularization: Double,
                              range: Double,
                              maxIter: Int,
                              crossValidationContext: Int, // = 10,
                              crossValidationWords: Int, // = 10,
                              winnerGetsAll: Boolean,
                              useLBFGS: Boolean,
                              rfo: Double,
                              tolerance: Double,
                              memoryLimit: Int,
                              dropout: Int)
