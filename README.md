Natural Language Processing Tools (NLPT)
==========================================
NLPT is a set of tools intended to learn for a text corpora how to recognize and synthesize a text on a natural language. The final point of development is supposed to be a productive game for a few persons, including an artificial one, that leads to a composing of a text by means of collaborative work of participants. During the game an artificial intelligent leads a team of collaborators, prompting keywords for a possible continuation, evaluating the writings of participants and might even producing its own phrases.
 
The idea of an algorithm are based on statistical analyses of text and simple machine learning techniques.
 
The Game Script
--------------------

  1. Introduction part:
  
    1. Participants choose a topic a few phrases;
  
    2. An AI suggests a few keywords to clarify the topic;
    
    3. Participants vote for the keywords;
    
    4. The AI sets up elected keywords to be used in the text.
    
  2. Main part:
    
    1. The AI offers a few words to write a phrase;
    
    2. Participants are writing a phrase that contains a few of the offered words;
    
    3. The AI evalutes how good the phrases are and exposes an evaluation mark (as a number from 0 to 10);
     
    4. The AI might produce it's own phrase;
    
    5. Participants are voting for the phrases;
    
    6. The elected phrase becomes a continuation of the text above;
    
    7. Game continues from the step 2.1.
    
  3. Additional services:
      
    1. The AI can suggest to change a phrase a bit (add more words, change a word to a synonym or something like that);
       
    2. The AI can sugest synonyms for a word;
       
    3. The AI can feel a gap in a phrase (for example, if a participant have no idea which word to use there);
       
    4. The AI can synthesize a phrase from a set of words;
       
    5. The AI can suggest a few related words.
       
    6. Show pictures that can ilustrate a phrase, a word, or an idea;
    
  4. Social services:
    
    Actualy, some social services have to be provided for the sake of teambuilding, but this idea is out of scope of this very project.
     
  5. Autonomous work:
       
    For the lack of collaborators during a development phase, and especially because necesserity to check a lot of approaches the first few releases are going to be providing only autonomous services, that can resolve all of the challenges the game script needs without arranging them into the script.   
  

The componenents
------------------
Gathering statistic from a text corpora includes tokenizing a text stream, gathering phrases from the stream of tokens, counting simple statistics from the phrases, saving statistic, phrases, and everything in a storage.

Below I provide a simplified description for the algorithms that I suppose to use developing this components.

Tokenizer
~~~~~~~~~~~

Tokenizer splits text to tokens, actually, in some places it can provide a few variants of a token (for example, the dot can be an end of phrase or the end of a shortcut). Thats why tokenizer provides stream of the sets of the tokens for each position in the text.

Phrase buffer
~~~~~~~~~~~~~~
Phrase buffer accumulates stream of the tokens and applies a phrase detector for the current content. Each time the detector assures the buffer contains the whole sentense, the sentece is raised into the output stream wiping of the buffer. 

Phrase detector
~~~~~~~~~~~~~~~
Phrase detector checks for a sequence of token variants if the begining of the sequence contains a whole phrase. The main approach is to use 2- 3- gramms statistic to build the most probable way thru the bufer of tokens looking for more probable one. If it passes thru the dot-as-end-of-phrase (DAEP) it suggests it has found a whole sentence. The main problem there is that actually tokenizer never knows which dot it has, and if the word is a begining of a phrase (escpecially for German language). Also, the text can contain some mistyping. 

Phrase evaluator
~~~~~~~~~~~~~~~~
Phrase evaluator counts the probability of a phrase from the set of tokens it contains.

2 Gramm and 3 gramm counter
~~~~~~~~~~~~~~~~~~~~~~~~~~~~
2-gramm and 3-gramm counter is just a map of n-gramms to the probabilities. Just because of tremendous amount of n-grams, it works in a memory but can incrementally save statistic in a storage.

Storage
~~~~~~~
Storage contains:
  
  1. All of the tokens (as id) and words they reperesent;
   
  2. Probabilities of 2-gramms and 3-gramms;
  
  3. The original phrases;
  
  4. The histogramms of the words (or phrases) for each of the samples (to avoid possible duplication of samples);


Usage
-------
The initial release only contains a few console applications those can gather statistic from a text corpora, save it into the storage and provide simple services from a command prompt. 

To collect statistic,  issue:

  `sbt "run-main com.github.dronegator.nlp.main.NLPTMainStream <FILE WITH A TEXT CORPUS> <FILE OF STORAGE>`
  
  or 

  `sbt "run-main com.github.dronegator.nlp.main.NLPTMain <FILE WITH A TEXT CORPUS> <FILE OF STORAGE>`
  
The applications do the same, except the first one uses AKKA-STREAMS that helps to require less RAM.
   
To use collect statistic, issue:   

  `sbt "run-main com.github.dronegator.nlp.main.NLPTReplMain <FILE OF STORAGE>`
    
  * probability [Word]
  
    Evaluates a probability of a phrase:
     > probability He was reading a book .
     probability = 0.00000010187081
     length = 9
     tokens = 1 :: 1 :: 1494 :: 1047 :: 495 :: 43 :: 670 :: 4 :: 2 :: Nil

  * [Word]+
  
    Suggest a few words to continue the phrase:
    
      > He was reading a
      - magazine (18153), p = 0.1038961038961039
      - book (670), p = 0.16883116883116883
      - paperback (22287), p = 0.16883116883116883

  * [Word]+ .
  
    Suggest a few words for the next phrase:
    
     > He was reading a book .
     - pretending, p = 0.08330280953101102
     - judgment, p = 0.08330766559047138
     - Turkish, p = 0.0833250086599727
     
  * advice [Word] .
     
    Suggest possible substitution of the words in a phrase (the intentional error has provided for the illustration puprpose):
     
      > advice He were reading a book .
      0.3902 They were reading a book .
      0.3532 There were reading a book .
      0.6429 He was reading a book .
      0.0714 He remembered reading a book .
      0.1667 He were in a book .
      0.0549 He were on a book .
      0.0363 He were reading a little .
      0.0140 He were reading a moment .
      0.0102 He were reading a while .
      0.0101 He were reading a lot .

  * generate [Word]+
    Generates a phrase containing the sequence of words:
    
    > generate reading a book
    She was reading a book of famous people .
  
  The storage has to be converted to the dictionary for an android application (but the feature has not implemented yet).      

The outcome
-----------
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

It can suggest different words for a phrase (the phrase contains an intentional error for the sake of illustration):

  > advice The man were trying to make sure .
  
  0.0559 The walls were trying to make sure .
  
  0.0194 The others were trying to make sure .
  
  0.0178 The windows were trying to make sure .
  
  0.0138 The men were trying to make sure .
  
  0.9000 The man was trying to make sure .
  
  0.1000 The man either trying to make sure .
  
  0.2004 The man were going to make sure .
  
  0.0621 The man were supposed to make sure .
  
  0.2315 The man were trying to make it .
  
  0.1034 The man were trying to make sense .
  
  0.0887 The man were trying to make out .

History
---------
[Version 0.0](https://github.com/dronegator/nlp/tree/v.0.0), 20160814, Initial release of nothing.

[Version 0.1](https://github.com/dronegator/nlp/tree/v.0.1), 20160825, Prototype the index tools and functions to cover the simplified script of the game.

[Version 0.2](https://github.com/dronegator/nlp/tree/v.0.2), 20160831, Improve inner architecture and packaging.
