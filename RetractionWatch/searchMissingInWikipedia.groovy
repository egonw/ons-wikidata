// Copyright (C) 2023-2025  Egon Willighagen
// License: MIT
// If you use this software, please check the CITATION.cff file 

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.6-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.6-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='net.bioclipse.managers.wikidata', version='1.0.6-SNAPSHOT')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
wikidata = new net.bioclipse.managers.WikidataManager(workspaceRoot);

missingDOIs = new File("missing_dois.txt")

wikipediaLang = "en"

missingDOIs.eachLine { line ->
  doi = line.trim()
  println "Searching ${doi} in Wikipedia..."
  
  sparql = """
    SELECT ?titleUrl {
      SERVICE wikibase:mwapi {
        bd:serviceParam wikibase:endpoint "${wikipediaLang}.wikipedia.org" ;
                        wikibase:api "Generator" ;
                        mwapi:generator "search" ;
                        mwapi:gsrsearch '"${doi}"' ;
                        mwapi:gsrlimit "200" .
        ?title_ wikibase:apiOutput mwapi:title .
        ?item wikibase:apiOutputItem mwapi:item .
      }
      BIND(URI(CONCAT("https://${wikipediaLang}.wikipedia.org/wiki/", ENCODE_FOR_URI(REPLACE(?title_, " ", "_")))) AS ?titleUrl)
    }     
  """
  if (bioclipse.isOnline()) {
    rawResults = bioclipse.sparqlRemote(
      "https://query-scholarly.wikidata.org/sparql", sparql
    )
    results = rdf.processSPARQLXML(rawResults, sparql)
  }
  for (i=1;i<=results.rowCount;i++) {
    rowVals = results.getRow(i)
    wpPage = rowVals[0]
    println "  ... found in this Wikipedia page: ${wpPage}"
  }
}
