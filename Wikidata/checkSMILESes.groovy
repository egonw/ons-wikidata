@Grab(group='net.bioclipse.bacting', module='managers-cdk', version='0.0.4-SNAPSHOT')
@Grab(group='net.bioclipse.bacting', module='managers-ui', version='0.0.4-SNAPSHOT')
@Grab(group='net.bioclipse.bacting', module='managers-rdf', version='0.0.4-SNAPSHOT')

workspaceRoot = ".."
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);

identifier = "P233" // SMILES
type = "smiles"

sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT ?compound ?smiles WHERE {
  ?compound wdt:P233 ?smiles .
}
"""
mappings = rdf.sparqlRemote("https://query.wikidata.org/sparql", sparql)

outFilename = "/Wikidata/badWikidataSMILES.txt"
fileContent = ""
for (i=1; i<=mappings.rowCount; i++) {
  try {
    wdID = mappings.get(i, "compound")
    smiles = mappings.get(i, "smiles")
    mol = cdk.fromSMILES(smiles)
  } catch (Exception exception) {
    fileContent += wdID + "," + smiles + ": " + exception.message + "\n"
  }
}
ui.append(outFilename, fileContent)
// ui.open(outFilename)
