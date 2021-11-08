
# Proteins

```shell
curl -H "Accept: text/csv" --data-urlencode query@proteins.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o proteins.csv
cat proteins.csv | sed 's/^/https:\/\/scholia.toolforge.org\/protein\//' > proteins_scholia.txt
```

# Genes

```shell
curl -H "Accept: text/csv" --data-urlencode query@genes.rq -G https://query.wikidata.org/bigdata/namespace/wdq/sparql -o genes.csv
cat genes.csv | sed 's/^/https:\/\/scholia.toolforge.org\/gene\//' > genes_scholia.txt
```

