The game script for version 0.1 for 85 MB corpora
=====================================================

The first version easily gathers dictionaries from  huge text corpora (I feed it in 85m of novels), generates a phrase from one word, followed by a prompting of a few words for next phrase. The implementation is based on a simple statistical approach without any of substantial ML techniques, but the fact of usage of quite outdated algorithms makes an outcome even more impressive, 

An example of final text:

    I hope you enjoy your trip .
    And still hungry .
    There was no problem , of course , we deserve a more global problem .
    It would have hit the targeted house was a statement .
    Soon it will revolutionize every field .

And how I got it:
  
    > generate hope
    I hope you enjoy your trip .
    
    > I hope you enjoy your trip .
    We suggest a few words for the next phrase:
     - hungry, p = 0.03266713608466161
     
     ... (There were a lot of trash actually)
     
    > generate hungry
    And still hungry .
    
     (a few attempt actually)
     
    > And still hungry .
    We suggest a few words for the next phrase:
     - course, p = 0.015324305712431879
     - tucked, p = 0.019890646915401836
     - he, p = 0.05362317241041657
     
    > generate course 
    There was no problem , of course , we deserve a more global problem .
    
    > There was no problem , of course , we deserve a more global problem .
     - attempted, p = 0.09997779416380262
     - targeted, p = 0.09999748181238999
     - subsidiary, p = 0.09999771073853636
     
    > generate targeted
    It would have hit the targeted house was a statement .
    
    > It would have hit the targeted house was a statement .
    We suggest a few words for the next phrase:
     - Asia, p = 0.24999549931115772
     - revolutionize, p = 0.24999905248655951
  
  Soon it will revolutionize every field .

