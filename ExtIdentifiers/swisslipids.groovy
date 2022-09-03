// Copyright (C) 2018-2022  Egon Willighagen
// License: MIT

// Usage:
//
//   change the values of the following property variables. 
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/
//
//   Use, see: https://github.com/elixir-europe/biohackathon-projects-2021/blob/main/projects/6/SwissLipid-in-Wikidata.md

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.45')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.45')

import java.text.SimpleDateFormat;
import java.util.Date;

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);


property = "P8691" // SwissLipids identifier
source = "Q41165322"
referenceURL = "https://www.swisslipids.org/#/downloads"

input = "/ExtIdentifiers/swisslipids_ids.tsv"
splitString = ","
idIndex = 0
inchikeyIndex = 1

// ignore certain Wikidata items, where I don't want the values added
ignores = new java.util.HashSet();

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?wd ?key ?value WHERE {
  SERVICE <https://query.wikidata.org/sparql> {
    SELECT (substr(str(?compound),32) as ?wd) ?key ?value WHERE {
      ?compound wdt:P235 ?key .
      OPTIONAL { ?compound wdt:${property} ?value . }
    }
  }
}
"""
println sparql

if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://beta.sparql.swisslipids.org/sparql?format=xml", sparql
  )
  results = rdf.processSPARQLXML(rawResults, sparql)
}

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

mappingsFile = input + "_mappings.txt"
renewFile(mappingsFile)
missingCompoundFile = input + "_missing.txt"
renewFile(missingCompoundFile)

// make a map
map = new HashMap()
existingMappings = new HashSet()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  if (rowVals[2] == "") {
    map.put(rowVals[1], rowVals[0])
  } else {
    existingMappings.add(rowVals[1])
  }
}
println "# Found InChIKeys<>Wikidata mappings with ${property}: " + existingMappings.size()
println "# Found InChIKeys<>Wikidata mappings without ${property}: " + map.size()

batchSize = 500
batchCounter = 0
mappingContent = ""
missingContent = ""
print "Saved a batch"
renewFile(mappingsFile)
renewFile(missingCompoundFile)
String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
new File(bioclipse.fullPath(input)).eachLine{ line ->
  fields = (splitString != null) ? line.split(splitString) : line.split()
  if (fields.length < 2) {
    return
  }
  extid = fields[idIndex]
  inchikey = fields[inchikeyIndex]
  batchCounter++
  if (map.containsKey(inchikey)) {
    wdid = map.get(inchikey)
    if (!ignores.contains(wdid)) {
      mappingContent += "${wdid}\t${property}\t\"${extid}\"" +
        ((source == null) ? "" : "\tS248\t${source}") +
        ((referenceURL == null) ? "" : "\tS854\t\"${referenceURL}\"") +
        "\tS813\t+${date}T00:00:00Z/11\n";
    }
  } else if (!existingMappings.contains(inchikey)) {
    missingContent += "${inchikey}\n"
  }
  if (batchCounter >= batchSize) {
    ui.append(mappingsFile, mappingContent)
    ui.append(missingCompoundFile, missingContent)
    batchCounter = 0
    mappingContent = ""
    missingContent = ""
    print "."
  }
}
ui.append(mappingsFile, mappingContent)
ui.append(missingCompoundFile, missingContent)
println "\n"
