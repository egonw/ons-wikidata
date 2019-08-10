@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.8')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.8')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.8')

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
unitFilename = "/Wikidata/badWikidataSMILES.xml"

fileContent = ""
unitContent = ""
unitContent = "<testsuite tests=\"1\">\n"
unitContent += "  <testcase classname=\"SMILESTests\" name=\"Parsable\">\n"
for (i=1; i<=mappings.rowCount; i++) {
  try {
    wdID = mappings.get(i, "compound")
    smiles = mappings.get(i, "smiles")
    mol = cdk.fromSMILES(smiles)
  } catch (Exception exception) {
    fileContent += wdID + "," + smiles + ": " + exception.message + "\n"
  }
}
if (fileContent.length() > 0) {
  unitContent += "<error message=\"Unparsable SMILES Found\" " +
    "type=\"org.openscience.cdk.exception.InvalidSmilesException\">\n" +
    fileContent + "\n</error>\n"
}
unitContent += "  </testcase>\n"
unitContent += "</testsuite>\n"
ui.append(outFilename, fileContent)
ui.append(unitFilename, unitContent)
// ui.open(outFilename)
