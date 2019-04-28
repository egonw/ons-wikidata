// Copyright (C) 2018-2019  Egon Willighagen
// License: MIT

// Usage:
//
//   change the values of the following property variables. 
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/

@Grab(group='net.bioclipse.bacting', module='managers-ui', version='0.0.3')
@Grab(group='net.bioclipse.bacting', module='managers-rdf', version='0.0.3')
workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);


property = "P6689" // Massbank Accession ID
source = null

input = "/ExtIdentifiers/Accession_to_InChi-Key.txt"
splitString = null
idIndex = 0
inchikeyIndex = 1

// ignore certain Wikidata items, where I don't want the values added
ignores = new java.util.HashSet();

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT (substr(str(?compound),32) as ?wd) ?key ?value WHERE {
  ?compound wdt:P235 ?key .
  MINUS { ?compound wdt:${property} ?value . }
}
"""

if (bioclipse.isOnline()) {
  results = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
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
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  map.put(rowVals[1], rowVals[0])  
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
        mappingContent += "${wdid}\t${property}\t\"${extid}\"\tS248\t${source}\n"
      } else {
        mappingContent += "${wdid}\t${property}\t\"${extid}\"\n"
      }
    }
  } else {
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
