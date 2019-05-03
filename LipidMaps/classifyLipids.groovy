@Grab(group='net.bioclipse.bacting', module='managers-rdf', version='0.0.3')
@Grab(group='net.bioclipse.bacting', module='managers-ui', version='0.0.3')

workspaceRoot = ".."
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

lmClassQ = "Q4198767"
lmClassID = "LMFA0311"
propID = "P2063"

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX wd: <http://www.wikidata.org/entity/>
SELECT (substr(str(?compound),32) as ?wd) ?key ?lmid WHERE {
  ?compound wdt:${propID} ?lmid .
  MINUS { ?compound wdt:P31 wd:${lmClassQ} . }
  FILTER ( STRSTARTS(STR(?lmid), "${lmClassID}") )
  FILTER ( ?compound != wd:${lmClassQ} )
}
"""

if (bioclipse.isOnline()) {
  results = rdf.sparqlRemote(
    "https://query.wikidata.org/sparql", sparql
  )
}

for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  wdid = rowVals[0]
  println "${wdid}\tP31\t${lmClassQ}"
}
