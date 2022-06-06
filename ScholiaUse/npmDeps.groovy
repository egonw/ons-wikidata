// Copyright (c) 2022  Egon Willighagen <egon.willighagen@gmail.com>
//
// GPL v3

@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.37')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.37')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.jsoup', version='0.0.37')

import groovy.json.JsonSlurper

bioclipse = new net.bioclipse.managers.BioclipseManager(".")
rdf = new net.bioclipse.managers.RDFManager(".")

def parser = new JsonSlurper()

// single package
npmPkgs = [ "citation-js" ]

// OR all packages in Wikidata

sparql = """
PREFIX wd: <http://www.wikidata.org/entity/>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT DISTINCT ?npm WHERE {
  ?qid wdt:P8262 ?npm .
}"""
results = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)
npmPkgs = results.getColumn("npm")

println "qid,P1547"

npmPkgs.each { npm ->
  // STEP 0: figure out what package this Wikidata item is for

  sparql = """
  PREFIX wd: <http://www.wikidata.org/entity/>
  PREFIX wdt: <http://www.wikidata.org/prop/direct/>
  SELECT ?qid WHERE {
    ?qid wdt:P8262 \"$npm\" .
  }"""

  results = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)
  qid = results.get(1, "qid")
  println("# package: $npm ($qid)")

  jsonText = bioclipse.download("https://registry.npmjs.com/$npm")
  //println(jsonText)

  def jsonResp = parser.parseText(jsonText)
  version = jsonResp."dist-tags".latest

  // Step 1: get the DEPENDENCIES
  deps = jsonResp.versions."$version".dependencies
  if (deps) {
    depListVals = ""
    deps.each { dep, version ->
      // println("dependency: $dep")
      if (!version.contains("/")) depListVals += "\"$dep\" ";
    }

    sparql = """
    PREFIX wd: <http://www.wikidata.org/entity/>
    PREFIX wdt: <http://www.wikidata.org/prop/direct/>
    SELECT ?package ?npm WHERE {
      VALUES ?npm { $depListVals}
      ?package wdt:P8262 ?npm .
      MINUS { $qid wdt:P1547 ?package }
    }"""
    results = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

    for (i=1;i<=results.rowCount;i++) {
      rowVals = results.getRow(i)
      qid = qid.replace("wd:","")
      pkg = rowVals[0].replace("wd:","")
      println("$qid,$pkg")
    }
  }
}
