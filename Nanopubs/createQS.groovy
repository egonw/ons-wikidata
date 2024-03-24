// Copyright (C) 2021-2023  Egon Willighagen
// License: MIT
// If you use this software, please check the CITATION.cff file 
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.5.2')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.5.2')

import java.util.*

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

String.metaClass.encodeURL = {
   java.net.URLEncoder.encode(delegate, "UTF-8")
}

sparql = """
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>
prefix np: <http://www.nanopub.org/nschema#>
prefix npa: <http://purl.org/nanopub/admin/>
prefix npx: <http://purl.org/nanopub/x/>
prefix xsd: <http://www.w3.org/2001/XMLSchema#>
prefix dct: <http://purl.org/dc/terms/>

select ?np ?subj ?citationrel ?obj ?date where {
  graph npa:graph {
    ?np npa:hasValidSignatureForPublicKey ?pubkey .
    ?np dct:created ?date .
    ?np np:hasAssertion ?assertion .
    optional { ?np rdfs:label ?label . }
    filter not exists { ?npx npx:invalidates ?np ; npa:hasValidSignatureForPublicKey ?pubkey . }
    filter not exists { ?np npx:hasNanopubType npx:ExampleNanopub . }
  }
  graph ?assertion {
    ?subj ?citationrel ?obj .
    filter(regex(str(?citationrel), "^http://purl.org/spar/cito/.*\$"))
    filter(regex(str(?subj), "doi.org/10"))
    filter(regex(str(?obj), "doi.org/10"))
  }
}
"""

if (bioclipse.isOnline()) {
  rawResults = bioclipse.download(
    "https://query.np.trustyuri.net/repo/type/2c1cce3f3152738c1009d59251409392aaaa3b0324bcb5fdfb4b7b944b8f0c18?query=" + sparql.encodeURL(),
    "application/sparql-results+xml"
  )
  npResults = rdf.processSPARQLXML(rawResults.getBytes(), sparql)
}

// collect all DOIs
allDOIs = new HashSet<String>()
for (row in 1..npResults.getRowCount()) {
  allDOIs.add("10." + npResults.get(row, "obj").split("10\\.")[1])
  allDOIs.add("10." + npResults.get(row, "subj").split("10\\.")[1])
}
// get all Wikidata <> DOI pairs
values = "" // we also need a QID for the citing article
allDOIs.each { doi ->
  values += "\"${doi.toUpperCase()}\" "
}
sparql = "SELECT DISTINCT ?work ?doi WHERE { VALUES ?doi { ${values} } ?work wdt:P356 ?doi }"
if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
  results = rdf.processSPARQLXML(rawResults, sparql)
}
doiToWikidata = new HashMap<String,String>()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  doiToWikidata.put(rowVals[1], rowVals[0].replace("http://www.wikidata.org/entity/",""))
}

// DOIs that are not in Wikidata are not in the doiToWikidata map

for (row in 1..npResults.getRowCount()) {
  npid = npResults.get(row, "np")
  citingDOI = "10." + npResults.get(row, "subj").split("10\\.")[1].toUpperCase()
  intent = npResults.get(row, "citationrel").replace("http://purl.org/spar/cito/", "")
  citedDOI = "10." + npResults.get(row, "obj").split("10\\.")[1].toUpperCase()
  date = npResults.get(row, "date")
  println "# ${citingDOI} (${doiToWikidata.get(citingDOI)}) ${intent} ${citedDOI} (${doiToWikidata.get(citedDOI)})"
  sparql = """
SELECT DISTINCT ?citingArticle ?intention ?citedArticle ?np WHERE {
  ?citingArticle p:P2860 ?citationStatement ; wdt:P356 "${citingDOI}" .
  ?citationStatement ps:P2860 ?citedArticle .
  ?citedArticle wdt:P356 "${citedDOI}" .
  OPTIONAL {
    ?citationStatement pq:P3712 ?INTENTION .
    ?INTENTION wdt:P31 wd:Q96471816 ; wdt:P2888 ?intentionIRI .
    OPTIONAL { ?citationStatement prov:wasDerivedFrom / pr:P12545 ?np }
    BIND (substr(str(?intentionIRI),27) AS ?intention)
  }
}
"""
  if (bioclipse.isOnline()) {
    rawResults = bioclipse.sparqlRemote(
      "https://query.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
  }
  if (results.rowCount > 1) {
    printn "# multiple results found. not sure what to do"
  } else {
    println "# Wikidata results: ${results.getRow(1)}"
    wdIntent = results.get(1, "intention")
    if (wdIntent == null) {
      println "# no CiTO intent found in Wikidata"
    } else if (!intent.equals(wdIntent)) {
      println "# different CiTO intent found in Wikidata"
    } else {
      println "# matching CiTO intent found in Wikidata"
    }
  }
}
