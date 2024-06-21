rootProject.name = "backend"

include(
    "collector",
    "apigateway",
    "publisher",
    "lib:commonconfig",
    "lib:transport",
    "lib:collectsynchronizer"
)
