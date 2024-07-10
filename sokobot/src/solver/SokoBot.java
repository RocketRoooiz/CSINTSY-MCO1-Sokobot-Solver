package solver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

public class SokoBot {
  private HashSet<String> visitedStates = new HashSet<>();
  class sokoState{

    char[][] currentState;
    int[] playerPos;
    char action; // the move done to get to the state
    int heuristic;
    sokoState parent;
    ArrayList<sokoState> childNodes = new ArrayList<>();

    public sokoState(char[][] currentState, int[] playerPos, char action, int heuristic, sokoState parent)
    {
      this.currentState = new char[currentState.length][currentState[0].length];
      this.playerPos = new int[2];
      for (int i = 0; i < currentState.length; i++)
        for (int j = 0; j < currentState[i].length; j++)
          this.currentState[i][j] = currentState[i][j];

      for(int i = 0; i < 2; i++)
        this.playerPos[i] = playerPos[i];

      this.action = action;
      this.heuristic = heuristic;this.parent = parent;
    }

    public sokoState(sokoState parent, char action)
    {
      this.currentState = new char[parent.currentState.length][parent.currentState[0].length];
      this.playerPos = new int[2];

      for (int i = 0; i < parent.currentState.length; i++)
        for (int j = 0; j < parent.currentState[i].length; j++)
          this.currentState[i][j] = parent.currentState[i][j];

      for(int i = 0; i < 2; i++)
        this.playerPos[i] = parent.playerPos[i];

      this.action = action;
      this.parent = parent;
    }

    public int getHeuristic()
    {
      return this.heuristic;
    }

    public void setAction(char action)
    {
      this.action = action;
    }

    public void setChildNodes(ArrayList<sokoState> childNodes)
    {
      this.childNodes = childNodes;
    }
  }


  /**
  *   Generates all possible branches for the state of the Sokoban
  *
  *   @param parent the state of the parent
  *
  *   @return Arraylist of the child nodes (possible branches for the state)
   *
  * */
  private ArrayList<sokoState> createConnections(sokoState parent)
  {
    char[] actions = {'u', 'd', 'l', 'r'};
    ArrayList<sokoState> childNodes = new ArrayList<>();
    ArrayList<sokoState> tempChildNodes = new ArrayList<>();

    // All child node candidates will first start as a copy of the parent

    for(int i = 0; i < 4; i++) // iterate all the actions
    {
      if(isValidMove(actions[i], parent)) // check if the moves is valid
      {
        tempChildNodes.add(new sokoState(parent, actions[i])); // add the valid child nodes to a temp
        tempChildNodes.get(tempChildNodes.size() - 1).setAction(actions[i]); // set the action to the corresponding move done
      }
    }

    for(int i = 0; i < tempChildNodes.size(); i++) // iterate all the temporary children
    {
      tempChildNodes.set(i, updateBoard(parent, tempChildNodes.get(i).action, tempChildNodes.get(i).currentState, tempChildNodes.get(i).playerPos));
      // ^^ updates the sokoState (changes to when the move is executed) of all the temporary children

      // if the state of that temporary child has no duplicate (not in the hash of visited states)
      if(!visitedStates.contains(hashState(tempChildNodes.get(i).currentState)))
      {
        if(!isFailedState(getBoxPos(tempChildNodes.get(i).currentState), tempChildNodes.get(i).currentState)) // check if the state is doable (no stuck box/player)
          childNodes.add(tempChildNodes.get(i)); // add the temp child to the real list

        visitedStates.add(hashState(tempChildNodes.get(i).currentState)); // add the added child to visited states (hash table)
      }
    }

    return childNodes;
  }

  /**
   * @param state current state of the sokoban
   * @return string representation of the 2D array
   */
  private String hashState(char[][] state)
  {
    StringBuilder hash = new StringBuilder();

    for (char[] row : state)
      for (char cell : row)
        hash.append(cell);

    return hash.toString();
  }

  /**
   * Solves the manhattan distance of the boxes and the goals used for the heuristics
   *
   * @param goalPos positions of the goal
   * @param boxPos positions of the box
   *
   * @return the manhattan distance between the boxes and goals
   */
  public int manDist(int[] goalPos, int[] boxPos)
  {
    return Math.abs(goalPos[0] - boxPos[0]) + Math.abs(goalPos[1] - boxPos[1]);
  }

