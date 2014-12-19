package ch.isbsib.proteomics.mzviz.matches.models

import ch.isbsib.proteomics.mzviz.commons._
import ch.isbsib.proteomics.mzviz.matches.{SearchId}
import ch.isbsib.proteomics.mzviz.theoretical.{SequenceSource, AccessionCode}
import org.specs2.mutable.Specification

/**
 * Created by Roman Mylonas on 25/11/14.
 */
class PepSpectraMatchSpecs extends Specification {
  "pepSpectraMatch" should {

    // vals used in following tests
    val matching = PepMatchInfo(scoreMap = Map("p-value" -> 0.001), numMissedCleavages = Option(1), massDiff = Option(0.01), rank = 1, totalNumIons = Option(1), isRejected = None)
    val pep = Peptide(sequence = "AKKKAA", molMass = 123.23, dbSequenceRef="dbref_01")
    val protRef = ProteinRef(AC = AccessionCode("AC001"), source = Some(SequenceSource("dbref")))
    val protMatch = Seq(ProteinMatch(proteinRef = protRef, previousAA = "A", nextAA = "K", startPos = 1, endPos = 10))

    "create simple Peptide" in {
      pep.molMass must equalTo(123.23)
    }

    "create simple PepMatchInfo" in {
      matching.scoreMap("p-value") must equalTo(0.001)
      matching.isRejected should be(None)
    }

    "create simple ProteinMatch" in {
      protMatch.size mustEqual 1
      protMatch(0).proteinRef.AC mustEqual(AccessionCode("AC001"))
      protMatch(0).proteinRef.source mustEqual(Some(SequenceSource("dbref")))
    }

    "create simple PepSpectraMatch" in {
      val pepSpMatch = PepSpectraMatch(searchId = SearchId("a search result"), spId = SpectraId("sp_01"), spSource = SpectraSource("blabla.mgf"), pep = pep, matchInfo = matching, proteinList = protMatch)
      pepSpMatch.spId must equalTo(SpectraId("sp_01"))
      pepSpMatch.spSource must equalTo(SpectraSource("blabla.mgf"))
      pepSpMatch.matchInfo.massDiff must equalTo(Some(0.01))
      pepSpMatch.searchId must equalTo(SearchId("a search result"))
    }

  }
}
