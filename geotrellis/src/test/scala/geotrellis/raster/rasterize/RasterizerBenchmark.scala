package benchmark.geotrellis.raster.rasterize

import geotrellis._
import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.mapalgebra._
import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json._
import geotrellis.raster.PixelSampleType
import geotrellis.raster.rasterize.polygon._
import geotrellis.raster.rasterize.Rasterizer.Options

import scala.math.{min, max}
import scala.util.Random

class RasterizerBenchmark extends OperationBenchmark {
  var r: Tile = _
  var re: RasterExtent = _
  var tile: IntArrayTile = _
  var poly: vector.PolygonFeature[Int] = _

//  @Param(Array("512","1024","2048","4096","8192"))
//  @Param(Array("512","1024","2048"))
  @Param(Array("512"))
  var rasterSize: Int = 0

  var transitPoly: Polygon = null
  var transitPolyNoHoles: Polygon = null
  var transitRe: RasterExtent = null

  override def setUp() {
    r = randomRasterN(rasterSize)
    // rasters go from 0,0 to 10n,10n so we'll stick
    // a triangle in here

    val p1 = Point(0,0)
    val p2 = Point(10*rasterSize,0)
    val p3 = Point(10*rasterSize/2, 10*rasterSize)
    poly = PolygonFeature(Polygon(Line(p1,p2,p3,p1)), 1)

    transitPoly = GeoJson.fromFile[Polygon]("../raster-test/data/transitgeo.json")
    transitPolyNoHoles = Polygon(transitPoly.exterior)
    val vector.Extent(xmin, ymin, xmax, ymax) = transitPoly.envelope
    val dx = (xmax - xmin) / 4
    val dy = (ymax - ymin) / 4
    val ext = Extent(xmin - dx, ymin - dy, xmax + dx, ymax + dy)
    transitRe = RasterExtent(ext, rasterSize, rasterSize)
  }

  def rasterize() = {
    poly.geom.foreach(re)({ (col: Int, row: Int) =>
      tile.set(col,row,4)
    })
  }

  //Because of a refactor, Callback is not getting a geom as a param, since it can close over it if it really wanted
  //this renders the following benchmark pointless, but lets preserve this file in case other cases emerge
  def rasterizeUsingValue() = {
    poly.geom.foreach(re)({ (col: Int, row: Int) =>
      tile.set(col,row, poly.data)
    })
  }


  def timeRasterizer(reps:Int) = run(reps)(rasterize())
  def timeRasterizerUsingValue(reps:Int) = run(reps)(rasterize())

  def randomRasterN(n: Int) = {
    val a = Array.ofDim[Int](n*n).map(a => Random.nextInt(255))
    IntArrayTile(a, n, n)
  }

  def timeRasterizeTransitPoly(reps: Int) = run(reps)(rasterizeTransitPoly)
  def rasterizeTransitPoly = {
    var x = 0
    val options = Options(includePartial = true, sampleType = PixelIsArea)
    transitPoly.foreach(transitRe, options)({ (col: Int, row: Int) =>
      x += (col + row)
    })
  }

  def timeRasterizeTransitPolyNoHoles(reps: Int) = run(reps)(rasterizeTransitPolyNoHoles)
  def rasterizeTransitPolyNoHoles = {
    var x = 0
    val options = Options(includePartial = true, sampleType = PixelIsArea)
    transitPolyNoHoles.foreach(transitRe, options)({ (col: Int, row: Int) =>
      x += (col + row)
    })
  }
}