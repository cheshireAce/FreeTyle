package freetyle

/**
 * @author apinson
 */

package object ir {
  /** The number for a layer, which specified precedence */
  type LayerNum = Int
  /** The name of a tile */
  type TileName = String
  /** The abstract syntax tree for this language */
  type AST = List[Layer]
}