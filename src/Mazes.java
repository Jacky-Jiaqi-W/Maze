import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import tester.*;
import javalib.impworld.*;
import javalib.worldimages.LineImage;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.WorldImage;

// to represent a cell
class Cell {
  //In logical coordinates, with the origin at the top-left corner of the screen
  int row;
  int col;
  boolean colorExploration = false;
  boolean colorSolution = false;
  ArrayList<Edge> edges;

  Cell(int row, int col, ArrayList<Edge> edges) {
    this.row = row;
    this.col = col;
    this.edges = edges;
  }

  // EFFECT: to add the given edge to this cell's edges
  void addToEdges(Edge edge) {
    this.edges.add(edge);
  }

  // to check if this cell is in the same line with the given cell
  boolean sameRow(Cell that) {
    return this.row == that.row;
  }

  // EFFECT: update the colorExploration to be true
  void updateColorExploration() {
    this.colorExploration = true;
  }

  // EFFECT: update the colorSolution to be true
  void updateColorSolution() {
    this.colorSolution = true;
  }
}

// to represent an edge between the cells
class Edge implements Comparable<Edge> {
  Cell first;
  Cell second;
  int weight;
  boolean valid;

  Edge(Cell first, Cell second, int weight, boolean valid) {
    this.first = first;
    this.second = second;
    this.weight = weight;
    this.valid = valid;
  }

  // to compare the edges by their weights
  public int compareTo(Edge that) {
    return this.weight - that.weight;
  }

  // to return the first cell of this edge
  Cell getFirst() {
    return this.first;
  }

  // to return the second cell of this edge
  Cell getSecond() {
    return this.second;
  }

  // to determine if this edge is vertical
  boolean vertical() {
    return this.first.sameRow(this.second);
  }

  // to draw the edge
  WorldImage draw() {
    WorldImage image;
    if (this.vertical()) {
      image = new LineImage(new Posn(0,20), Color.DARK_GRAY);
    } 
    else {
      image = new LineImage(new Posn(20,0), Color.DARK_GRAY);
    }
    return image;
  }
}

// to represent the Mazes World
class MazesWorld extends World {
  int col;
  int row;
  ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();
  ArrayList<Edge> worklist = new ArrayList<Edge>();
  Random rand = new Random();
  HashMap<Cell, Cell> representatives = new HashMap<Cell, Cell>();
  ArrayList<Edge> minimalPath = new ArrayList<Edge>();
  int count = -1;
  ArrayList<Cell> alreadySeen = new ArrayList<Cell>();
  ArrayList<Cell> worklistSearch = new ArrayList<Cell>();
  ArrayList<Cell> path = new ArrayList<Cell>();
  HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();

  MazesWorld(int col, int row) {
    this.col = col;
    this.row = row;
    this.board = this.createBoard();
    this.createEdges();
    this.initHashMap();
    Collections.sort(this.worklist);
    this.findMinimalPath();
    this.containOnlyWalls();
  }

  MazesWorld(int col, int row, int seed) {
    this.col = col;
    this.row = row;
    this.rand = new Random(seed);
    this.board = this.createBoard();
  }

  // to create the two-dimensional grid using the given number of rows and columns
  // where there are no edges betwee the cells
  ArrayList<ArrayList<Cell>> createBoard() {
    ArrayList<Cell> rowBoard; // to store the each created row
    ArrayList<ArrayList<Cell>> board = new ArrayList<ArrayList<Cell>>();

    for (int i = 0; i < this.row; i++) {
      rowBoard = new ArrayList<Cell>();
      for (int j = 0; j < this.col; j++) {
        rowBoard.add(new Cell(i, j, new ArrayList<Edge>()));
      }
      board.add(rowBoard);
    }
    return board;
  }

  // EFFECT: to create the edges between the cells inside the board, add the edges to worklist 
  // and add the edges to related cells
  void createEdges() {
    Edge edge;
    for (int i = 0; i < this.row - 1; i++) {
      for (int j = 0; j < this.col; j++) {
        edge = new Edge(this.board.get(i).get(j), 
            this.board.get(i + 1).get(j), this.rand.nextInt(100), false);
        this.worklist.add(edge);
        this.board.get(i).get(j).addToEdges(edge);
        this.board.get(i + 1).get(j).addToEdges(edge);
      }
    }

    for (int i = 0; i < this.row; i++) {
      for (int j = 0; j < this.col - 1; j++) {
        edge = new Edge(this.board.get(i).get(j), 
            this.board.get(i).get(j + 1), this.rand.nextInt(100), false);
        this.worklist.add(edge);
        this.board.get(i).get(j).addToEdges(edge);
        this.board.get(i).get(j + 1).addToEdges(edge);
      }
    } 
  }

