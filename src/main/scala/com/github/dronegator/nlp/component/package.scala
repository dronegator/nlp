package com.github.dronegator.nlp

import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/14/16.
 */
package object component {
  trait Component[-A, +B] {
    def cfg: CFG

    def apply(in: A): B
  }
}
