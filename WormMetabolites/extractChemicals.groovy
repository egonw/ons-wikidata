@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.1.2')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.1.2')

workspaceRoot = ".."
println "Initializing the managers..."

bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
excel = new net.bioclipse.managers.ExcelManager(workspaceRoot);
ui = new net.bioclipse.managers.UIManager(workspaceRoot);

sheet = "/WormMetabolites/SI_Table_1.xlsx"

println "Loading $sheet ..."
data = excel.getSheet(sheet, 0)
println data.getRow(2)

keggOutput = "/WormMetabolites/kegg.smi"
ui.renewFile(keggOutput)

for (row in 3..3740) {
  name = data.get(row,10)
  kegg = data.get(row,12)
  smiles = data.get(row,16)
  if (name && smiles) {
    if (kegg) {
      ui.append(keggOutput, "${smiles}\t${kegg}\t${name}\n")
    }
  }
}