  // EFFECT: initialize every cell's representative to itself
  void initHashMap() {
    for (ArrayList<Cell> row : this.board) {
      for (Cell cell : row) {
        this.representatives.put(cell, cell);
      }
    }
  }

  // to find the representative of the given cell in the HashMap
  Cell find(Cell c) {
    if (this.representatives.get(c).equals(c)) {
      return c;
    }
    else {
      return this.find(this.representatives.get(c));
    }
  }

  // to check if the HashMap has more than one representative
  boolean moreThanOneRepre() {
    Cell representative = this.find(this.board.get(0).get(0));
    boolean result = true;
    for (ArrayList<Cell> row : this.board) {
      for (Cell cell : row) {
        result = result && representative.equals(this.find(cell));
      }
    }
    return !result;
  }

  // EFFECT: to connect one representative to the other representative
  void union(Cell repre1, Cell repre2) {
    this.representatives.put(repre2, this.representatives.get(repre1));
  }

  // EFFECT: to find the minimal path of all the cells in the board
  void findMinimalPath() {
    int idx = 0;
    while (this.moreThanOneRepre()) {
      Edge edge = this.worklist.get(idx);
      Cell repre1 = this.find(edge.getFirst());
      Cell repre2 = this.find(edge.getSecond());
      if (!repre1.equals(repre2)) {
        this.minimalPath.add(edge);
        this.union(repre1, repre2);
      }
      idx++;
    }
  }

  // EFFECT: to remove the edges inside the minimalPath from worklist to only keep the walls
  void containOnlyWalls() {
    for (Edge e : this.minimalPath) {
      this.worklist.remove(e);
    }
  }

  // EFFECT: handle the key event
  public void onKeyEvent(String key) {
    if (key.equals("b")) {
      this.count = 0;
      this.search(key);
    }
    else if (key.equals("d")) {
      this.count = 0;
      this.search(key);
    }
    else if (key.equals("n")) {
      this.board = this.createBoard();
      this.createEdges();
      this.initHashMap();
      Collections.sort(this.worklist);
      this.findMinimalPath();
      this.containOnlyWalls();
    }
  }

  // EFFECT: to do the dfs search or bfs search
  void search(String key) {
    Cell from = this.board.get(0).get(0);
    Cell to = this.board.get(this.row - 1).get(this.col - 1);
    this.worklistSearch.add(from);

    // As long as the worklist isn't empty...
    while (this.worklistSearch.size() > 0) {
      Cell next = this.worklistSearch.remove(0);
      if (next.equals(to)) {
        this.reconstruct(this.cameFromEdge, to);
        return ; // Success!
      }
      else if (this.alreadySeen.contains(next)) {
        // do nothing: we've already seen this one
      }
      else {
        // add all the neighbors of next to the worklist for further processing
        for (Edge e : next.edges) {
          if (e.first.equals(next) && this.minimalPath.contains(e)) {
            if (key.equals("b")) {
              this.worklistSearch.add(e.second);
            }
            else if (key.equals("d")) {
              this.worklistSearch.add(0, e.second);
            }
            this.cameFromEdge.put(e.second, next);
          }
          else if (e.second.equals(next) && this.minimalPath.contains(e)) {
            if (key.equals("b")) {
              this.worklistSearch.add(e.first);
            }
            else if (key.equals("d")) {
              this.worklistSearch.add(0, e.first);
            }
            this.cameFromEdge.put(e.first, next);
          }
        }
        // add next to alreadySeen, since we're done with it
        this.alreadySeen.add(next);
      }
    }
  }

  // EFFECT: to reconstruct the path from the end to the beginning
  void reconstruct(HashMap<Cell, Cell> cameFromEdge, Cell end) {
    this.path.add(this.board.get(this.row - 1).get(this.col - 1));
    Cell begin = this.board.get(0).get(0);

    while (begin != end) {
      this.path.add(cameFromEdge.get(end));
      end = cameFromEdge.get(end);
    }
  }

