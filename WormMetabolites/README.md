# Worm metabolites

The code in this folder takes the data from a supplementary file from
a paper by Witting and Hastings et al. reviewing several other papers reporting experimental evidence
of the occurance of metabolites in C. elegans
(doi:[10.3389/fmolb.2018.00096](https://tools.wmflabs.org/scholia/doi/10.3389/fmolb.2018.00096)).

Before we kick off, make sure to download the `SI_Table_1.xlsx` file.

First, we extract the PubMed identifiers, to check with
[SourceMD](https://tools.wmflabs.org/sourcemd/) if they are in Wikidata and add them if not:

```shell
groovy extractPubMedIdentifiers.groovy
```

This script expects a Bioclipse working space `/WormMetabolites/` where the supplementary
information and the Groovy script is stored, just like in this repository.

The second step is to extract the SMILES strings of the chemicals, along with one of the
external identifiers, and save these are input files for the import-into-Wikidata tool:

```shell
groovy extractChemicals.groovy
```
