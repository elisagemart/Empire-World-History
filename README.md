# Empire-World-History
A Java applet that allows users to play Empire World History against computer opponents.

## What It Does
Empire World History is a turn-based card game designed to be fun to play and help students review for the AP world history exam. The Empire World History Applet is a client which allows users to play the game against two AI opponents.

![GUI](https://user-images.githubusercontent.com/10715620/104393689-aa4ae100-550a-11eb-8cc4-edd68b3a8f78.PNG)

## Code Structure
The GameBoard class contains the logic that handles player turns and game actions. The GUI class contains the code that generates the menu and visual gameboard which is presented to the user. The logic used by AI players to make decisions can also be found in the GameBoard class.

## AI Behavior
Because of the highly stochastic nature of the game (including random draws from a pool of over 300 unique cards) it is currently intractable to produce a backwards induction agent for Empire World History. Instead, the most advanced level of AI opponent performs a minimax search to the end of the current turn, then evaluates the possible positions using a hand-tuned heuristic which takes into account the score and board positions of itself and both opponents. This method produces opponents who are slightly weaker than strong human players but perform extremely well against inexperienced opponents.

## Website
The Empire World History site, with additional information and an applet download, can be found here: https://elisagemart.wixsite.com/empireworldhistory
