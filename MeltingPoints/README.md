# Melting Points

This notebook is about getting melting points in Wikidata, as published in
https://figshare.com/articles/Jean_Claude_Bradley_Open_Melting_Point_Datset/1031637

First, download the Excel spreadsheet and safe it in this folder, with something
like:

```shell
wget -O BradleyMeltingPointDataset.xlsx https://ndownloader.figshare.com/files/1503990
```

QuickStatements can then be generated for compounds without missing boiling
points with:

```shell
groovy createQuickStatements.groovy > output.qs
```


