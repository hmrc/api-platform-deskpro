resolvers += MavenRepository("HMRC-open-artefacts-maven2", "https://open.artefacts.tax.service.gov.uk/maven2")
resolvers += Resolver.url("HMRC-open-artefacts-ivy2", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"        % "3.22.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"    % "2.5.0")
addSbtPlugin("org.playframework" % "sbt-plugin"            % "3.0.4")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "2.0.9")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("ch.epfl.scala"     % "sbt-bloop"             % "1.5.15")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"          % "2.5.2")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"          % "0.11.1")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
