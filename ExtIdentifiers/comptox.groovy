// Copyright (C) 2018-2023  Egon Willighagen
// License: MIT

// Usage:
//
//   change the values of the following property variables. 
//
//   The output of this script is a set of QuickStatements that can be uploaded here:
//
//     https://tools.wmflabs.org/quickstatements/

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.3.2')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.3.2')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);


property = "P3117" // DSS CompTox ID
source = "Q28061352" // Figshare data dump entity

input = "/ExtIdentifiers/dsstox_20160701.tsv"
idIndex = 0
inchikeyIndex = 2

// ignore certain Wikidata items, where I don't want the values added
ignores = new java.util.HashSet();

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT (substr(str(?compound),32) as ?wd) ?key ?value WITH {
  SELECT ?compound ?key WHERE {
    ?compound wdt:P235 ?key .
  }
} AS %INCHIKEYS {
  INCLUDE %INCHIKEYS
  OPTIONAL { ?compound wdt:${property} ?value . }
}
"""

if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
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
  fields = line.split("\t")
  extid = fields[idIndex]
  inchikey = fields[inchikeyIndex]
  batchCounter++
  if (map.containsKey(inchikey)) {
    wdid = map.get(inchikey)
    if (!ignores.contains(wdid)) {
      if (source) {
        mappingContent += "${wdid}\tP3117\t\"${extid}\"\tS248\t${source}\n"
      } else {
        mappingContent += "${wdid}\tP3117\t\"${extid}\"\n"
      }
    }
  } else if (!existingMappings.contains(inchikey)) {
    missingContent += "${inchikey}\t${extid}\n"
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
