Demystification of keywords extraction 
========================================

An algorithm can learn to classify words between groups auxiliary and meaningful on the base of their closest context - the words before and after. This approach addresses to  a substantial issue of natural language processing: everything is possible to meet in a large text corpus, but the most frequent cases bring only a small piece of information in the text.

Entropy-based metrics, such as TD-IDF, are supposed to address exactly that statistical problem, but helping a lot they leave the whole issue mainly intact: the results contain enough of useless noise to distract and upset a human consumer. 

The idea of classification is quite different. Instead of analyzing some statistical metrics to weight the words involved into processing, the algorithm just learns to classify words between two categories, defined by a sample, proposed by a human consumer. It gives better results  since, besides any logical criteria, only a consumer can decide which word is more useful to describe a text.

Categories can be different, ones think the nouns are more meaningful than pronouns and even verbs, the others point out a fact that for some languages (especially germanic ones) verbs are mostly important for getting a meaning from a grammar structure.

Actually, there are a few samples of professionally written small vocabularies for language learners, such as  [Dolch word list](https://en.wikipedia.org/wiki/Dolch_word_list), but I wrote down a few dozens of nouns related to the basic human needs (except sex and violence), and a few dozens of auxiliary words that anyone can get from the first chapter of any grammar handbook.

  * Auxiliary words:
  
        mine he him his she her you yours 
        am is are was were be being 
        have has had 
        do did does
        going giving taking making 
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

A simple neural network of 2 (two) neurons takes a vector of context (each dimension means one of a million possible pairs of words before and after the checked one) to  choose if the word is auxiliary or meaningful. Even without any sophisticated learning techniques, the network gives a tremendous outcome: It can correctly classify most of the words, that absent in the sample. It allows extending a sample up to 300 words giving a way to extract keywords from a sample.

  * [Meaningful words](../data/hints/english/sense.txt)
        
  * [Auxiliary words](../data/hints/english/auxiliary.txt) 

A few examples
---------------

A command *meaning* takes two files of samples for auxiliary and meaningful words, extracts possible context from a text corpus to learn how to classify them. Than it produces a file of other words, which are weighted between 1 ("meaningful") and -1 ("auxiliary").


    > meaning meaningful.txt auxiliary.txt words.txt
    
For 200MB of a text corpus it gives 50000 words, ranged from -1 to one, such as:
         
  - This supposed to have an auxiliary role in a text:
  
        shoulda -0.882098609150
        havena -0.865894721591
        hadna -0.864526866806
        recognisably -0.708383173844
        censorious -0.695518123026
        yeh -0.642973856209
        thinnish -0.586348009612
        hoary -0.567293112570
        xenophobic -0.555261652845
        havent -0.537787648232
        yous -0.535286935287
        deciduous -0.518020033734
        uncontaminated -0.513888888889
        romancing -0.509445148125
        untempered -0.499297240462
        affords -0.485949839446
        leprechauns -0.477145158448
        hazarding -0.476848394324
        drizzly -0.474192902843
        
   - This supposed to be meaningful:
           
        gunfighter 0.260501001001
        fiddler 0.246559260434
        sommelier 0.229535021318
        airlift 0.227259480529
        swindler 0.224637681159
        pigments 0.220760233918
        laundromat 0.216538849958
        newsreader 0.213306359445
        gunslinger 0.212293827332
        brewer 0.210974157571
        bellboy 0.210753233953
        snitch 0.197839826622
        centurion 0.196348298146
        solder 0.190903480828
        munchkin 0.185409090909
        feasibility 0.185361008829
        sleepwalker 0.184085044008

The algorithm can classify words from a context, that allows taking into consideration only meaningful words, making a suggestion of possible words for the same or next phrases. Also, it gives a few special options for making advice how to improve a  phrase [advice](HowToCheckTheGrammarStructure.md):

   * Select meaningful words:

        > keywords My cat was drinking a milk.
        milk                 0.088 (0.097-0.008)
        cat                  0.114 (0.117-0.003)
            
        > keywords I had come to his office trying to earn some money .
        office               0.042 (0.042-0.000)
        money                0.056 (0.065-0.009)
             
        > keywords The algorithm can classify words from a context .
        context              0.093 (0.102-0.009)
        algorithm            0.123 (0.123-0.000)
     
   * Suggest words for the same phrase:

        > expand I had come to his office trying to earn some money .
         - mouth, p = 0.0861712001142286
         - man, p = 0.08727164186908883
         - other, p = 0.09262378114664156
         - door, p = 0.10098264193233515
         - vehicle, p = 0.125
         - back, p = 0.1310148362982848
         - head, p = 0.16501827433411617
         - way, p = 0.18085427529524586
         - rush, p = 0.1875
         - cell, p = 0.18952839756592293

        > expand  My cat was drinking a milk.
         - kitchen, p = 0.04123448195846385
         - fridge, p = 0.04609003364659474
         - tea, p = 0.048039215686274506
         - water, p = 0.04878048780487805
         - door, p = 0.05199559113586263
         - time, p = 0.07186125188492506
         - table, p = 0.09414085160691496
         - cup, p = 0.09803921568627451
         - while, p = 0.16863905325443787
         - houses, p = 0.2
         - street, p = 0.2
         - temper, p = 0.2
         
I guess it has to be a a lot of more challenging for German language, though.