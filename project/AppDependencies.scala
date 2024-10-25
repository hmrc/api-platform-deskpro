import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  private val bootstrapVersion = "9.5.0"
  private val hmrcMongoVersion = "2.2.0"
  private val commonDomainVersion = "0.17.0"

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-30"    % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"           % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "api-platform-common-domain"   % commonDomainVersion,
    "uk.gov.hmrc"             %% "internal-auth-client-play-30" % "3.0.0",
    "uk.gov.hmrc"             %% "http-metrics"                 % "2.8.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"              % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"             % hmrcMongoVersion,
    "uk.gov.hmrc"             %% "api-platform-common-domain-fixtures" % commonDomainVersion
  ).map(_ % Test)

  val it = Seq.empty
}
