@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.0.9-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.8')

workspaceRoot = ".."
println "Initializing the managers..."

bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
excel = new net.bioclipse.managers.ExcelManager(workspaceRoot);

sheet = "/WormMetabolites/SI_Table_1.xlsx"

println "Loading $sheet ..."
data = excel.getSheet(sheet, 0)
println data.getRow(2)

for (row in 3..3740) {
  name = data.get(row,10)
  kegg = data.get(row,12)
  smiles = data.get(row,16)
  if (name && kegg && smiles) {
    println "${smiles}\t${kegg}\t${name}"
  }
}
