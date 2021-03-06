@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.12')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.12')

workspaceRoot = ".."
excel = new net.bioclipse.managers.ExcelManager(workspaceRoot);
cdk   = new net.bioclipse.managers.CDKManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf   = new net.bioclipse.managers.RDFManager(workspaceRoot);

input = "/MeltingPoints/BradleyMeltingPointDataset.xlsx"
data = excel.getSheet(input, 0, true)

mps = new HashMap<String,String>();

// first collect all the InChIs
for (i in 1..data.rowCount) {
//for (i in 1..20) {
  smiles = data.get(i, "smiles").replace("\n","").replace("\r","").trim()
  donotuse = data.get(i, "donotuse")
  source = data.get(i, "source")
  if (smiles && !donotuse && source == "Alfa Aesar") {
    try {
      mol = cdk.fromSMILES(smiles)
      inchiObj = inchi.generate(mol)
      inchiShort = inchiObj.value.substring(6)
      key = inchiObj.key

      mps.put(key, data.get(i, "mpC"))
    } catch (Exception exception) {
      // ignore for now
    }
  }
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
  MINUS { ?cmp wdt:P31 wd:Q55662747 }
  MINUS { ?cmp wdt:P31 wd:Q59199015 }
  MINUS { ?cmp wdt:P2101 ?mp }
}
"""
results = rdf.sparqlRemote(
  "https://query.wikidata.org/sparql", sparql
)

// results at all?
if (results.rowCount == 0) {
  println "No new melting points"
  System.exit(0);
}

// create the QuickStatements
for (i in 1..results.rowCount) {
  inchikey = results.get(i, "inchikey")
  cmp = results.get(i, "cmp").substring(31)
  println "\t${cmp}\tP2101\t${mps.get(inchikey)}U25267\tS248\tQ69644056"
}