  /**
   * Gets the positions(x, y) of boxes given the current state
   * of the board
   * @param currentState current state of the Sokoban puzzle
   *
   * @return the arraylist of all the positions of the boxes
   */
  public ArrayList<int[]> getBoxPos(char[][] currentState)
  {
    ArrayList<int[]> boxPos = new ArrayList<>(); // box positions

    for(int i = 0; i < currentState.length; i++)
      for(int j = 0; j < currentState[i].length; j++)
      {
        // Get the box positions
        if(currentState[i][j] == '$') {
          boxPos.add(new int[]{i, j});
        }
        // Get the box in goal tile
        if(currentState[i][j] == '*') {
          boxPos.add(new int[]{i, j});
        }
      }

    return boxPos;
  }

  /**
   * Calculates for a heuristic of a given state
   *
   * @param currentState the current state of the Sokoban puzzle
   *
   * @return the heuristic of the state
   */
  public int getHeuristic(char[][] currentState)
  {
    int heuristic = 0;
    int boxNotGoal = 0;
    ArrayList<int[]> boxPos = new ArrayList<>(); // box positions
    ArrayList<int[]> goalPos = new ArrayList<>(); // goal Positions
    int[] playerPos = new int[2];
    int min;

    // Get the goal and box positions for the state that is being evaluated
    for(int i = 0; i < currentState.length; i++)
      for(int j = 0; j < currentState[i].length; j++)
      {
        // Get the box positions
        if(currentState[i][j] == '@' || currentState[i][j] == '+')
        {
          playerPos[0] = i;
          playerPos[1] = j;
        }
        if(currentState[i][j] == '$')
        {
          boxPos.add(new int[]{i, j});
          boxNotGoal++;
        }
        // Get the goal positions
        if(currentState[i][j] == '.' || currentState[i][j] == '+')
          goalPos.add(new int[]{i, j});

        // Get the box pos in goal tile
        if(currentState[i][j] == '*') {
          boxPos.add(new int[]{i, j});
          goalPos.add(new int[]{i, j});
        }
      }

    // Compare the distance of a box to each goal and add to the array the minimum distance got
    for(int i = 0; i < boxPos.size(); i++)
    {
      min = manDist(goalPos.get(0), boxPos.get(i));
      for (int j = 1; j < goalPos.size(); j++)
        if(manDist(goalPos.get(j), boxPos.get(i)) < min)
          min = manDist(goalPos.get(j), boxPos.get(i));
      heuristic += min;
    }

    return heuristic + (boxNotGoal * 2);
  }

  /**
   *  checks if the state cannot be continued anymore (not a solution anymore)
   *
   * @param boxPos positions of the boxes
   * @param currentState current state of the Sokoban puzzle
   *
   * @return if the state is not a solution anymore, true. if the state can still be continues, false.
   */
  public boolean isFailedState(ArrayList<int[]> boxPos, char[][] currentState)
  {
    for (int i = 0; i < boxPos.size(); i++)
    {
      if (currentState[boxPos.get(i)[0]][boxPos.get(i)[1]] == '$' && currentState[boxPos.get(i)[0] + 1][boxPos.get(i)[1]] == '#' && currentState[boxPos.get(i)[0] + 1][boxPos.get(i)[1] - 1] == '#' && currentState[boxPos.get(i)[0]][boxPos.get(i)[1] - 1] == '#')
        return true;
      if (currentState[boxPos.get(i)[0]][boxPos.get(i)[1]] == '$' && currentState[boxPos.get(i)[0] + 1][boxPos.get(i)[1]] == '#' && currentState[boxPos.get(i)[0] + 1][boxPos.get(i)[1] + 1] == '#' && currentState[boxPos.get(i)[0]][boxPos.get(i)[1] + 1] == '#')
        return true;
      if (currentState[boxPos.get(i)[0]][boxPos.get(i)[1]] == '$' && currentState[boxPos.get(i)[0] - 1][boxPos.get(i)[1]] == '#' && currentState[boxPos.get(i)[0] - 1][boxPos.get(i)[1] - 1] == '#' && currentState[boxPos.get(i)[0]][boxPos.get(i)[1] - 1] == '#')
        return true;
      if (currentState[boxPos.get(i)[0]][boxPos.get(i)[1]] == '$' && currentState[boxPos.get(i)[0] - 1][boxPos.get(i)[1]] == '#' && currentState[boxPos.get(i)[0] - 1][boxPos.get(i)[1] + 1] == '#' && currentState[boxPos.get(i)[0]][boxPos.get(i)[1] + 1] == '#')
        return true;
      //adding the other conditions to the loop
      if (isFailedStateIndiv(boxPos.get(i), currentState)){
        return true;
    }
  }
    return false;
  }

