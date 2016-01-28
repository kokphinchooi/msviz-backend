package ch.isbsib.proteomics.mzviz.controllers.matches

import javax.ws.rs.PathParam

import ch.isbsib.proteomics.mzviz.controllers.JsonCommonsFormats._
import ch.isbsib.proteomics.mzviz.controllers.matches.PSMController._
import ch.isbsib.proteomics.mzviz.experimental.RunId
import ch.isbsib.proteomics.mzviz.matches.importer.{LoaderMaxQuant, LoaderMzIdent}
import ch.isbsib.proteomics.mzviz.matches.services.JsonMatchFormats._
import ch.isbsib.proteomics.mzviz.experimental.services.ExpMongoDBService
import ch.isbsib.proteomics.mzviz.matches.SearchId
import ch.isbsib.proteomics.mzviz.matches.models.{ProteinIdent, PepSpectraMatch, SearchInfo}
import ch.isbsib.proteomics.mzviz.matches.services.{ProteinMatchMongoDBService, MatchMongoDBService, SearchInfoDBService}
import com.wordnik.swagger.annotations._
import play.api.Logger
import play.api.cache.Cached
import play.api.libs.json._
import play.api.mvc.Action
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @author Roman Mylonas, Trinidad Martin & Alexandre Masselot
 *         copyright 2014-2015, SIB Swiss Institute of Bioinformatics
 */
@Api(value = "/searches", description = "searches")
object SearchController extends MatchController {


  def stats = Action.async {
    ExpMongoDBService().stats.map { st =>
      Ok(jsonWritesMap.writes(st))
    }
  }

  @ApiOperation(nickname = "listMSRunIds",
    value = "the list of run ids",
    notes = """from the parameter run-id at load time""",
    response = classOf[List[String]],
    httpMethod = "GET")
  def listMSRunIds = Action.async {
    ExpMongoDBService().listMsRunIds.map {
      ids => Ok(Json.obj("msRuns" -> ids.map(_.value)))
    }
  }

  @ApiOperation(nickname = "list",
    value = "the list of search ids",
    notes = """from the parameter search-id at load time""",
    response = classOf[List[String]],
    httpMethod = "GET")
  def list =
    Action.async {
      SearchInfoDBService().list.map {
        searchInfos => Ok(Json.toJson(searchInfos))
      }
    }


  @ApiOperation(nickname = "get",
    value = "find all SearchInfo object for a given searchId",
    notes = """SearchInfos  list in TSV or JSON form""",
    response = classOf[List[SearchInfo]],
    produces = "application/json, application/tsv",
    httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "searchId", value = "", required = false, dataType = "string", paramType = "searchId")
  ))
  def get(
           @ApiParam(value = """searchId""", defaultValue = "M_100") @PathParam("searchId") searchId: String
           ) = Cached(req => req.uri) {
    Action.async {
      for {
        searchInfo <- SearchInfoDBService().get(SearchId(searchId))
      } yield {
        Ok(Json.toJson(searchInfo))
      }

    }
  }

  @ApiOperation(nickname = "loadResults",
    value = "Loads an MzIdentMl or MaxQuant result",
    notes = """ """,
    response = classOf[String],
    httpMethod = "POST")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "result file", required = true, paramType = "body")
  ))
  def loadResults(@ApiParam(name = "searchId", value = "a string id with search identifier") @PathParam("searchId") searchId: String,
               resultType: Option[String], // mzIdentML or maxQuant
               runId: Option[String]) =
    Action.async(parse.temporaryFile) {
      request =>
        val rid = runId match {
          case Some(r: String) => RunId(r)
          case None => RunId(searchId)
        }

        (for {

          results  <-  Future { resultType.getOrElse("mzIdentML") match {
            case "mzIdentML" => Seq(LoaderMzIdent.parse(request.body.file, SearchId(searchId), rid))
            case "maxQuant" => LoaderMaxQuant.parseZip(request.body.file, searchId)
            case _ => throw new Exception(s"illegal resultType [$resultType] -> accepted types are: mzIdentML, maxQuant")
          }

          }

        } yield {
            results.foreach({ psmAndProteinList =>
              MatchMongoDBService().insert(psmAndProteinList._1)
              ProteinMatchMongoDBService().insert(psmAndProteinList._2)
              SearchInfoDBService().insert(psmAndProteinList._3)
            })

            Ok(Json.obj("inserted" -> results.length))
        }).recover {
          case e =>
            Logger.error(e.getMessage, e)
            BadRequest(Json.prettyPrint(Json.obj("status" -> "ERROR", "message" -> e.getMessage)) + "\n")
        }
    }


  @ApiOperation(nickname = "delete",
    value = "delete PSMs, Proteins & searchInfo for a given list of searchIds (or one), seperated by comma",
    notes = """No double check is done. Use with caution""",
    response = classOf[String],
    httpMethod = "DELETE")
  def delete(@ApiParam(value = """searchIds""", defaultValue = "") @PathParam("searchIds") searchIds: String) = Action.async {
    for {
      dPsms <- MatchMongoDBService().deleteAllBySearchIds(queryParamSearchIds(searchIds))
      dProt <- ProteinMatchMongoDBService().deleteAllBySearchIds(queryParamSearchIds(searchIds))
      dSearchInfo <- SearchInfoDBService().deleteAllBySearchIds(queryParamSearchIds(searchIds))
    } yield {
      Ok (Json.obj(
        "psms" -> dPsms,
        "proteinMatches" -> dProt,
        "searchInfos" -> dSearchInfo
      ))
    }
  }
}
