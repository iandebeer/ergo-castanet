import Util._

addCommandAlias("l", "projects")
addCommandAlias("ll", "projects")
addCommandAlias("ls", "projects")
addCommandAlias("cd", "project")
addCommandAlias("c", "compile")
addCommandAlias("ca", "test:compile")
addCommandAlias("t", "test")
addCommandAlias("r", "run")
addCommandAlias("rs", "reStart")
addCommandAlias("s", "reStop")
addCommandAlias(
  "up2date",
  "reload plugins; dependencyUpdates; reload return; dependencyUpdates",
)
