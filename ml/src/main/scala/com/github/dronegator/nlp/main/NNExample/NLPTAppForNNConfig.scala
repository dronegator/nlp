package com.github.dronegator.nlp.main.NNExample

/**
  * Created by cray on 12/1/16.
  */
case class NLPTAppForNNConfig(nKlassen: Int = 20,
                              nGram: Option[Int] = None,
                              nSample: Option[Int],
                              regularization: Double = 0.01,
                              range: Double = 0.1,
                              maxIter: Int = 10,
                              crossValidationContext: Int = 10,
                              crossValidationWords: Int = 10,
                              useLBFGS: Boolean = true,
                              rfo: Double = 0.1,
                              tolerance: Double = 1E-02,
                              memoryLimit: Int = 7,
                              dropout: Int = 20)