  // EFFECT: to handle the tick of the world 
  public void onTick() {
    if (this.count > -1) {
      this.count = this.count + 1;

      if (this.count < this.alreadySeen.size()) {
        this.alreadySeen.get(this.count).updateColorExploration();
      }
      else if (this.count >= this.alreadySeen.size() 
          && this.count - this.alreadySeen.size() < this.path.size()) {
        this.path.get(this.count - this.alreadySeen.size()).updateColorSolution();
      }
    }
  }

  // to make the scene of this world
  public WorldScene makeScene() {
    WorldScene background = new WorldScene(this.col * 20, this.row * 20);
    background.placeImageXY(
        new RectangleImage(this.col * 20, this.row * 20, OutlineMode.SOLID, Color.GRAY),
        this.col * 10, this.row * 10);
    background.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.GREEN), 10, 10);
    background.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.RED), 
        this.col * 20 - 10, this.row * 20 - 10);
    for (ArrayList<Cell> row : this.board) {
      for (Cell cell : row) {
        if (cell.colorExploration && cell.colorSolution) {
          background.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.RED), 
              cell.col * 20 + 10, cell.row * 20 + 10);
        }
        else if (cell.colorExploration && cell.colorSolution) {
          background.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.GREEN), 
              cell.col * 20 + 10, cell.row * 20 + 10);
        }
      }
    }

    for (Edge e : this.worklist) {
      if (e.vertical() ) {
        background.placeImageXY(e.draw(), e.second.col * 20, e.second.row * 20 + 10);
      }
      else {
        background.placeImageXY(e.draw(), e.second.col * 20 + 10, e.second.row * 20);
      }
    }
    return background;
  }
}

// to represent examples and tests of Mazes
class ExamplesMazes {
  MazesWorld world1;
  MazesWorld world2;
  Cell cell100;
  Cell cell101;
  Cell cell102;
  Cell cell110;
  Cell cell111;
  Cell cell112;
  ArrayList<ArrayList<Cell>> board1;
  Edge edge0001;
  Edge edge0010;
  Edge edge0210;
  HashMap<Cell, Cell> hashMap1;
  HashMap<Cell, Cell> hashMap2;

  // to initialize the examples
  void initMazes() {
    this.world1 = new MazesWorld(3, 2, 5);
    this.world2 = new MazesWorld(3, 3, 5);
    this.cell100 = new Cell(0, 0, new ArrayList<Edge>());
    this.cell101 = new Cell(0, 1, new ArrayList<Edge>());
    this.cell102 = new Cell(0, 2, new ArrayList<Edge>());
    this.cell110 = new Cell(1, 0, new ArrayList<Edge>());
    this.cell111 = new Cell(1, 1, new ArrayList<Edge>());
    this.cell112 = new Cell(1, 2, new ArrayList<Edge>());
    this.board1 = new ArrayList<ArrayList<Cell>>(Arrays.asList(
        new ArrayList<Cell>(Arrays.asList(this.cell100, this.cell101, this.cell102)), 
        new ArrayList<Cell>(Arrays.asList(this.cell110, this.cell111, this.cell112))));
    this.edge0001 = new Edge(this.cell100, this.cell101, 20, false);
    this.edge0010 = new Edge(this.cell100, this.cell110, 20, false);
    this.edge0210 = new Edge(this.cell102, this.cell110, 30, false);
    this.hashMap1 = new HashMap<Cell, Cell>();
    this.hashMap1.put(this.cell100, this.cell102);
    this.hashMap1.put(this.cell102, this.cell102);
    this.hashMap1.put(this.cell110, this.cell110);
  }

  // test the method createBoard in MazesWorld class
  void testCreateBoard(Tester t) {
    this.initMazes();
    t.checkExpect(this.world1.createBoard(), this.board1);
  }

  // test the method addToEdges in Cell class
  void testAddToEdges(Tester t) {
    this.initMazes();
    t.checkExpect(this.cell100.edges, new ArrayList<Edge>());
    this.cell100.addToEdges(this.edge0001);
    t.checkExpect(this.cell100.edges, new ArrayList<Edge>(Arrays.asList(this.edge0001)));
  }

