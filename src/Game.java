import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Class Game - the main class of the "Zork" game.
 *
 * Author: Michael Kolling Version: 1.1 Date: March 2000
 * 
 * This class is the main class of the "Zork" application. Zork is a very
 * simple, text based adventure game. Users can walk around some scenery. That's
 * all. It should really be extended to make it more interesting!
 * 
 * To play this game, create an instance of this class and call the "play"
 * routine.
 * 
 * This main class creates and initialises all the others: it creates all rooms,
 * creates the parser and starts the game. It also evaluates the commands that
 * the parser returns.
 */
public class Game {
  private Parser parser;
  private Room currentRoom;
  // This is a MASTER object that contains all of the rooms and is easily
  // accessible.
  // The key will be the name of the room -> no spaces (Use all caps and
  // underscore -> Great Room would have a key of GREAT_ROOM
  // In a hashmap keys are case sensitive.
  // masterRoomMap.get("GREAT_ROOM") will return the Room Object that is the Great
  // Room (assuming you have one).
  private HashMap<String, Room> masterRoomMap;
  private static final int MAX_WEIGHT=25; //Maximum weight I have decided
  private ArrayList<String> inventory = new ArrayList<>(); // This is an array list for my inventory
  private HashMap<String, Integer> weight= new HashMap<>(); // Giving Weight to Items
  int score=0; // Bonus Points
  Scanner scan=new Scanner(System.in);
  private void initRooms(String fileName) throws Exception {
    masterRoomMap = new HashMap<String, Room>(); // Here it lists the weights for each item
    Scanner roomScanner;
    weight.put("Batteries",5);
    weight.put("Bottle",2);
    weight.put("Water Bottle",4);
    weight.put("Empty Flashlight",10);
    weight.put("Working Flashlight",15);
    weight.put("Lighter",5);
    weight.put("Mantlepiece",10);
    try {
      HashMap<String, HashMap<String, String>> exits = new HashMap<String, HashMap<String, String>>();
      roomScanner = new Scanner(new File(fileName));
      while (roomScanner.hasNext()) {
        Room room = new Room();
        // Read the Name
        String roomName = roomScanner.nextLine();
        room.setRoomName(roomName.split(":")[1].trim());
        // Read the Description
        if (roomName.split(":")[1].trim().equals("Escape") || (roomName.split(":")[1].trim().equals("Deserted Temple")))
          room.setIsLocked(true);
        // Sets the locked doors
        else
          room.setIsLocked(false);
        String roomDescription = roomScanner.nextLine();
        room.setDescription(roomDescription.split(":")[1].replaceAll("<br>", "\n").trim());
        // Read the Exits
        String roomExits = roomScanner.nextLine();
        // An array of strings in the format E-RoomName
        String[] rooms = roomExits.split(":")[1].split(",");
        HashMap<String, String> temp = new HashMap<String, String>();
        for (String s : rooms) {
          temp.put(s.split("-")[0].trim(), s.split("-")[1]);
        }

        exits.put(roomName.substring(10).trim().toUpperCase().replaceAll(" ", "_"), temp);

        // This puts the room we created (Without the exits in the masterMap)
        masterRoomMap.put(roomName.toUpperCase().substring(10).trim().replaceAll(" ", "_"), room);

        // Now we better set the exits.
      }

      for (String key : masterRoomMap.keySet()) {
        Room roomTemp = masterRoomMap.get(key);
        HashMap<String, String> tempExits = exits.get(key);
        for (String s : tempExits.keySet()) {
          // s = direction
          // value is the room.

          String roomName2 = tempExits.get(s.trim());
          Room exitRoom = masterRoomMap.get(roomName2.toUpperCase().replaceAll(" ", "_"));
          roomTemp.setExit(s.trim().charAt(0), exitRoom);
        }
      }

      roomScanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the game and initialise its internal map.
   */
  public Game() {
    try {
      initRooms("data/rooms.dat");
      currentRoom = masterRoomMap.get("MAIN_AREA");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    parser = new Parser();
  }

  /**
   * Main play routine. Loops until end of play.
   */
  public void play() {
    printWelcome();
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.

    boolean finished = false;
    while (!finished) {
      Command command = parser.getCommand();
      finished = processCommand(command);

    }
    System.out.println("Thank you for playing. Your bonus score is "+score+" Good bye.");
  }

  /**
   * Print out the opening message for the player.
   */
  private void printWelcome() {
    System.out.println();
    System.out.println("Welcome to Reetaban’s Imagination!");
    System.out
        .println("As you can see you are not real. Oops I just broke the 4th wall. Anyway you must escape the game.");
    System.out.println("Good Luck!");
    System.out.println();
    System.out.println(currentRoom.longDescription());
  }

  /**
   * Given a command, process (that is: execute) the command. If this command ends
   * the game, true is returned, otherwise false is returned.
   */
  private boolean processCommand(Command command) {
    if (command.isUnknown()) {
      System.out.println("I don't know what you mean...");
      return false;
    }
    if (currentRoom.getRoomName().equals("Escape")){ // If you reach the last room you quit the game and win
      System.out.println("Thank you for playing. Your bonus score is "+score+" Good bye.");
      System.exit(0);
    }
    String commandWord = command.getCommandWord();
    if (commandWord.equals("help"))
      printHelp();
    else if (commandWord.equals("go"))
      goRoom(command);
    else if (commandWord.equals("look")) { // Searches the map for items
      String item = lookaround();  
      if (item.equals("No items found")){
        System.out.println("No items found");
      }
      else if (!item.equals("Diamond")&& !item.equals("Gold Coin")){ //These items do not get added to inventory, but added to bonus score as they are hard to get
        System.out.println("You find "+item);
        int totalweight=totalMass();
        if (totalweight+weight.get(item)<=MAX_WEIGHT){
        inventory.add(item); //adds if they dont go over the total weight
        }
        else {
          System.out.println("You need to drop an item");
          System.out.println(inventory);
          while(totalMass()+weight.get(item)>MAX_WEIGHT){
          System.out.println("Which item do you want to drop");
          String toDrop= scan.nextLine(); //scans which item u want to drop
          if (inventory.contains(toDrop)){
          inventory.remove(toDrop);
          System.out.println("You removed "+toDrop);
          }
          else{
            System.out.println("Enter the correct item name"); // Catches error in item name
          }
          }
          inventory.add(item);
        }
      }
      else{
      System.out.println("You find "+item); 
      }
    } 
    else if (commandWord.equals("jump")) { //jump command
      System.out.println("Van Halen plays in the background. Might as well jump.");
    }
    else if(commandWord.equals("swim")){
    if (currentRoom.getRoomName().equals("Murky Water")){
      if (command.hasSecondWord()==false){
        System.out.println("Please enter a direction");
      }
      else {
      String second=command.getSecondWord(); //reskinning go command as swim so it makes sense
      if (second.equals("south")){
        goRoom(new Command("swim", "south"));
      }
      if (second.equals("east")){
        goRoom(new Command("swim","east"));  
      }
      if (second.equals("north")){
        goRoom(new Command("swim", "north"));
      }
    }
    }
    if (currentRoom.getRoomName().equals("Rapids")){ //doing it in the rapids as well
    if (command.hasSecondWord()==false){
    System.out.println("Please enter a direction");
    }
    else {
    String second=command.getSecondWord();
    if (second.equals("north")){
      goRoom(new Command("swim", "north"));
    }
    }
  }
  else {
    System.out.println("Why are trying to swim on land"); //makes sure u cant swim on land
  }
}
    else if (commandWord.equals("scream")){
      System.out.println("Stop screaming, you're straining your vocal chords");
    }
    else if (commandWord.equals("yell")){
      System.out.println("Same thing as scream basically");
    }
    else if (commandWord.equals("collect")){
      if (inventory.contains("Bottle")) {
        System.out.println("You collect water with your bottle");
        inventory.add("Water Bottle");
        inventory.remove("Bottle");
      }
      else {
        System.out.println("Where is your bottle");
      }
    }
    else if (commandWord.equals("drink")){
      if (inventory.contains("Water Bottle")){
        System.out.println("Your brain clears up after drinking water. For some reason you want to craft a flashlight"); // gives a hint
        inventory.remove("Water Bottle");
      }
      else {
        System.out.println("Where is your water");
      }
    }
    else if (commandWord.equals("run")){ 
    boolean result=run();
    if(result==true && currentRoom.getRoomName().equals("Deeper Woods")){
      currentRoom=currentRoom.nextRoom("west"); //makes the current room the next room because go room command wasnt working
      System.out.println("You run away from the Wolverine");
      System.out.println(currentRoom.getRoomName());
      System.out.println(""); //This was done because it wasnt reading properly so I had to make it manually
      System.out.println("The trees shroud your vision, as you slowly traverse through the woods. \n To your distance you can hear an animal screeching. Your gut clenches, and your body is screaming to go back");
      System.out.println("E-Deeper Woods, S-Abandoned House");
    }
    else if (result){
      System.out.println("Why are you running");
    }
    else {
      System.out.println("You died to the Wolverine, Thank you for playing. Your bonus score is "+score+" Good Luck Next Time!!!");
      return true;
    }
    }
    else if (commandWord.equals("climb")){ //The other way to escape from the wolverine
    boolean result=climb();
    if(result==true && currentRoom.getRoomName().equals("Deeper Woods")){
      currentRoom=currentRoom.nextRoom("east");
      System.out.println("You have succesfully ran away from the Wolverine");
      System.out.println(currentRoom.getRoomName());
      System.out.println("");
      System.out.println("hoa. I am impressed. Congrats on getting past the wolverine without dying.\n You have reached the only easter egg in the game. Look around for your prize. Also good luck getting back. ");
      System.out.println("W-Deeper Woods");
    }
    else if (result){
      System.out.println("Why are you running");
    }
    else {
      System.out.println("You died to the Wolverine, Thank you for playing. Your bonus score is "+score+" Good Luck Next Time!!!");
      return true;
  }
}
      else if (commandWord.equals("walk")){
      System.out.println("You tread lightly, how cool you must be");
    }
    else if (commandWord.equals("craft")){ //removes the materials to create one item
      String item= craft();
      if (!item.equals("You do not have the required materials to craft")){
        inventory.add(item);
        inventory.remove("Batteries");
        inventory.remove("Empty Flashlight");
        System.out.println("You add the batteries to the flashlight");
        System.out.println(inventory);
      }
      else {
        System.out.println("You do not have the required materials to craft");
      }
    }
    else if (commandWord.equals("quit")) {
      if (command.hasSecondWord())
        System.out.println("Quit what?");
      else
        return true; // signal that we want to quit
    } else if (commandWord.equals("eat")) {
      System.out.println("Do you really think you should be eating at a time like this?");
    }
    return false;
  }

  private String lookaround() { //describes the items in given areas
    if (currentRoom.getRoomName().equals("Main Area")) {
      return "Batteries";
    } else if (currentRoom.getRoomName().equals("Cave")) {
      return "Lighter";
    } else if (currentRoom.getRoomName().equals("King Tomb")) {
      return "Mantlepiece";
    } else if (currentRoom.getRoomName().equals("Treasure Chest")) {
      score+=10;
      return "Diamond";
    } else if (currentRoom.getRoomName().equals("River Bank")) {
      return "Bottle";
    } else if (currentRoom.getRoomName().equals("Hidden Grotto")) {
      score+=5;
      return "Gold Coin";
    } else if (currentRoom.getRoomName().equals("Dead End")) {
      score+=30;
      return "Diamond"; //harder area so higher score
      
    } else if (currentRoom.getRoomName().equals("Basement")) {
      return "Empty Flashlight";
    } else {
      return "No items found";
    }
  }

  private String craft() { // if the inventory has both materials can craft the working flashlight
    if (inventory.contains("Batteries") && inventory.contains("Empty Flashlight")){
      return "Working Flashlight";
    }
    else {
      return "You do not have the required materials to craft";
    }
  }

  private boolean climb(){
    if (currentRoom.getRoomName().equals("Deeper Woods")){
      int chance=(int)(Math.random()*10)+1; // percentage of survival and death while climbing a tree
      if (chance<2){
        return true;
      }
      else{
        return false;
    }
  }
    else{
      return true;
  } 
  }

  private int totalMass(){
    int w=0;
    for (String item:inventory){
      w+=weight.get(item);
    }
    return w;
  }

  



  private boolean run() {
    if (currentRoom.getRoomName().equals("Deeper Woods")){
      int chance=(int)(Math.random()*10)+1; //percentage of survival and death while running
      if (chance<6){
        return true;
      }
      else{
        return false;
    }
  }
    else{
      return true;
  }
}
  // implementations of user commands:
  /**
   * Print out some help information. Here we print some stupid, cryptic message
   * and a list of the command words.
   */
  private void printHelp() {
    System.out.println("Why do you want help");
    System.out.println("Are you so much a simpleton that you cant escape.");
    System.out.println("I am dissapointed");
    System.out.println("Your command words are:");
    parser.showCommands();
  }


  /**
   * Try to go to one direction. If there is an exit, enter the new room,
   * otherwise print an error message.
   */
  private void goRoom(Command command) {
    if (!command.hasSecondWord()) {
      // if there is no second word, we don't know where to go...
      System.out.println("Go where?");
      return;
    }
    String direction = command.getSecondWord();
    if (currentRoom.getRoomName().equals("Deeper Woods")){
    System.out.println("You can not use the go method here, use run or climb"); //forces the player to use run and climb and not allowing bypass using go
    }
    else{
    Room nextRoom = currentRoom.nextRoom(direction);
    if (nextRoom == null)
      System.out.println("There is no door!");
    else if (nextRoom.getIsLocked()){ //locked doors, its unlocked when u have the right items
      if(nextRoom.getRoomName().equals("Deserted Temple")){
        if(!inventory.contains("Working Flashlight")){
        System.out.println("You do not have the items to get to the next area"); 
      }
      else if (inventory.contains("Working Flashlight")){
      nextRoom.setIsLocked(false);
      goRoom(command);
      }
    }
    else if(nextRoom.getRoomName().equals("Escape")){ //locked doors, its unlocked when u have the right items
        System.out.println("Escape");
        if(!inventory.contains("Mantlepiece")){
        System.out.println("You do not have the items to get to the next area");
      }
      else {
        nextRoom.setIsLocked(false);
        goRoom(command);
    }
  }
}
      else if (nextRoom.getRoomName().equals("Treasure Chest")){
        if (!inventory.contains("Lighter")){ 
        int chance=(int)(Math.random()*10)+1; //percentage of survival in the snakepit without a lighter
        if (chance<4){
          goRoom(command);
        }
        else if (chance>4){
          System.out.println("You die to the snakes, You have a bonus score of "+score+" thank you for playing");
          System.exit(0);
        }
        }
        else{ //if u have a lighter u can survive 
          System.out.println("You use the lighter to distract the snakes");
          goRoom(command);
        }
        }
      else if (nextRoom.getRoomName().equals("King Tomb")){
        int chance=(int)(Math.random()*10)+1;
        System.out.println(chance); // Percentage of survival, the go command wasnt working so I had to manually move it
        if (chance>3){
          currentRoom=nextRoom;
          System.out.println("King Tomb");
          System.out.println("");
          System.out.println("The tomb is threatening but awe inspiring at the same time.\n Built in ancient times, there could be the mantlepiece that you are looking for.");
          System.out.println("D-Scorpion Pit");
        }
        else {
          System.out.println("You get bit by a scorpion, You have a bonus score of "+score+" thank you for playing");
          System.exit(0);
        }
        } 
    else {
      currentRoom = nextRoom;
      System.out.println(currentRoom.longDescription());
    }
  }
  }
}

