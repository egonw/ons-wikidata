# Getting the data from CrossRef

```
wget -O retractions.csv https://api.labs.crossref.org/data/retractionwatch?egonw@your.email.domain
```

# Create quickstatements

```
groovy quickstatements.groovy | tee output.qs
```

# Update citations from/to retracted articles

```
csvtool col 15 retractions.csv | head -25 | grep -v DOI > ../OpenCitations/retracted_dois_latest25.txt
```
