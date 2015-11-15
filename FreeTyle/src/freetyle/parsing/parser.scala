package freetyle.parsing

import freetyle.ir._
import scala.util.parsing.combinator._
import scala.collection.mutable

/**
 * @author apinson
 */
object parser extends JavaTokenParsers with PackratParsers{
  
  def apply(s: String): ParseResult[AST] = parseAll(file, s)
  
  lazy val file: PackratParser[AST] = (
      (tile.+)~map~(generates.+) ^^ {case ts~m~gs => new AST(ts, m, gs)}
      | map~(generates.+)~>failure("Expected one or more tile specifications")
      | (tile.+)~(generates.+)~>failure("Expected a map specification")
      | failure("File needs to have one or more tiles and a map specified")
      )
      
  lazy val tile: PackratParser[(TileName, Tile)] = (
      rword("tile")~>tilename~rword("=")~path~edge ^^ {case tname~"="~u~e => (tname, new BaseTile(tname, u, e))}
      | rword("tile")~>tilename~rword("=")~path ^^ {case tname~"="~u => (tname, new  BaseTile(tname, u, ""))}
      | rword("freeform")~>rword("tile")~>tilename~rword("=")~path~anchor ^^ {case tname~"="~p~a => (tname, new FreeTile(tname, p, a))}
      | rword("freeform")~>rword("tile")~>tilename~rword("=")~path ^^ {case tname~"="~p => (tname, new FreeTile(tname, p, new Point(0,0)))}
      )
      
//  lazy val baseTile: PackratParser[(TileName, Tile)] = (
//      rword("tile")~>tilename~rword("=")~path~edge ^^ {case tname~"="~u~e => (tname, new BaseTile(tname, u, e))}
//      | rword("tile")~>tilename~rword("=")~path ^^ {case tname~"="~u => (tname, new  BaseTile(tname, u, ""))}
//      )
      
  lazy val edge: PackratParser[String] = (
      rword("{")~>rword("edge")~>rword("=")~>path~rword("}") ^^ {case p~"}" => p}
      | failure("A proper edge definition was not supplied")
      )
      
//  lazy val freeTile: PackratParser[(TileName, Tile)] = (
//      rword("freeform")~>rword("tile")~>tilename~rword("=")~path~anchor ^^ {case tname~"="~p~a => (tname, new FreeTile(tname, p, a))}
//      | rword("freeform")~>rword("tile")~>tilename~rword("=")~path ^^ {case tname~"="~p => (tname, new FreeTile(tname, p, new Point(0,0)))}
//      )
  
  lazy val anchor: PackratParser[Point] = (
      rword("{")~>rword("anchor")~>rword("=")~>point~rword("}") ^^ {case p~"}" => p}
      | failure("A proper anchor point was not supplied")
      )
  
  lazy val map: PackratParser[Map] = (
      rword("map")~>rword("{")~>width~height~origin~(layer.+)~rword("}") ^^ {case w~h~o~l~"}" => new Map(w,h,o,l)}
      | rword("map")~>rword("{")~>height~width~origin~(layer.+)~rword("}") ^^ {case h~w~o~l~"}" => new Map(w,h,o,l) }
      | rword("map")~>rword("{")~>width~origin~height~(layer.+)~rword("}") ^^ {case w~o~h~l~"}" => new Map(w,h,o,l) }
      | rword("map")~>rword("{")~>height~origin~width~(layer.+)~rword("}") ^^ {case h~o~w~l~"}" => new Map(w,h,o,l) }
      | rword("map")~>rword("{")~>origin~width~height~(layer.+)~rword("}") ^^ {case o~w~h~l~"}" => new Map(w,h,o,l) }
      | rword("map")~>rword("{")~>origin~height~width~(layer.+)~rword("}") ^^ {case o~h~w~l~"}" => new Map(w,h,o,l) }
      | rword("map")~>rword("{")~>height~width~(layer.+)~rword("}") ^^ {case h~w~l~"}" => new Map(w,h,topLeft,l) }
      | rword("map")~>rword("{")~>width~height~(layer.+)~rword("}") ^^ {case w~h~l~"}" => new Map(w,h,topLeft,l) }
      | failure("Map specifications must come before layers and must include a width and a height")
      )
      
  
      
      //TODO: Fix this?
  lazy val layer: PackratParser[Layer] = (
      rword("layer")~>wholeNumber~rword("=")~rword("{")~(instr.+)~rword("}") ^^ {case num~"="~"{"~is~"}" => new Layer(num.toInt, is)}
      )
      
  lazy val instr: PackratParser[Instr] = (
      placeAt ^^ {case p => p}
//      | fillArea ^^ {case f => f}
      )
      
      //TODO: Add areas/shapes
      
//  lazy val fillArea: PackratParser[Area] = (
//      
//      )
      
  lazy val placeAt: PackratParser[PlacePoint] = (
      rword("at")~>(point.+)~rword("place")~tilename ^^ {case points~"place"~tname => new PlacePoint(tname, points) }
      | failure("Improper specification of at statement")
      )
       
      //TODO: Make generate calls optional
  lazy val generates: PackratParser[(MapType, String)] = (
      rword("generate")~>rword("map")~>rword("as")~>filename ^^ {case fname => (basic, fname)}
      | rword("generate")~>rword("debug")~>rword("map")~>rword("as")~>filename ^^ {case fname => (debug, fname)}
      )
      
  lazy val width: PackratParser[Int] = (
      rword("width")~>rword("=")~>wholeNumber ^^ {case num => num.toInt}
      | failure("Improper width specification (all numbers must be ints)")
      )
      
  lazy val height: PackratParser[Int] = (
      rword("height")~>rword("=")~>wholeNumber ^^ {case num => num.toInt}
      | failure("Improper height specification (all numbers must be ints)")
      )
      
  lazy val origin: PackratParser[Origin] = (
      rword("origin")~>rword("=")~>originKeyword ^^ {case key => key}
      | failure("Improper origin specification")
      )
      
      
      //TODO: Fix this
  lazy val originKeyword: PackratParser[Origin] = ( //ident ^^^ topLeft
      "bottomLeft" ^^^ bottomLeft
      | "topLeft" ^^^ topLeft
      | failure("Improper origin keyword")
      )   
      
  def rword(word: String): PackratParser[String] = {
    ident filter {_ == word} withFailureMessage "Expected reserved word <" + word + ">."
  }
  
  lazy val point: PackratParser[Point] = (
      rword("(")~>wholeNumber~rword(",")~wholeNumber~rword(")") ^^ {case x~","~y~")" => new Point(x.toInt, y.toInt)}
      )
  
  def tilename: Parser[TileName] = ident
  def path: Parser[String] = ident
  def filename: Parser[String] = ident

}