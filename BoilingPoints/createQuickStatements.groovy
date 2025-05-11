@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='1.0.5')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='1.0.5')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='1.0.5')

workspaceRoot = ".."
cdk   = new net.bioclipse.managers.CDKManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf   = new net.bioclipse.managers.RDFManager(workspaceRoot);

input = new File("haloalkane_smiles.csv")
rows = input.readLines().tail()*.split(',')
//println rows

mps = new HashMap<String,String>();

println "rows: " + rows.size()
bps = rows*.getAt(1)
smiles = rows*.getAt(6)
//println "bps: " + bps
rows.collect {
  mol = cdk.fromSMILES(it[6])
  inchiObj = inchi.generate(mol)
  key = inchiObj.key
  mps.put(key, it[1])
}

// query Wikidata for the detected InChIKeys
sparql = """
PREFIX wd: <http://www.wikidata.org/entity/>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT DISTINCT ?cmp ?inchikey WHERE {
  VALUES ?inchikey {
"""
for (inchikey in mps.keySet()) {
  sparql += """    \"${inchikey}\"
"""
}
sparql += """  }
  ?cmp wdt:P235 ?inchikey .
  MINUS { ?cmp wdt:P2102 ?bp }
}
"""
results = rdf.sparqlRemote(
  "https://query.wikidata.org/sparql", sparql
)

// results at all?
if (results.rowCount == 0) {
  println "No new boiling points"
  System.exit(0);
}

// create the QuickStatements
for (i in 1..results.rowCount) {
  inchikey = results.get(i, "inchikey")
  cmp = results.get(i, "cmp").substring(3)
  println "\t${cmp}\tP2102\t${mps.get(inchikey)}U25267\tP2077\t101.325U21064807\tS248\tQ51983889\ts854\t\"https://gist.github.com/dehaenw/9b43e42e17a388a4f66670d5f89e3378#file-haloalkane_smiles-csv\""
}
