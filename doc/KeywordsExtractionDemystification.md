Keywords Extraction Demystification
====================================
A substantial issue of natural language which leads to the most challenges for statistic based approaches   of text generation is that on the one hand everything is possible in a large text corpora and on another hand the most frequent cases bring only a least piece of information in the text.

We usually engage with entropy based metrics, as TD-IDF, that supposed to address exactly that statistical problem, but since everything is still possible…they enrich the trashed outcome a lot leaving the whole issue mainly intact: the results are still dirty.

That's why another approach does exist: what if some algorithm will provide a direct classification of words in a few different categories, as service words and meaningful words. They usually suppose that nouns are more useful than pronouns,  modal verbs and even verbs themselves. The others point out the fact, that for some languages (especially germanic ones) verbs even more important for getting the meaning of the grammar structure. But whatsoever it is just a classification of words in the context into two quite intuitive categories, the goal that neural networks are pretty good to deal with.

My idea (I bet I am not the first, at least I can mention McCollins,  whom lectionaries I was happy to listen to) was to write a simple neural based (wow) classifier of words in triplets. I thought that having a sample of a few dozens meaningful and service words I might have write a classifier that can give an enriched sample of a few hundred that I can rectify just by hands and eyes. Honestly it was an act of despair.

My materialistic frame of mind keeps me considering a noun to be an objective and verbs with others just behavioral preferences (it was a joke) so commuting at home I wrote down a few dozens of nouns for the basic human needs (except sex and violence) and a few dozens of service words that anyone can get from the first chapter of grammar reference.

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


I trained a lazily designed neural network of 2 (two) neurons to make a mapping of the vector of the context (each dimension means one of a million possible pairs of word before and after checked one to  a choice one of two categories: service vs meaningful words. I was going to apply a sophisticated learning technique but surprisingly even the simple one gave a tremendous outcome, most words was correctly classified despite the fact they were not presented in the training sample. Anyway I ve extended the sample up to 300 words that gives an algorithm that can outline the keywords in a phrase.

A few examples are followed.

A few examples followed:

    > keywords My cat was drinking a milk.
    a                    -1.000 (0.000-1.000)
    drinking             -0.275 (0.000-0.276)
    was                  0.000 (0.000-0.000)
    milk                 0.088 (0.097-0.008)
    cat                  0.114 (0.117-0.003)
            
I guess it is gonna be a bit challenging for the German though ^^.   
