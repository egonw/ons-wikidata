// Copyright (C) 2018  Egon Willighagen
// License: MIT

totalArticleCount = 15500000

batchSize = 500000
concept = "MetaboLights"
conceptQ = "Q24174701"

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

qsFile = "/Wikicite/output.quickstatements"
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
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    try {
      results = rdf.processSPARQLXML(rawResults, sparql)
      missing = results.rowCount == 0
      if (!missing) {
        println "found ${results.rowCount} article(s)!"
        1.upto(results.rowCount) { artCounter ->
          artIRI = results.get(artCounter, "art")
          artQ = artIRI.substring(31)
          println "${artQ}\t" + results.get(artCounter, "artLabel")
          statement = "${artQ}\tP921\t${conceptQ}\n"
          ui.append(qsFile, statement)
        }
      } else {
        println "no hits"
      }
    } catch (Exception exception) {
      println "Error while retrieving this batch: " + e.message
    }
  } else {
    println "no online access"
  }
}
ui.open(qsFile)
