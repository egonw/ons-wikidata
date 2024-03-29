// Copyright (C) 2018-2023  Egon Willighagen
// License: MIT

// Usage:
//
//   change the values of the following property variables. 
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/
//
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.3.1')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.3.1')

import java.text.SimpleDateFormat;
import java.util.Date;

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

property = "P6689" // Massbank Accession ID
source = "Q113696454" // doi: 10.5281/zenodo.7046820

input = "/ExtIdentifiers/220931_massbank_records_v2022.06_to_wikidata.csv"
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

batchSize = 500
batchCounter = 0
mappingContent = ""
missingContent = ""
print "Saved a batch"
renewFile(mappingsFile)
renewFile(missingCompoundFile)
new File(bioclipse.fullPath(input)).eachLine{ line ->
  fields = (splitString != null) ? line.split(splitString) : line.split()
  extid = fields[idIndex]
  inchikey = fields[inchikeyIndex]
  batchCounter++
  if (map.containsKey(inchikey)) {
    wdid = map.get(inchikey)
    if (!ignores.contains(wdid)) {
      if (source) {
        mappingContent += "${wdid}\t${property}\t\"${extid}\"\tS248\t${source}\tS813\t+${date}T00:00:00Z/11\tS235\t\"${inchikey}\"\tS887\tQ100452164\n"
      } else {
        mappingContent += "${wdid}\t${property}\t\"${extid}\"\tS813\t+${date}T00:00:00Z/11\tS235\t\"${inchikey}\"\tS887\tQ100452164\n"
      }
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
