@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.0.9')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.0.9') 
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.0.9')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.0.9')

workspaceRoot = ".."
excel = new net.bioclipse.managers.ExcelManager(workspaceRoot);
cdk   = new net.bioclipse.managers.CDKManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);
rdf   = new net.bioclipse.managers.RDFManager(workspaceRoot);

input = "/MeltingPoints/BradleyMeltingPointDataset.xlsx"
data = excel.getSheet(input, 0, true)

csids = new HashMap<String,Integer>();
names = new HashMap<String,String>();
smileses = new HashMap<String,String>();

// first collect all the InChIs
for (i in 1..data.rowCount) {
//for (i in 1..20) {
  smiles = data.get(i, "smiles").replace("\n","").replace("\r","").trim()
  donotuse = data.get(i, "donotuse")
  if (smiles && !donotuse) {
    try {
      mol = cdk.fromSMILES(smiles)
      inchiObj = inchi.generate(mol)
      inchiShort = inchiObj.value.substring(6)
      key = inchiObj.key
      name = data.get(i, "name")
      csid = Double.parseDouble(data.get(i, "csid")).intValue()

      names.put(key, name)
      csids.put(key, csid)
      smileses.put(key, smiles)
    } catch (Exception exception) {
      // ignore for now
    }
  }
}

// query Wikidata for the detected InChIKeys
sparql = """
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
SELECT DISTINCT ?cmp ?inchikey WHERE {
  VALUES ?inchikey {
"""
for (inchikey in names.keySet()) {
  sparql += """    \"${inchikey}\"
"""
}
sparql += """  }
  ?cmp wdt:P235 ?inchikey .
}
"""
results = rdf.sparqlRemote(
  "https://query.wikidata.org/sparql", sparql
)

// track which InChIKeys are in Wikidata
foundInWikidata = new HashSet<String>();
for (i in 1..results.rowCount) {
  inchikey = results.get(i, "inchikey")
  foundInWikidata.add(inchikey)
}

// see which InChIKeys are not in Wikidata and output
for (inchikey in names.keySet()) {
  if (!foundInWikidata.contains(inchikey)) {
    smi = smileses.get(inchikey)
    csid = csids.get(inchikey)
    name = names.get(inchikey)
    println "${smi}\t${csid}\t${name}"
  }
}
