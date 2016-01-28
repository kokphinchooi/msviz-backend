package ch.isbsib.proteomics.mzviz.matches.services

import java.io.File

import ch.isbsib.proteomics.mzviz.commons._
import ch.isbsib.proteomics.mzviz.experimental.services.ExpMongoDBService
import ch.isbsib.proteomics.mzviz.experimental.{SpectrumUniqueId, RunId}
import ch.isbsib.proteomics.mzviz.matches.SearchId
import ch.isbsib.proteomics.mzviz.matches.importer.{LoaderMaxQuant, LoaderMzIdent}
import ch.isbsib.proteomics.mzviz.modifications.ModifName
import ch.isbsib.proteomics.mzviz.theoretical.{SequenceSource, AccessionCode}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.specs2.mutable.Specification

import play.api.test._
import play.api.test.Helpers._


/**
 * @author Roman Mylonas, Trinidad Martin & Alexandre Masselot
 * copyright 2014-2015, SIB Swiss Institute of Bioinformatics
 */
class MatchesMongoDBServiceSpecs extends Specification with ScalaFutures {
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(15, Seconds), interval = Span(5000, Millis))

  /**
   * extends the temp mngodatabase and add a exp service above it
   */
  trait TempMongoDBService extends TempMongoDBForSpecs {
    val service = new MatchMongoDBService(db)
    val expService = new ExpMongoDBService(db)
  }

  "empty service" should {
    "counts are 0" in new TempMongoDBService {
      service.countEntries.futureValue must equalTo(0)
      service.countRunIds.futureValue must equalTo(0)
    }
  }
/*
  "import and insert q psm list" should {

    val file_1 = new File("test/resources/mascot/M_100.mzid")
    val file_2 = new File("test/resources/mascot/F001644.mzid")

    "get them up " in new TempMongoDBService {
      running(FakeApplication()) {
        service.countEntries.futureValue must equalTo(0)
        service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
        Thread.sleep(200)
        service.countEntries.futureValue must equalTo(62)
        service.insert(LoaderMzIdent.parse(file_2, SearchId("F001644"), RunId("F001644.mgf"))._1).futureValue
        Thread.sleep(200)
        service.countEntries.futureValue must equalTo(499)
        service.countRunIds.futureValue must equalTo(2)
      }
    }

  }

  "delete" should {

    val file_1 = new File("test/resources/mascot/M_100.mzid")

    "get 2 , remove 1 " in new TempMongoDBService {

      running(FakeApplication()) {
        service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
        Thread.sleep(200)
        val psmList = service.findAllPSMBySearchId(SearchId("M_100")).futureValue
        psmList.size must equalTo(62)
        service.deleteAllBySearchId(SearchId("M_100")).futureValue
        Thread.sleep(200)
        service.countEntries.futureValue must equalTo(0)
        service.countRunIds.futureValue must equalTo(0)

      }

    }

  }
  "findAllEntriesByRunId" should {
    "find all" in new TempMongoDBService {

      running(FakeApplication()) {

        val file_1 = new File("test/resources/mascot/M_100.mzid")

        //insert and check size
        service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
        val idList = service.findAllSpectrumIdBySearchId(SearchId("M_100")).futureValue
        Thread.sleep(200)
        idList.size must equalTo(62)

        //check JSON content
        idList(0).id must equalTo(SpectrumUniqueId(27))
        idList(0).runId must equalTo(RunId("M_100.mgf"))
      }
    }

  }

  "findAllPSMByRunId" should {
    running(FakeApplication()) {
      val file_1 = new File("test/resources/mascot/M_100.mzid")

      "find all" in new TempMongoDBService {
        running(FakeApplication()) {

          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
          val psmList = service.findAllPSMBySearchId(SearchId("M_100")).futureValue
          Thread.sleep(200)
          psmList.size must equalTo(62)
        }
      }
    }
  }


*/
  "listProteinRefsBySearchId" should {

    running(FakeApplication()) {
      val file_1 = new File("test/resources/mascot/M_100.mzid")

      "list all" in new TempMongoDBService {
        running(FakeApplication()) {
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
          val protRefList = service.listProteinRefsBySearchIds(Set(SearchId("M_100"))).futureValue
          Thread.sleep(200)
          protRefList.size must equalTo(27)
          protRefList(0).AC mustEqual AccessionCode("CD109_HUMAN")
          protRefList(0).source mustEqual Some(SequenceSource("SwissProt_2014_08.fasta"))
        }
      }
    }
  }


  "listProteinRefsBySearchId" should {

    running(FakeApplication()) {

      val file_1 = new File("test/resources/mascot/M_100.mzid")

      "list all" in new TempMongoDBService {
        running(FakeApplication()) {
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
          val protRefList = service.listProteinRefsBySearchIds(Set(SearchId("M_100"))).futureValue
          Thread.sleep(200)
          protRefList.size must equalTo(27)
          protRefList(0).AC mustEqual AccessionCode("CD109_HUMAN")
          protRefList(0).source mustEqual Some(SequenceSource("SwissProt_2014_08.fasta"))
        }
      }

    }

  }

  "listProteinRefsBySearchIdWithModification" should {

    running(FakeApplication()) {

      val file_1 = new File("test/resources/mascot/M_100.mzid")

      "list all" in new TempMongoDBService {
        running(FakeApplication()) {
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
          val protRefList = service.listProteinRefsBySearchIds(Set(SearchId("M_100")), Option(ModifName("Acetyl"))).futureValue
          Thread.sleep(200)
          protRefList.size must equalTo(1)
          protRefList(0).AC mustEqual AccessionCode("ANXA2_HUMAN")
          protRefList(0).source mustEqual Some(SequenceSource("SwissProt_2014_08.fasta"))
        }
      }

    }

  }

  "findPSMByProtein" should {

    running(FakeApplication()) {

      val file_1 = new File("test/resources/mascot/M_100.mzid")

      "list all" in new TempMongoDBService {
        running(FakeApplication()) {
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100_1"), RunId("M_100.mgf"))._1).futureValue

          val psms = service.findAllPSMsByProtein(AccessionCode("CD109_HUMAN")).futureValue
          //println(psms.mkString("\n"))

          psms.size must equalTo(4)
        }
      }
      "list all" in new TempMongoDBService {
        running(FakeApplication()) {
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
          service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100_1"), RunId("M_100.mgf"))._1).futureValue

          val psms = service.findAllPSMsByProtein(AccessionCode("CD109_HUMAN"), searchIds = Some(Set(SearchId("M_100_1")))).futureValue
          //println(psms.mkString("\n"))

          psms.size must equalTo(2)
        }
      }

    }
  }
/*
  "find All Modifications By SearchIds" in new TempMongoDBService {

    running(FakeApplication()) {

     // val file_1 = new File("test/resources/mascot/M_100.mzid")
      val loadMaxQ= LoaderMaxQuant.parse("test/resources/maxquant/")
      val insertMQ= service.insert(loadMaxQ(0)._1)
      //service.insert(LoaderMzIdent.parse(file_1, SearchId("M_100"), RunId("M_100.mgf"))._1).futureValue
      val setSearchIds= Set(SearchId("4-Nocodazole"))
      Thread.sleep(20000)
      val hashModif= service.findAllModificationsBySearchIds(setSearchIds)
      hashModif.futureValue mustEqual(Map("Acetyl" -> 1))
    }

  }
*/



}