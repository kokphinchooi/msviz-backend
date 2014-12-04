package ch.isbsib.proteomics.mzviz.matches.models

import ch.isbsib.proteomics.mzviz.commons._
import org.specs2.mutable.Specification

/**
 * Created by Roman Mylonas on 25/11/14.
 */
class PepSpectraMatchSpecs extends Specification {
  "pepSpectraMatch" should {

    // vals used in following tests
    val matching = PepMatchInfo(scoreMap = Map("p-value" -> 0.001), numMissedCleavages = Option(1), massDiff = Option(0.01), rank = 1, totalNumIons = Option(1), isRejected = None)
    val pep = Peptide(sequence = "AKKKAA", molMass = 123.23, dbSequenceRef="dbref_01")
    val protMatch = Seq(ProteinMatch(AC = "AC001", previousAA = "A", nextAA = "K", startPos = 1, endPos = 10))

    "create simple Peptide" in {
      pep.molMass must equalTo(123.23)
    }

    "create simple PepMatchInfo" in {
      matching.scoreMap("p-value") must equalTo(0.001)
      matching.isRejected should be(None)
    }

    "create simple ProteinMatch" in {
      protMatch.size mustEqual 1
    }

    "create simple PepSpectraMatch" in {
      val pepSpMatch = PepSpectraMatch(spId = SpectraId("sp_01", "blabla.mgf"), pep = pep, matchInfo = matching, proteinList = protMatch)
      pepSpMatch.spId must equalTo(SpectraId("sp_01", "blabla.mgf"))
      pepSpMatch.matchInfo.massDiff must equalTo(Some(0.01))
    }

  }
}