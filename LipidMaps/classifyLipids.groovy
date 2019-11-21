@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.10')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.10')

import java.text.SimpleDateFormat;
import java.util.Date;

workspaceRoot = ".."
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

lmClassQ = "Q32528"
lmClassID = "LMPR010206"
String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

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

println "qid,P31,S248,s813"
for (i=1;i<=results.rowCount;i++) {
  rowVals = results.getRow(i)
  wdid = rowVals[0]
  println "${wdid},${lmClassQ},Q20968889,+${date}T00:00:00Z/11"
}
