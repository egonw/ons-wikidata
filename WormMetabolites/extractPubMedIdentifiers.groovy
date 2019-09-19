@Grab(group='io.github.egonw.bacting', module='managers-excel', version='0.0.9-SNAPSHOT')
@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.0.8')

workspaceRoot = ".."
println "Initializing the managers..."

bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
excel = new net.bioclipse.managers.ExcelManager(workspaceRoot);

sheet = "/WormMetabolites/SI_Table_1.xlsx"

println "Loading $sheet ..."
data = excel.getSheet(sheet, 0)

pmids = data.getColumn(4)
for (pmid in pmids) {
  if (pmid &&
     !pmid.contains("PMID") &&
     !pmid.contains("No") &&
     !pmid.contains("doi")) {
    println "" + Double.parseDouble(pmid).intValue()
  }
}
