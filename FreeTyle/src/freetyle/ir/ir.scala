package freetyle.ir

import scala.collection.mutable
import util.control.Breaks._
import scala.language.postfixOps

/**
 * @author apinson
 */


/**
 * Points: Will be declared as tuples (x,y)
 */
class Point(xCoord: Int, yCoord: Int) {
  val x = xCoord
  val y = yCoord
}

/** 
 *  All tiles have a name (to be referred to by maps) and a file (to load in the image)
 */
class Tile(tileName: TileName, url: String) {
  val name = tileName
  val file = new java.io.File(url)
}

/**
 * Base tiles may have an edge specified
 */
class BaseTile(tileName: TileName, url: String, edgeUrl: String) extends Tile(tileName, url) {
  
  if (edgeUrl == "") {
     val edgeFile = null
  } else {
     val edgeFile = new java.io.File(url)
  }
}

/**
 * Freeform tiles have an anchorpoint; the standard is the top left of the image
 */
class FreeTile(tileName: TileName, url: String, anchorPoint: Point) extends Tile(tileName, url) {
  val anchor = anchorPoint
}

/**
 * The Origin specifies how the map is oriented.
 */
abstract sealed class Origin
case object topLeft extends Origin
case object bottomLeft extends Origin

/**
 * Map types are specified for the generate calls
 */
abstract sealed class MapType
case object basic extends MapType
case object debug extends MapType

/**
 * Instructions are given to either fill an area or place a freeform tile;
 * either way, there is a tile associated with them
 */
class Instr(t: TileName){
  val tileName = t
}

/**
 * Areas have a list of points specifying the area to fill
 * The rect field specifies whether or not the area being filled is rectangular;
 * if it is, filling it in is simplified
 */
class Area(t: TileName, pointList: List[Point], rectangle: Boolean) extends Instr(t){
  val rect = rectangle
  val points = pointList
  if ((points.length <= 1 || points.length > 4) && rect) {
    error("A rectangle must be specified with between 2 and 4 points")
  }
  //make sure the area has to have more than 2 points otherwise
  if (!rect && points.length <= 2) {
    error("Areas must be specified with more than 2 points")
  }
}

/**
 * PlacePoints have a list of points specifying where to place the tile
 */
class PlacePoint(t: TileName, p: List[Point]) extends Instr(t){
  val points = p
}

/**
 * Layers have a precedence number, where lower numbered layers are on the bottom,
 * and a list of instructions to do for that layer
 */
class Layer(prec: LayerNum, is: List[Instr]) {
  val precedence = prec
  val instructions = sortInstrs(is)
  
  // Sort so that the fill instructions (Area) come before the place instructions (PlacePoint)
  // This guarantees that freeform tiles are always placed above the filled in areas
  def sortInstrs(instrs: List[Instr]): List[Instr] = {
  // Bubblesort
    if (instrs.length == 0 || instrs.length == 1) {return instrs}
    else {
      // Start at the end of the list
      var i = instrs.length - 1
      do {
        var j = i
        instrs(i) match {
          // Move any Area calls up
          case a: Area => do {
                               // Backtrack from i until the first PlacePoint is found
                               j -= 1
                               instrs(j) match {
                                 case a: Area => null
                                 case p: PlacePoint => {
                                   // Swap the PlacePoint with the Area
                                   var temp = instrs(i)
                                   instrs.updated(i, instrs(j))
                                   instrs.updated(j, temp)
                                 }
                               }
                              } while (j > 0)
          case p: PlacePoint => null
      }
        // If we hit the beginning with j, then the list only has Areas
        // Or the list is sorted so that all the preceding values are Areas
        if (j == 0) {return instrs}
        i -= 1
      } while (i > 0)
        return instrs
    }
  }
}

/**
 * Map objects have the origin, specifying their orientation, width and height,
 * and a list of layers
 */
class Map(w: Int, h: Int, orig: Origin, lays: List[Layer]) {
  val width = w
  val height = h
  val origin = orig
  val layers = layerMergeSort(lays)
  
  /**
   * The layers are sorted by precedence number
   */
  def layerMergeSort (layers: List[Layer]): List[Layer] = {
      if(layers.length == 1 || layers.length == 0) {return layers}
      else {
        val mid = layers.length/2
        val split = layers.splitAt(mid)
        val lhs = layerMergeSort(split._1)
        val rhs = layerMergeSort(split._2)
        return merge(lhs, rhs)
      }
    }
    
    def merge(l1: List[Layer], l2: List[Layer]): List[Layer] = {
      if (l1.length == 0) {return l2}
      if (l2.length == 0) {return l1}
      
      val headA = l1.head
      val headB = l2.head
      if(headA.precedence < headB.precedence) {
        return headA :: headB :: merge(l1.tail, l2.tail)
      } else if(headB.precedence < headA.precedence) {
        return headB :: headA :: merge(l1.tail, l2.tail)
      }
      else {
        error("Each layer must have a unique precedence value")
        return List()
      }
    }
}

/**
 * The table has all possible tiles that can be used to build the map.
 * If two tiles are assigned to the same name, an "error" tile is generated,
 * which will be checked in semantics and throw an error.
 */
class Table(list: List[(TileName, Tile)]) {
  val hash = new mutable.HashMap[TileName, Tile]
  for ((tileName, tile) <- list) {
    if (hash.contains(tileName) == false) {
      hash(tileName) = tile
    } else {
      hash("error") = new Tile("error", "")
    }
  }
}
/**
 * The AST has three main components:
 *    a table of tiles (for the map construction to use)
 *    the map object itself
 *    and a list of "generate" calls (this will probably either be generate the normal map, debug map, or both)
 */
class AST(tList: List[(TileName, Tile)], m: Map, gens: List[(MapType, String)]) {
  val tileTable = new Table(tList)
  val map = m
  val genCalls = gens
}