  /**
   * Checks each individual box if the boxes they are stuck to are stuck
   *
   * @param boxPos positions of boxes
   * @param currentState current state of the sokoban puzzle
   *
   * @return true if the box is stuck, false if the box is not stuck
   */
  public boolean isFailedStateIndiv(int[] boxPos, char[][] currentState)
  {
      if (currentState[boxPos[0]][boxPos[1]] == '$' && (currentState[boxPos[0] + 1][boxPos[1]] == '$' && isFailedStateIndiv(new int[]{boxPos[0] + 1, boxPos[1]},  currentState))
      && currentState[boxPos[0] + 1][boxPos[1] - 1] == '#' && currentState[boxPos[0]][boxPos[1] - 1] == '#')
        return true;

      if (currentState[boxPos[0]][boxPos[1]] == '$' && currentState[boxPos[0] + 1][boxPos[1]] == '#' && currentState[boxPos[0] + 1][boxPos[1] + 1] == '#' && currentState[boxPos[0]][boxPos[1] + 1] == '$' && isFailedStateIndiv(new int[]{boxPos[0], boxPos[1] + 1},  currentState))
        return true;

      if (currentState[boxPos[0]][boxPos[1]] == '$' && currentState[boxPos[0] + 1][boxPos[1]] == '$' && currentState[boxPos[0] ][boxPos[1] + 1] == '#' && currentState[boxPos[0] + 1][boxPos[1] + 1] == '#')
        return true;

      if (currentState[boxPos[0]][boxPos[1]] == '$' && currentState[boxPos[0] - 1][boxPos[1]] == '#' && currentState[boxPos[0] - 1][boxPos[1] + 1] == '#' && currentState[boxPos[0]][boxPos[1] + 1] == '$' && isFailedStateIndiv(new int[]{boxPos[0], boxPos[1] + 1},  currentState))
        return true;

      return false;
  }

  /**
   * Checks if a move is valid
   *
   * @param move the move to be checked
   * @param currentSokoState the current state of the Sokoban puzzle
   *
   * @return if the move is valid, true. if the move is not valid, false.
   */
  public boolean isValidMove(char move, sokoState currentSokoState)
  {
    boolean isValid = true; // if clear (empty space OR box can be pushed OR no walls in the way)
    char[][] currentState = currentSokoState.currentState;
    int[] playerPos = currentSokoState.playerPos;

    if (move == 'u') {
      if (currentState[playerPos[0] - 1][playerPos[1]] == '#') // check wall
        isValid = false;
      else if (currentState[playerPos[0] - 1][playerPos[1]] == '$' || currentState[playerPos[0] - 1][playerPos[1]] == '*') // check box/box in goal
        if (currentState[playerPos[0] - 2][playerPos[1]] == '#' || currentState[playerPos[0] - 2][playerPos[1]] == '$' || currentState[playerPos[0] - 2][playerPos[1]] == '*')
          isValid = false; // ^^ if box meets a wall/another box in push
    }
    else if (move == 'd') {
      if (currentState[playerPos[0] + 1][playerPos[1]] == '#') // check wall
        isValid = false;
      else if (currentState[playerPos[0] + 1][playerPos[1]] == '$' || currentState[playerPos[0] + 1][playerPos[1]] == '*') // check box/box in goal
        if (currentState[playerPos[0] + 2][playerPos[1]] == '#' || currentState[playerPos[0] + 2][playerPos[1]] == '$' || currentState[playerPos[0] + 2][playerPos[1]] == '*')
          isValid = false; // ^^ if box meets a wall/another box in push
    }
    else if (move == 'l') {
      if (currentState[playerPos[0]][playerPos[1] - 1] == '#') // check wall
        isValid = false;
      else if (currentState[playerPos[0]][playerPos[1] - 1] == '$' || currentState[playerPos[0]][playerPos[1] - 1] == '*') // check box/box in goal
        if (currentState[playerPos[0]][playerPos[1] - 2] == '#' || currentState[playerPos[0]][playerPos[1] - 2] == '$' || currentState[playerPos[0]][playerPos[1] - 2] == '*')
          isValid = false; // ^^ if box meets a wall/another box in push
    }
    else if (move == 'r') {
      if (currentState[playerPos[0]][playerPos[1] + 1] == '#') // check wall
        isValid = false;
      else if (currentState[playerPos[0]][playerPos[1] + 1] == '$' || currentState[playerPos[0]][playerPos[1] + 1] == '*') // check box/box in goal
        if (currentState[playerPos[0]][playerPos[1] + 2] == '#' || currentState[playerPos[0]][playerPos[1] + 2] == '$' || currentState[playerPos[0]][playerPos[1] + 2] == '*')
          isValid = false; // ^^ if box meets a wall/another box in push
    }
    return isValid;
  }


