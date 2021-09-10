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

cociURL = new URL("https://opencitations.net/index/coci/api/v1/citations/${doi}")
println "Fetching ${doi} from ${cociURL}..."

data = new groovy.json.JsonSlurper().parseText(cociURL.text)

citingDOIs = new java.util.HashSet()

data.each { citation ->
  citingDOIs.add(citation.citing)
}

println "Found DOI: ${citingDOIs.size()}"

values = ""
citingDOIs.each { doi ->
  values += "\"${doi.toUpperCase()}\" \n"
}

sparql = "SELECT ?work ?doi WHERE {\n VALUES ?doi {\n ${values} }\n ?work wdt:P356 ?doi\n}"

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

println "" + map
