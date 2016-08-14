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
The initial release only contains a console application that can gather statistic from a text corpora, save it into the storage and provide simple services from a command prompt. To start the application issue:

  `java -jar nlp-tools.jar [gather|prompt] <Directory> [<List of files>]`, where:

  * "gather" gathers data from the files, incrementally saving in a storage, hosting in the Directory, at the end in shows statistic;

  * "promt" shows a prompt with a few possiible commands:
  
    * "fill" [<word> | "*" *] fills the gap denoted with "*"
      
    * "arrange" [<word> *] builds a phrase from the words.
      
The storage can be converted to the dictionary for an android application.      

The outcome
-----------
No outcomes yet

History
---------
[Version 0.0](https://github.com/dronegator/nlp/tree/v.0.0),, 20160814, Initial release of nothing.