  /**
   * Updates a board state given a move
   *
   * @param move  the move taken
   * @param currentState  the current state of the sokoban puzzle
   * @param playerPos the current position of the player
   *
   * @return the new state of the sokoban puzzle after the update
   */
  public sokoState updateBoard(sokoState parent, char move, char[][] currentState, int[] playerPos)
  {
    int[] newPlayerPos = new int[2];
    switch (move) {
      case 'u' -> {
        newPlayerPos[0] = playerPos[0] - 1;
        newPlayerPos[1] = playerPos[1];
        if (currentState[playerPos[0] - 1][playerPos[1]] == '$' || currentState[playerPos[0] - 1][playerPos[1]] == '*')
        {  // If there is a box above the player, move it up first
          if (currentState[playerPos[0] - 2][playerPos[1]] == '.') // If there is a goal above box, set it to box in goal
            currentState[playerPos[0] - 2][playerPos[1]] = '*';
          else
            currentState[playerPos[0] - 2][playerPos[1]] = '$';
        }

        // when player steps on a goal
        if(currentState[playerPos[0] - 1][playerPos[1]] == '*' || currentState[playerPos[0] - 1][playerPos[1]] == '.')
          currentState[playerPos[0] - 1][playerPos[1]] = '+';
        else // when player steps on an empty space
          currentState[playerPos[0] - 1][playerPos[1]] = '@'; //adds a new '@' above player

        if(currentState[playerPos[0]][playerPos[1]] == '+') // if player is stepping on a box
          currentState[playerPos[0]][playerPos[1]] = '.'; // return to goal symbol
        else
          currentState[playerPos[0]][playerPos[1]] = ' '; // deletes the old '@'
      }
      case 'd' -> {
        newPlayerPos[0] = playerPos[0] + 1;
        newPlayerPos[1] = playerPos[1];
        if (currentState[playerPos[0] + 1][playerPos[1]] == '$' || currentState[playerPos[0] + 1][playerPos[1]] == '*')
        {
          if (currentState[playerPos[0] + 2][playerPos[1]] == '.')
            currentState[playerPos[0] + 2][playerPos[1]] = '*';
          else
            currentState[playerPos[0] + 2][playerPos[1]] = '$';
        }

        if(currentState[playerPos[0] + 1][playerPos[1]] == '*' || currentState[playerPos[0] + 1][playerPos[1]] == '.')
          currentState[playerPos[0] + 1][playerPos[1]] = '+';
        else // when player steps on an empty space
          currentState[playerPos[0] + 1][playerPos[1]] = '@'; //adds a new '@' below player

        if(currentState[playerPos[0]][playerPos[1]] == '+')
          currentState[playerPos[0]][playerPos[1]] = '.';
        else
          currentState[playerPos[0]][playerPos[1]] = ' '; // deletes the old '@'
      }
      case 'l' -> {
        newPlayerPos[0] = playerPos[0];
        newPlayerPos[1] = playerPos[1] - 1;
        if (currentState[playerPos[0]][playerPos[1] - 1] == '$' || currentState[playerPos[0]][playerPos[1] - 1] == '*')
        {
          if (currentState[playerPos[0]][playerPos[1] - 2] == '.')
            currentState[playerPos[0]][playerPos[1] - 2] = '*';
          else
            currentState[playerPos[0]][playerPos[1] - 2] = '$';
        }

        if(currentState[playerPos[0]][playerPos[1] - 1] == '*' || currentState[playerPos[0]][playerPos[1] - 1] == '.')
          currentState[playerPos[0]][playerPos[1] - 1] = '+';
        else // when player steps on an empty space
          currentState[playerPos[0]][playerPos[1] - 1] = '@'; //adds a new '@' below player

        if(currentState[playerPos[0]][playerPos[1]] == '+')
          currentState[playerPos[0]][playerPos[1]] = '.';
        else
          currentState[playerPos[0]][playerPos[1]] = ' '; // deletes the old '@'
      }
      case 'r' -> {
        newPlayerPos[0] = playerPos[0];
        newPlayerPos[1] = playerPos[1] + 1;
        if (currentState[playerPos[0]][playerPos[1] + 1] == '$' || currentState[playerPos[0]][playerPos[1] + 1] == '*')
        {
          if (currentState[playerPos[0]][playerPos[1] + 2] == '.')
            currentState[playerPos[0]][playerPos[1] + 2] = '*';
          else
            currentState[playerPos[0]][playerPos[1] + 2] = '$';
        }

        if(currentState[playerPos[0]][playerPos[1] + 1] == '*' || currentState[playerPos[0]][playerPos[1] + 1] == '.')
          currentState[playerPos[0]][playerPos[1] + 1] = '+';
        else // when player steps on an empty space
          currentState[playerPos[0]][playerPos[1] + 1] = '@'; //adds a new '@' above player

        if(currentState[playerPos[0]][playerPos[1]] == '+')
          currentState[playerPos[0]][playerPos[1]] = '.';
        else
          currentState[playerPos[0]][playerPos[1]] = ' '; // deletes the old '@'
      }
     }
    return new sokoState(currentState, newPlayerPos, move, getHeuristic(currentState), parent);
  }

