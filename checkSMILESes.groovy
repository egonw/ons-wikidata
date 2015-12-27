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
ui.open(outFilename)
