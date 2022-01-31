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

input = "/OpenAlex/data/wikidata_concepts"
mappingsFile = input + "_mappings.txt"
ui.renewFile(mappingsFile)

property = "P10283" // OpenAlex ID
source = "Q107507571"
referenceURL = "https://docs.openalex.org/download-snapshot/snapshot-data-format"

batchSize = 500
batchCounter = 0
mappingContent = ""
ui.renewFile(mappingsFile)
String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

data = new File(bioclipse.fullPath(input));
data.eachLine { line, number ->
  fields = line.split("\t")
  if (fields.length < 2) return;
  openalexID = fields[0]
  wdid = fields[1].substring(30)
  batchCounter++
  mappingContent += "${wdid}\t${property}\t\"${openalexID}\"" +
    ((source == null) ? "" : "\tS248\t${source}") +
    ((referenceURL == null) ? "" : "\tS854\t\"${referenceURL}\"") +
    "\tS813\t+${date}T00:00:00Z/11\n";
  if (batchCounter >= batchSize) {
    ui.append(mappingsFile, mappingContent)
    batchCounter = 0
    mappingContent = ""
    missingContent = ""
    print "."
  }
}
ui.append(mappingsFile, mappingContent)
println "\n"
