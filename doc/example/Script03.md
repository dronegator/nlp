The game script for version 0.3 and 200 MB corpora
=====================================================
This version cat digest more text data, I've tried up to 700M, but an example above build for 200M only. Using a special approach to pick up meaningful words led to less memory consumption and lack of useless words in suggestions. 


Statistic
-----------
 - ngram1 size = 103401
 - ngram2 size = 2342440
 - ngram3 size = 7640428
 - phrases size = 2359260
 - tokens size = 144527
 - consequent correlation size = 27383
 - inner correlation size = 28567
         


An example of the final text
-----------------------------    

    I hope she had on earlier out of the election .
    And though she sought a better life in the process .
    We would share it .
    Her voice was natural or supernatural disaster .
    I ' ll never find another situation .

And how I got it
-----------------

    > generate hope
    I hope she had on earlier out of the election .
    
    > I hope she had on earlier out of the election .
        - life, p = 0.15232079221437295
        - day, p = 0.16370884586843645
        - time, p = 0.2290106713742067
            
        ....(and no trash any more)
            
    > generate life
    And though she sought a better life in the process .
            
    > And though she sought a better life in the process .
            
        - man, p = 0.11130073087136756
        - time, p = 0.16715590562212587
        - way, p = 0.1866615087675489
        - share, p = 0.5
    
    > generate share
    We would share it .
    
    > We would share it .
        - supernatural, p = 0.06666666666666667
        - door, p = 0.0708554686242313
        - person, p = 0.07408641617566566
        - time, p = 0.08766219131330692
        - way, p = 0.11086802537106795
    
    > generate supernatural
    Her voice was natural or supernatural disaster .
    
    > Her voice was natural or supernatural disaster .
        - situation, p = 0.07837301587301587
        - way, p = 0.09633055116926084
        - room, p = 0.10711676316515027
    
    > generate situation
    I ' ll never find another situation .
    
Additional features
--------------------
Besides suggestion for the next phrase the program now can suggest words to use at the same phrase. It seems a bit useless for now, until we implement an alogrithm to expand the phrase with a specific words, but it seems to be an interesting example, suggest to the same phrase:


    > We would share it expandphrase
      - interest, p = 0.04878048780487805
      - front, p = 0.04878048780487805
      - go, p = 0.051185204480333424
      - cost, p = 0.05314685314685315
      - good, p = 0.05887751217264112
      - past, p = 0.06984478935698449

and suggest to the next phrase:

    > We would share it . continuephrase
      - supernatural, p = 0.06666666666666667
      - door, p = 0.0708554686242313
      - person, p = 0.07408641617566566
      - time, p = 0.08766219131330692
      - way, p = 0.11086802537106795

Obviously an outcome is a bit different, in the first case this is the words that we can use to better detaise the current claim and in the second phrase to advance the store a bit longer.