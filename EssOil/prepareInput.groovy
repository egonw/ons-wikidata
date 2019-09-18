@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.8')
@Grab(group='io.github.egonw.bacting', module='managers-pubchem', version='0.0.8')

workspaceRoot = ".."
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
pubchem = new net.bioclipse.managers.PubChemManager(workspaceRoot);
ui = new net.bioclipse.managers.UIManager(workspaceRoot);

input = "/EssOil/notFoundWikidata.csv"

def renewFile(file) {
  if (ui.fileExists(file)) ui.remove(file)
  ui.newFile(file)
  return file
}

outFile = "/EssOil/input.smi"
renewFile(outFile)

new File(bioclipse.fullPath(input)).eachLine{ line ->
  fields = line.split(",")
  if (fields.length > 4) return

  intid = fields[0].trim()
  extid = fields[3].trim()
  name = fields[2].trim()
  if (extid == "CID") return

  smiles = bioclipse.download("https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/cid/${extid}/property/IsomericSMILES/TXT")
  smiles = smiles.trim()
  sleep(250) // keep PubChem happy

  if (smiles) {
    println "$intid -> PubChem CID $extid -> $name -> $smiles"
    ui.append(outFile, "$smiles\t$extid\t$name\n")
  } else {
    println "$intid -> PubChem CID $extid -> $name -> SMILES NOT FOUND IN PUBCHEM"
  }
}