  // test the method createEdges in MazesWorld class
  void testCreateEdges(Tester t) {
    this.initMazes();
    t.checkExpect(this.world1.worklist, new ArrayList<Edge>());
    t.checkExpect(this.world1.board.get(0).get(0).edges, new ArrayList<Edge>());
    this.world1.createEdges();
    t.checkExpect(this.world1.worklist.get(0), 
        new Edge(this.world1.board.get(0).get(0), this.world1.board.get(1).get(0), 87, false));
    t.checkExpect(this.world1.board.get(0).get(0).edges,
        new ArrayList<Edge>(Arrays.asList(
            new Edge(this.world1.board.get(0).get(0), 
                this.world1.board.get(1).get(0), 87, false), 
            new Edge(this.world1.board.get(0).get(0), 
                this.world1.board.get(0).get(1), 24, false))));
  }

  // test the method initHashMap in MazesWorld class
  void testInitHashMap(Tester t) {
    this.initMazes();
    t.checkExpect(this.world1.representatives.get(this.world1.board.get(0).get(0)), null);
    this.world1.initHashMap();
    t.checkExpect(this.world1.representatives.get(this.world1.board.get(0).get(0)), 
        this.world1.board.get(0).get(0));
  }

  // test the method find in MazesWorld class
  void testFind(Tester t) {
    this.initMazes();
    this.world1.representatives = this.hashMap1;
    t.checkExpect(this.world1.find(this.cell100), this.cell102);
    t.checkExpect(this.world1.find(this.cell110), this.cell110);
  }

  // test the method moreThanOneRepre in MazesWorld class
  void testMoreThanOneRepre(Tester t) {
    this.initMazes();
    this.world1.initHashMap();
    this.world2.representatives.put(this.world2.board.get(0).get(0), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(0).get(1), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(0).get(2), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(1).get(0), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(1).get(1), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(1).get(2), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(2).get(0), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(2).get(1), 
        this.world2.board.get(0).get(0));
    this.world2.representatives.put(this.world2.board.get(2).get(2), 
        this.world2.board.get(0).get(0));
    t.checkExpect(this.world1.moreThanOneRepre(), true);
    t.checkExpect(this.world2.moreThanOneRepre(), false);
  }

  // test the method union in MazesWorld class
  void testUnion(Tester t) {
    this.initMazes();
    this.world1.initHashMap();
    t.checkExpect(this.world1.representatives.get(this.world1.board.get(0).get(0)), 
        this.world1.board.get(0).get(0));
    t.checkExpect(this.world1.representatives.get(this.world1.board.get(0).get(1)), 
        this.world1.board.get(0).get(1));
    this.world1.union(this.world1.board.get(0).get(0), this.world1.board.get(0).get(1));
    t.checkExpect(this.world1.representatives.get(this.world1.board.get(0).get(0)), 
        this.world1.board.get(0).get(0));
    t.checkExpect(this.world1.representatives.get(this.world1.board.get(0).get(1)), 
        this.world1.board.get(0).get(0));
  }

  // test the method getFirst in Edge class
  void testGetFirst(Tester t) {
    this.initMazes();
    t.checkExpect(this.edge0001.getFirst(), this.cell100);
    t.checkExpect(this.edge0210.getFirst(), this.cell102);
  }

  // test the method getSecond in Edge class
  void testGetSecond(Tester t) {
    this.initMazes();
    t.checkExpect(this.edge0001.getSecond(), this.cell101);
    t.checkExpect(this.edge0210.getSecond(), this.cell110);
  }

  // test the method findMinimalPath in MazesWorld class
  void testFindMinimalPath(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    t.checkExpect(this.world1.minimalPath.size(), 0);
    this.world1.findMinimalPath();
    t.checkExpect(this.world1.minimalPath.size(), 5);
  }

  // test the method containOnlyWalls in MazesWorld class
  void testContainOnlyWalls(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    this.world1.findMinimalPath();
    t.checkExpect(this.world1.worklist.size(), 7);
    this.world1.containOnlyWalls();
    t.checkExpect(this.world1.worklist.size(), 2);
  }

  // test the method sameRow in Cell class
  void testSameRow(Tester t) {
    this.initMazes();
    t.checkExpect(this.cell100.sameRow(this.cell101), true);
    t.checkExpect(this.cell100.sameRow(this.cell110), false);
  }

  // test the method vertical in Edge class
  void testVertical(Tester t) {
    this.initMazes();
    t.checkExpect(this.edge0001.vertical(), true);
    t.checkExpect(this.edge0010.vertical(), false);
  }

  // test the method draw in Edge class
  void testDraw(Tester t) {
    this.initMazes();
    t.checkExpect(this.edge0001.draw(), new LineImage(new Posn(0, 20), Color.DARK_GRAY));
    t.checkExpect(this.edge0010.draw(), new LineImage(new Posn(20, 0), Color.DARK_GRAY));
  }

