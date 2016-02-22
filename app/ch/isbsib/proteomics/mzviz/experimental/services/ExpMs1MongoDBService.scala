package ch.isbsib.proteomics.mzviz.experimental.services

import ch.isbsib.proteomics.mzviz.commons.{Moz, Intensity, RetentionTime}
import ch.isbsib.proteomics.mzviz.commons.services.{MongoNotFoundException, MongoDBService}
import ch.isbsib.proteomics.mzviz.experimental.RunId
import ch.isbsib.proteomics.mzviz.experimental.models.{SpectrumRef, Ms1EntryWithRef, ExpMs1Spectrum}
import org.specs2.execute.Success
import play.api.libs.iteratee.Enumerator
import play.api.mvc.Controller
import ch.isbsib.proteomics.mzviz.experimental.services.JsonExpFormats._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.DefaultDB
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.core.commands.{Count, LastError}
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure

/**
 * @author Roman Mylonas, Trinidad Martin & Alexandre Masselot
 *         copyright 2014-2015, SIB Swiss Institute of Bioinformatics
 */
class ExpMs1MongoDBService (val db: DefaultDB) extends MongoDBService {
  val collectionName = "ms1Peaks"
  val mainKeyName = "ref"

  new Index(
    Seq("ref" -> IndexType.Ascending),
    name = Some("ref")
  )

  /**
   * insert every entry with RunId, rt, intensity and moz
   * @param listMS1, iterator of MS1
   * @return number of entries
   */

  def insertListMS1(listMS1: Iterator[ExpMs1Spectrum], intensityThreshold: Double): Future[Int] ={

    var inserted: List[Future[Int]] = List()

    while (listMS1.hasNext) {
      val ms1 = listMS1.next()
      inserted = inserted :+ insertMS1(ms1, intensityThreshold)._1
    }

    // give back the sum of all peaks entered
    Future.sequence(inserted).map(_.sum)
  }


  def insertMS1(ms1: ExpMs1Spectrum, intensityThreshold: Double): (Future[Int], Double, Double) = {
    // store entries to insert in a list 
    var itEntry: List[Ms1EntryWithRef]=List()
    var lowestMoz:Double = 9999999
    var highestMoz:Double = 0

    val runID = ms1.spId.runId
    val rt = ms1.retentionTime
    ms1.peaks.foreach {
      peak =>
        if(peak.intensity.value >= intensityThreshold) {
          val ms1Entry: Ms1EntryWithRef = Ms1EntryWithRef(runID, rt, peak.intensity, peak.moz)
          itEntry = itEntry :+ ms1Entry

          // remember the highest and lowest Moz
          val moz:Double = peak.moz.value
          if(moz < lowestMoz) lowestMoz = moz
          if(moz > highestMoz) highestMoz = moz
        }
    }
    // insert into db
    val enum=Enumerator.enumerate(itEntry)
    val futureNumber:Future[Int] = collection.bulkInsert(enum)

    (futureNumber, lowestMoz, highestMoz)

  }


  /**
   * retrieve a list of Ms1Entries by moz and tolerance. The list is unsorted.
   * @param moz
   * @param tolerance
   * @return seq of entries
   */
  def findMs1ByRunID_MozAndTol(runId: RunId, moz:Moz,tolerance: Double): Future[List[Ms1EntryWithRef]] = {
    val query = Json.obj("moz"->Json.obj("$lte" ->(moz.value + tolerance),"$gte"-> (moz.value - tolerance)))
    collection.find(query).cursor[Ms1EntryWithRef].collect[List]()
  }

  /**
   * retrieve all entries between a certain moz limit
   *
   * @param rundId
   * @param lowerLimit
   * @param upperLimit
   * @return
   */
  def findMs1ByRunID_MozBorders(rundId: RunId, lowerLimit:Moz, upperLimit:Moz): Future[List[Ms1EntryWithRef]] = {
    val query = Json.obj("moz"->Json.obj("$lte" -> upperLimit.value, "$gte"-> lowerLimit.value))
    collection.find(query).cursor[Ms1EntryWithRef].collect[List]()
  }



  /**
   * extract list of intensities and list of moz from list of MS1Entries. Group by retentionTimes and sum the intensities.
   * Then sort by retentionTimes and add 0 values between points which have distances > rtTolerance.
   * @param ms1List
   * @return list of intensities and list of moz
   */

  def extract2FutureLists(ms1List:Future[List[Ms1EntryWithRef]], rtTolerance: Double): Future[JsObject] = {

    // we're in a Future
    ms1List.map(m => {

     extract2Lists(m, rtTolerance)

    })
  }

  def extract2Lists(ms1List:List[Ms1EntryWithRef], rtTolerance: Double): JsObject = {
      if(ms1List.length > 0) {
        // group by retentionTimes
        val rtGroups = ms1List.groupBy(_.rt.value)

        // sum the groups => Map(rt, max(intensities))
        val summedMap = rtGroups.map({ case (k, v) => (k, v.map(_.intensity.value).max) })

        // sort by rt separate the lists and make a Json-object
        val sortedSums = summedMap.toSeq.sortBy(_._1)

        // function to add 0 values between peaks which are not close enough
        def checkAndAdd(b: List[(Double, Double)], a: (Double, Double), maxDiff: Double, f: (Double, Double, Double) => List[(Double, Double)]): List[(Double, Double)] = {
          if (b.last._1 + rtTolerance < a._1) {
            b ++ f(b.last._1, a._1, maxDiff) :+ a
          } else b :+ a
        }

        // helper function to add 0 values
        def addZeroValues(val1: Double, val2: Double, maxDiff: Double): List[(Double, Double)] = {
          if (val1 + maxDiff > val2 - maxDiff) {
            List(Tuple2((val1 + val2) / 2, 0.0))
          } else {
            List(Tuple2(val1 + maxDiff, 0.0), Tuple2(val2 - maxDiff, 0.0))
          }
        }

        val addedMissingRts = sortedSums.drop(1).foldLeft(List(sortedSums(0)))((b, a) => checkAndAdd(b, a, rtTolerance, addZeroValues))

        val aux = addedMissingRts.unzip
        return Json.obj("rt" -> aux._1, "intensities" -> aux._2)
      }else{
        val emptyList:List[Ms1EntryWithRef] = List()
        return Json.obj("rt" -> emptyList, "intensities" -> emptyList)
      }
  }


  /**
   * remove all Ms1Entry for a given run
   * @param runId the run id
   * @return
   */
  def delete(runId: RunId): Future[Boolean] = {
    val query = Json.obj("ref" -> runId.value)
    collection.remove(query).map {
      case e: LastError if e.inError => throw MongoNotFoundException(e.errMsg.get)
      case _ => true
    }
  }


  /**
   * retrieves  by run id
   * @param runId the run id
   * @return seq of entries
   */


  def findMs1ByRunId(runId: RunId): Future[Iterator[Ms1EntryWithRef]] = {
    val query = Json.obj("ref" -> runId.value)
    collection.find(query).cursor[Ms1EntryWithRef].collect[Iterator]()
  }



  /**
   * count the number of Spectra
   * @return
   */
  def countMsnSpectra: Future[Int] = {
    db.command(Count(collectionName))
  }

}


object ExpMs1MongoDBService extends Controller with MongoController {
  val default = new ExpMs1MongoDBService(db)

  /**
   * get the default database
   * @return
   */
  def apply() = default
}