  /**
   * Solves the sokoban puzzle
   *
   * @param width width of the puzzle
   * @param height  height of the puzzle
   * @param mapData the positions of the walls, empty spaces, and goals
   * @param itemsData the positions of the boxes and player
   *
   * @return the string of moves to be used to solve the puzzle
   */
  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData)
  {
    // save all possible data needed by the states into different arrays
    ArrayList<sokoState> visitedQ = new ArrayList<>();
    int[] playerPos = new int[2];
    char[][] copyPanel = new char[mapData.length][mapData[0].length];
    String moves = "";
    int i, j, k;
    boolean found = false;
    Comparator<sokoState> heuristicCheck = Comparator.comparingInt(sokoState::getHeuristic);
    PriorityQueue<sokoState> openNodes = new PriorityQueue<>(heuristicCheck); // keeps track of nodes yet to be visited

    // saves map into 2D array
    for(i = 0; i < itemsData.length; i++)
      for(j = 0; j < itemsData[i].length; j++)
      {
        copyPanel[i][j] = ' ';
        // get the player position
        if(itemsData[i][j] == '@')
        {
          playerPos[0] = i;
          playerPos[1] = j;
          copyPanel[i][j] = '@';
        }
        // get the box positions
        if(itemsData[i][j] == '$')
          copyPanel[i][j] = '$';
        // get the goal positions
        if(mapData[i][j] == '.')
          copyPanel[i][j] = '.';
        // get the wall positions
        if(mapData[i][j] == '#')
          copyPanel[i][j] = '#';
        // gets the player position in a goal
        if(itemsData[i][j] == '@' && mapData[i][j] == '.')
          copyPanel[i][j] = '+';
        // box is in goal tile
        if(mapData[i][j] == '.' && itemsData[i][j] == '$')
          copyPanel[i][j] = '*';
      }

    sokoState initialState = new sokoState(copyPanel, playerPos, ' ', getHeuristic(copyPanel), null);

    visitedQ.add(initialState); // put the initial state in the queue
    visitedStates.add(hashState(initialState.currentState)); // add the state in the hash table for duplicate checking

    for(k = 0; !found; k++)
    {
      ArrayList<sokoState> temp = createConnections(visitedQ.get(k)); // gets the children of the current node

      for(i = 0; i < temp.size(); i++) {
        openNodes.offer(temp.get(i));
      }

      if(visitedQ.get(k).heuristic == 0)
        found = true;

      visitedQ.get(k).setChildNodes(temp);
      visitedQ.add(openNodes.poll());
    }

    sokoState temp = visitedQ.get(k-1);
    moves = temp.action + moves;

    while(temp.parent.action == 'd' || temp.parent.action == 'u' ||
            temp.parent.action == 'l' || temp.parent.action == 'r')
    {
      moves = temp.parent.action + moves;
      temp = temp.parent;
    }

    //System.out.println("MOVES: " + moves.length());
    try {
          //Thread.sleep(3000);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
    return moves; // plays the solution in the bot
  }
}
