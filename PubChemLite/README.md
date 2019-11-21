# PubChemLite

Get the PubChemLite data from https://zenodo.org/record/3548654

```shell
grep "\-N" tier1_data_complete_filtered.out > tier1_data_complete_filtered_neutral.out 
grep -v "UHFFFAOYSA" tier1_data_complete_filtered_neutral.out > tier1_data_complete_filtered_neutral_chiral.out 
awk 'BEGIN { FS="\t"; OFS="\t"; ORS="\r\n" } { print $9, $1, $13 }' ./tier1_data_complete_filtered_neutral_chiral.out > cas.smi
```
