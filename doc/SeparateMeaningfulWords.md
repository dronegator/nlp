How to separate meaningful and not-meaningful words
=======================================================
I classify words by means of their closest context, i.e. the words right before and after the questionable one. The first iterations involves a human-written example of the service and meaningful words. I used this sample:
  
  * Service words:
  
        mine he him his she her you yours 
        am is are was were be being 
        have has had 
        do did does 
        not no yes 
        there those that 
        the a an 
        where when what who whose whom 
        on in about over above for with without 
        never nobody one some somebody
        to too also
     
  * Meaningful words:
        
        apple meat bread soup soap water tea coffee 
        knife spoon fork 
        shirt skirt coat shoes pants panties
        cat dog kitten cow rat
        wife husband son daughter sister brother
        boss cook secretary captain police
        teacher professor doctor nurse nun
        car auto rocket missile robot 
        alien spaceship ship space star sun planet sky moon earth
        tree growth 
        cloud rain frost weather day
        week morning evening dinner breakfast lunch 
        girl boy man woman
        bed table chair bench

I just wrote them, but one can use [Dolch word list](https://en.wikipedia.org/wiki/Dolch_word_list) or something like that.

After a learning phase, program can suggest a wordlist with weights in range [-1, 1], that supposed to an assurance the word is meaningful or just sevice. It allows fast expand inital sample up to 300 words:

  * [Meaningful words](../data/hints/english/sense.txt)
        
  * [Service words](../data/hints/english/nonsense.txt)    

Having this sample the program can be learn to select meaningful keywords from any phrase (incliuding the words it has never seen before). This function helped to substantially improve suggestion of the words for the next phrase, completely filtering out all service words that do not make much help to write the next phrase.

A few examples followed:

    > keywords My cat was drinking a milk.
    a                    -1.000 (0.000-1.000)
    drinking             -0.275 (0.000-0.276)
    was                  0.000 (0.000-0.000)
    milk                 0.088 (0.097-0.008)
    cat                  0.114 (0.117-0.003)
            
I guess it is gonna be a bit challenging for the German though ^^.            