@Grab(group='net.bioclipse.managers', module='bioclipse-cdk', version='0.0.3-SNAPSHOT')
@Grab(group='net.bioclipse.managers', module='bioclipse-rdf', version='0.0.3-SNAPSHOT')
@Grab(group='net.bioclipse.managers', module='bioclipse-ui', version='0.0.3-SNAPSHOT')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

restAPI = "http://www.lipidmaps.org/rest/compound/lm_id/LMSP02/all/download"
lmClass = "ceramide"
lmClassQ = "Q424213"
propID = "P2063"

allData = bioclipse.downloadAsFile(
  restAPI, "/LipidMaps/${lmClass}.txt"
)


sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX wd: <http://www.wikidata.org/entity/>
SELECT (substr(str(?compound),32) as ?wd) ?key ?lmid WHERE {
  ?compound wdt:P235 ?key ; wdt:${propID} ?lmid .
  MINUS { ?compound wdt:P31 wd:Q55282178 . }
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

mappingsFile = "/LipidMaps/${lmClass}Mappings.txt"
missingCompoundFile = "/LipidMaps/${lmClass}Missing.txt"

// ignore certain Wikidata items, where I don't want the LIPID MAPS ID added
ignores = new java.util.HashSet();
// ignores.add("Q37111097")

// make a map
map = new HashMap()
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  map.put(rowVals[1], rowVals[0])  
}

inchikey = ""
batchSize = 500
batchCounter = 0
mappingContent = ""
missingContent = ""
print "Saved a batch"
renewFile(mappingsFile)
renewFile(missingCompoundFile)
new File(bioclipse.fullPath("/LipidMaps/${lmClass}.txt")).eachLine{ line ->
  fields = line.split("\t")
  if (fields.length > 15) {
    lmid = fields[1]
    inchikey = fields[15]
    if (inchikey != null && inchikey.length() > 10) {
      batchCounter++
      if (map.containsKey(inchikey)) {
        wdid = map.get(inchikey)
        if (!ignores.contains(wdid)) {
          mappingContent += "${wdid}\tP31\t${lmClassQ}\tS143\tQ20968889\tS854\t\"${restAPI}\"\tS813\t+2018-08-16T00:00:00Z/11\n"
        }  
      } else {
        missingContent += "${inchikey}\n"
      }
    }
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
