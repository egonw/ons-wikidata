// Copyright (C) 2018-2019  Egon Willighagen
//               2018       Denise Slenter
// License: MIT

// Usage:
//
//   change the values of the following two variables. The first is the text string you
//   wish to find in titles of articles in Wikidata, and the second is the actual Wikidata
//   item of the concept.
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/
//
// Changelog:
//
// 2018-09-01 First upload to MyExperiment.org
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.31')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.31')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.31')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);

batchSize = 100000

def cli = new CliBuilder(usage: 'findConcepts.groovy')
cli.h(longOpt: 'help', 'print this message')
cli.s(longOpt: 'search-string', args:1, argName:'query', 'Search this query in the article titles')
cli.q(longOpt: 'qid', args:1, argName:'qid', 'QID of the item to set as main item')
cli.o(longOpt: 'offset', args:1, argName:'offset', 'Number of batches (batch size: ${batchSize}) to skip')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
  System.exit(0)
}

concept = null
conceptQ = null
if (options.s) concept = options.s
if (options.q) conceptQ = options.q
startBatch = 0
if (options.o) startBatch = Integer.parseInt(options.o)

if (concept == null | conceptQ == null) {
  println "A search string and QID must be given."
  System.exit(-1)
}

// the next is a list of false positives (all lower case)

blacklist = [
]

// the code (don't change)

concept = concept.toLowerCase()

totalArticleCount = 20000000
batchSize = 100000

start = startBatch*batchSize

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

qsFile = "/Wikicite/output." + concept.replace(" ", "_") + ".quickstatements"
renewFile(qsFile)
ui.append(qsFile, "qid,P921,#\n")

rounds = (int)Math.ceil((totalArticleCount-start) / batchSize)
1.upto(rounds) { counter ->
  print "batch ${counter}/${rounds}: "
  offset = start + (counter-1)*batchSize
  sparql = """
    SELECT ?art ?artLabel
    WITH {
      SELECT ?art WHERE {
        ?art wdt:P31 wd:Q13442814
      } LIMIT $batchSize OFFSET $offset
    } AS %RESULTS { 
      INCLUDE %RESULTS
      MINUS { ?art wdt:P921 wd:$conceptQ }
      ?art wdt:P1476 ?artLabel .
      FILTER (contains(lcase(str(?artLabel)), "$concept"))
    }
  """
  if (bioclipse.isOnline()) {
    try {
      rawResults = bioclipse.sparqlRemote(
        "https://query.wikidata.org/sparql", sparql
      )
      results = rdf.processSPARQLXML(rawResults, sparql)
      missing = results.rowCount == 0
      if (!missing) {
        println "found ${results.rowCount} article(s)!"
        printlnOutput = ""
        fileOutput = ""
        1.upto(results.rowCount) { artCounter ->
          artTitle = results.get(artCounter, "artLabel")
          blacklisted = false
          blacklist.each { badWord ->
            if (artTitle.toLowerCase().contains(badWord.toLowerCase())) {
              blacklisted = true
            }
          }
          if (!blacklisted) {
            artIRI = results.get(artCounter, "art")
            artQ = artIRI.substring(31)
            printlnOutput += "${artQ}\t${artTitle}\n"
            artTitle = artTitle.replaceAll(",", ";")
            fileOutput += "${artQ},${conceptQ},${artTitle}\n"
          }
        }
        print(printlnOutput)
        ui.append(qsFile, fileOutput)
      } else {
        println "no hits"
      }
    } catch (Exception exception) {
      println "Error while retrieving this batch: " + exception.message
    }
  } else {
    println "no online access"
  }
}
ui.open(qsFile)