  // test the method makeScene in MazesWorld class
  void testMakeScene(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    this.world1.findMinimalPath();
    this.world1.containOnlyWalls();
    WorldScene background = new WorldScene(60, 40);
    background.placeImageXY(new RectangleImage(60, 40, OutlineMode.SOLID, Color.GRAY), 30, 20);
    background.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.GREEN), 10, 10);
    background.placeImageXY(new RectangleImage(20, 20, OutlineMode.SOLID, Color.RED), 50, 30);
    background.placeImageXY(new LineImage(new Posn(20, 0), Color.DARK_GRAY), 10, 20);
    background.placeImageXY(new LineImage(new Posn(20, 0), Color.DARK_GRAY), 30, 20);
    t.checkExpect(this.world1.makeScene(), background);
  }

  // test the method reconstruct in MazesWorld class
  void testReconstruct(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    this.world1.findMinimalPath();
    this.world1.containOnlyWalls();
    this.world1.cameFromEdge.put(this.world1.board.get(1).get(2), 
        this.world1.board.get(0).get(0));

    t.checkExpect(this.world1.path, new ArrayList<Cell>());
    this.world1.reconstruct(this.world1.cameFromEdge, this.world1.board.get(1).get(2));
    t.checkExpect(this.world1.path, 
        new ArrayList<Cell>(Arrays.asList(this.world1.board.get(1).get(2), 
            this.world1.board.get(0).get(0)))); 
  }

  // test the method search in MazesWorld class
  void testSearch(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    this.world1.findMinimalPath();
    this.world1.containOnlyWalls();

    t.checkExpect(this.world1.alreadySeen.isEmpty(), true);
    t.checkExpect(this.world1.cameFromEdge.containsKey(this.world1.board.get(1).get(2)), false);
    this.world1.search("b");
    t.checkExpect(this.world1.alreadySeen.isEmpty(), false);
    t.checkExpect(this.world1.cameFromEdge.containsKey(this.world1.board.get(1).get(2)), true);
  }
  
  // test the method onKeyEvent in MazesWorld class
  void testOnKeyEvent(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    this.world1.findMinimalPath();
    this.world1.containOnlyWalls();
    
    t.checkExpect(this.world1.count, -1);
    t.checkExpect(this.world1.alreadySeen.isEmpty(), true);
    t.checkExpect(this.world1.cameFromEdge.containsKey(this.world1.board.get(1).get(2)), false);
    this.world1.onKeyEvent("b");
    t.checkExpect(this.world1.count, 0);
    t.checkExpect(this.world1.alreadySeen.isEmpty(), false);
    t.checkExpect(this.world1.cameFromEdge.containsKey(this.world1.board.get(1).get(2)), true);
  }

  // test the method updateColorExploration in Cell class
  void testUpdateColorExploration(Tester t) {
    this.initMazes();
    t.checkExpect(this.cell100.colorExploration, false);
    this.cell100.updateColorExploration();
    t.checkExpect(this.cell100.colorExploration, true);
  }

  // test the method updateColorSolution in Cell class
  void testUpdateColorSolution(Tester t) {
    this.initMazes();
    t.checkExpect(this.cell100.colorSolution, false);
    this.cell100.updateColorSolution();
    t.checkExpect(this.cell100.colorSolution, true);
  }

  // test the method onTick in MazesWorld class
  void testOnTick(Tester t) {
    this.initMazes();
    this.world1.createEdges();
    this.world1.initHashMap();
    Collections.sort(this.world1.worklist);
    this.world1.findMinimalPath();
    this.world1.containOnlyWalls();
    this.world1.count = 0;
    this.world1.alreadySeen = 
        new ArrayList<Cell>(Arrays.asList(this.world1.board.get(0).get(0), 
            this.world1.board.get(0).get(2)));
    
    t.checkExpect(this.world1.count, 0);
    t.checkExpect(this.world1.alreadySeen.get(1).colorExploration, false);
    this.world1.onTick();
    t.checkExpect(this.world1.count, 1);
    t.checkExpect(this.world1.alreadySeen.get(1).colorExploration, true);  
  }

  // to start the game and render the world
  void testMazes(Tester t) {
    MazesWorld world = new MazesWorld(10, 10);
    world.bigBang(world.col * 20, world.row * 20, 0.1);
  }
}
