// Copyright (C) 2022  Egon Willighagen
// License: MIT

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.31')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.31')

import java.text.SimpleDateFormat;
import java.util.Date;

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

property = "P10283" // OpenAlex ID
rorProp  = "P7363" // SSH-L
source = "Q107507571"
referenceURL = "https://docs.openalex.org/download-snapshot/snapshot-data-format"

input = "/OpenAlex/data/issn_l"
mappingsFile = input + "_mappings.txt"
ui.renewFile(mappingsFile)
missingCompoundFile = input + "_missing.txt"
ui.renewFile(missingCompoundFile)

rorData = new File(bioclipse.fullPath(input));
rorValues = "  VALUES ?issn {\n"
rorData.eachLine { line, number ->
  fields = line.split("\t")
  rorValues += "    \"${fields[1]}\"\n"
}
rorValues += "  }\n";

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?wd ?issn ?openalex WHERE {
  SERVICE <https://query.wikidata.org/sparql> {
    SELECT (substr(str(?wikidata),32) as ?wd) ?issn ?openalex WHERE {
      ${rorValues}
      ?wikidata wdt:${rorProp} ?issn .
      OPTIONAL { ?wikidata wdt:${property} ?openalex }
    }
  }
}
"""

if (bioclipse.isOnline()) {
  rawResults = bioclipse.sparqlRemote(
    "https://beta.sparql.swisslipids.org/sparql?format=xml", sparql // HT to Jerven and SIB
  )
  results = rdf.processSPARQLXML(rawResults, sparql)
}

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
println "# Found OpenAlex<>Wikidata mappings with ${property}: " + existingMappings.size()
println "# Found OpenAlex<>Wikidata mappings without ${property}: " + map.size()

batchSize = 500
batchCounter = 0
mappingContent = ""
missingContent = ""
ui.renewFile(mappingsFile)
ui.renewFile(missingCompoundFile)
String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

rorData = new File(bioclipse.fullPath(input));
rorData.eachLine { line, number ->
  fields = line.split("\t")
  if (fields.length < 2) return;
  openalexID = fields[0]
  rorID = fields[1]
  batchCounter++
  if (map.containsKey(rorID)) {
    wdid = map.get(rorID)
    mappingContent += "${wdid}\t${property}\t\"${openalexID}\"" +
      "\tS887\tQ110768944" +
      ((source == null) ? "" : "\tS248\t${source}") +
      ((referenceURL == null) ? "" : "\tS854\t\"${referenceURL}\"") +
      "\tS813\t+${date}T00:00:00Z/11\n";
  } else if (!existingMappings.contains(rorID)) {
    missingContent += "${rorID}\n"
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
