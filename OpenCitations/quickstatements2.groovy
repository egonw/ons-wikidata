// Copyright (C) 2021  Egon Willighagen
// License: MIT

// Usage:
//
//   Give it a DOI and it will fetch citations to that article from OpenCitations to other articles
//   and match this up with Wikidata for "cites" statements.
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/
//

// Bacting config
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.22')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.22')
workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

import java.text.SimpleDateFormat;
import java.util.Date;

def cli = new CliBuilder(usage: 'createWDitemsFromSMILES.groovy')
cli.h(longOpt: 'help', 'print this message')
cli.d(longOpt: 'doi', args:1, argName:'doi', 'DOI of the cited article')
def options = cli.parse(args)

if (options.help) {
  cli.usage()
  System.exit(0)
}

if (options.doi) {
  doi = options.d
} else {
  cli.usage()
  System.exit(0)
}

cociURL = new URL("https://opencitations.net/index/coci/api/v1/references/${doi}")
println "Fetching ${doi} from ${cociURL}..."

data = new groovy.json.JsonSlurper().parseText(cociURL.text)

citedDOIs = new java.util.HashSet()

data.each { citation ->
  citedDOIs.add(citation.cited)
}

println "Found DOI: ${citedDOIs.size()}"

values = "\"${doi}\" \n" // we also need a QID for the citing article
citedDOIs.each { doi ->
  values += "\"${doi.toUpperCase()}\" \n"
}

// find QIDs for articles citing the focus article, but not if they already cite it in Wikidata (MINUS clause)
sparql = "SELECT DISTINCT ?work ?doi WHERE {\n VALUES ?doi {\n ${values} }\n ?work wdt:P356 ?doi . MINUS { ?citingWork wdt:P356 \"${doi}\" ; wdt:P2860 ?work }\n}"

if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
  results = rdf.processSPARQLXML(rawResults, sparql)
}

// make a map
map = new HashMap<String,String>()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  map.put(rowVals[1], rowVals[0].replace("http://www.wikidata.org/entity/",""))
}

citingQID = map.get(doi)

String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

println "qid,P2860,S248,s854,s813"
map.each { citedDOI, qid ->
  if (citedDOI != doi) println "${citingQID},${qid},Q107507940,\"\"\"${cociURL}\"\"\",+${date}T00:00:00Z/11"
}

