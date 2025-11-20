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
//   Use, see: https://github.com/elixir-europe/biohackathon-projects-2021/blob/main/projects/6/SwissLipid-in-Wikidata.md

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.0')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.0')

import java.text.SimpleDateFormat;
import java.util.Date;

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);


property = "P36" // AOP Stressor identifier

input = "/VHP4Safety/aop_stressors.tsv"
splitString = "\t"
idIndex = 1
inchikeyIndex = 0

// ignore certain Wikidata items, where I don't want the values added
ignores = new java.util.HashSet();

sparql = """
PREFIX wd: <https://compoundcloud.wikibase.cloud/entity/>
PREFIX wdt: <https://compoundcloud.wikibase.cloud/prop/direct/>

SELECT (substr(str(?compound),45) as ?wd) ?key ?value WHERE {
  ?compound wdt:P10 ?key .
  OPTIONAL { ?compound wdt:${property} ?value . }
}
"""
println sparql

if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://compoundcloud.wikibase.cloud/query/sparql", sparql
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
println "# Found InChIKeys<>VHP Wiki mappings with ${property}: " + existingMappings.size()
println "# Found InChIKeys<>VHP Wiki mappings without ${property}: " + map.size()

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
