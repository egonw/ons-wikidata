@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.4')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='1.0.4')

import java.text.SimpleDateFormat;
import java.util.Date;

workspaceRoot = ".."
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
ui = new net.bioclipse.managers.UIManager(workspaceRoot);

String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

restAPI = "https://www.lipidmaps.org/rest/compound/lm_id/LM/all/download"
propID = "P2063"
property = propID

lipidmapstxt = "/LipidMaps/lipidmaps.txt"
if (!ui.fileExists(lipidmapstxt)) {
  allData = bioclipse.downloadAsFile(restAPI, lipidmapstxt)
}

// run:
// curl -s https://qlever.cs.uni-freiburg.de/api/wikidata -H "Accept: text/tab-separated-values" -H "Content-type: application/sparql-query" --data "PREFIX wdt: <http://www.wikidata.org/prop/direct/> SELECT DISTINCT (substr(str(?compound),32) as ?wd) ?key ?value WHERE { ?compound wdt:P235 ?key . OPTIONAL { ?compound wdt:P2063 ?value } }" > wikidata.cached

// make a map InChIKey -> Wikidata QID
map = new HashMap()
new File(bioclipse.fullPath("/LipidMaps/wikidata.cached")).eachLine{ line ->
  if (line.startsWith("?wd")) {}
  else {
    fields = line.split("\t")
    map.put(fields[1].replaceAll("\"",""), fields[0].replaceAll("\"",""))
  }
}

allpmidsSparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT (substr(str(?compound),32) as ?wd) ?key ?lmid WHERE {
  ?compound wdt:${propID} ?lmid .
}
"""
println allpmidsSparql
if (bioclipse.isOnline()) {
  allpmidsResults = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", allpmidsSparql
  )
}

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

mappingsFile = "/LipidMaps/mappings.txt"
missingCompoundFile = "/LipidMaps/missing.txt"

// ignore certain Wikidata items, where I don't want the LipidMaps ID added
ignores = new java.util.HashSet();
// ignores.add("Q37111097")

// make a map LMID -> Wikidata QID
lmidMap = new HashMap()
for (i=1;i<=allpmidsResults.rowCount;i++) {
  rowVals = allpmidsResults.getRow(i)
  lmidMap.put(rowVals[2], rowVals[0])
}

batchSize = 50
batchCounter = 0
mappingContent = ""
missingContent = ""
print "Saved a batch: "
renewFile(mappingsFile)
renewFile(missingCompoundFile)
new File(bioclipse.fullPath("/LipidMaps/lipidmaps.txt")).eachLine{ line ->
  fields = line.split("\t")
  if (fields.length > 14) {
    lmid = fields[1]
    inchikey = fields[14]
    if (inchikey != null && inchikey.length() > 10) {
      batchCounter++
      if (lmidMap.containsKey(lmid)) {
        // println "LMID ${lmid} already associated with Wikidata ${lmidMap.get(lmid)}" 
      } else if (map.containsKey(inchikey)) {
        wdid = map.get(inchikey)
        if (!ignores.contains(wdid)) {
          mappingContent += "${wdid}\t${propID}\t\"${lmid}\"\tS248\tQ20968889\tS854\t\"http://www.lipidmaps.org/rest/compound/lm_id/LM/all/download\"\tS813\t+${date}T00:00:00Z/11\tS235\t\"${inchikey}\"\tS887\tQ100452164\n"
        }  
      } else {
        missingContent += "${inchikey}\t${lmid}\n"
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
