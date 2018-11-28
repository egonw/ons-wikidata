// Copyright (C) 2018  Egon Willighagen
//               2018  Denise Slenter
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

concept = "aflatoxin B1"
conceptQ = "Q4689278"

// the next is a list of false positives (all lower case)

blacklist = [
]

// the code (don't change)
//
// Changelog:
//
// 2018-09-01 First upload to MyExperiment.org

concept = concept.toLowerCase()

// totalArticleCount = 17500000
totalArticleCount = 750000
batchSize = 250000

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

qsFile = "/Wikicite/output." + concept.replace(" ", "_") + ".quickstatements"
renewFile(qsFile)

rounds = (int)Math.ceil(totalArticleCount / batchSize) 
1.upto(rounds) { counter ->
  print "batch ${counter}/${rounds}: "
  offset = (counter-1)*batchSize
  sparql = """
    SELECT ?art ?artLabel
    WITH {
      SELECT ?art WHERE {
        ?art wdt:P31 wd:Q13442814  
      } LIMIT $batchSize OFFSET $offset
    } AS %RESULTS {
      INCLUDE %RESULTS
      ?art wdt:P1476 ?artLabel .
      MINUS { ?art wdt:P921 wd:$conceptQ }
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
            printlnOutput += "${artQ}\t" + artTitle + "\n"
            fileOutput += "${artQ}\tP921\t${conceptQ}\n"
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
