// This uses the lipidmaps_ids_cc0.tsv file (CC0) downloaded from https://lipidmaps.org/databases/lmsd/download

@Grab(group='io.github.egonw.bacting', module='managers-ui', version='0.5.2')
@Grab(group='io.github.egonw.bacting', module='managers-cdk', version='0.5.2')
@Grab(group='io.github.egonw.bacting', module='managers-rdf', version='0.5.2')
@Grab(group='io.github.egonw.bacting', module='managers-inchi', version='0.5.2')

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openscience.cdk.exception.Intractable
import org.openscience.cdk.graph.Cycles
import org.openscience.cdk.interfaces.IAtom
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.stereo.Stereocenters
import org.openscience.cdk.stereo.Stereocenters.Type

workspaceRoot = ".."
bioclipse = new net.bioclipse.managers.BioclipseManager(workspaceRoot);
ui = new net.bioclipse.managers.UIManager(workspaceRoot);
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
rdf = new net.bioclipse.managers.RDFManager(workspaceRoot);
inchi = new net.bioclipse.managers.InChIManager(workspaceRoot);

def potentialStereoCount(IAtomContainer container) {
  try {
    Cycles smallCycles = Cycles.all(6).find(container);
    rings = smallCycles.toRingSet();
  } catch (Intractable exception) {
    return 0
  }
  Set<IAtom> potential = new HashSet<IAtom>();
  Stereocenters centers =  Stereocenters.of(container);
  for (int i = 0; i < container.getAtomCount(); i++)  {
    if (centers.isStereocenter(i)) {
      if (centers.elementType(i) == Type.Tetracoordinate) {
        potential.add(container.getAtom(i));
      } else if (centers.elementType(i) == Type.Tricoordinate) {
        if (!rings.contains(container.getAtom(i))) {
          potential.add(container.getAtom(i));
        }
      }
    }
  }
  return potential.size()
}

input = "/LipidMaps/lipidmaps_ids_cc0.tsv"
new File(bioclipse.fullPath(input)).eachLine{ line ->
  if (!line.contains("obsolete_id")) { // skip the header line
    columns = line.split('\t')
    if (columns.length >= 3) {
      lmid = columns[0]
      inchikey = columns[1]
      smiles = columns[2]

      if (smiles.contains("{-}")) {
        println "# Found a SMILES with {-} for ${lmid}"
      } else {
        // okay, do the cheminformatics magic
        mol = cdk.fromSMILES(smiles)
        if (mol.atomContainer.atomCount == 0) {
          println "# Found an empty SMILES for ${lmid}"
        } else {
          inchiObj = inchi.generate(mol)
          inchiShort = inchiObj.value.substring(6)
          key = inchiObj.key
          if (key != inchikey) println "# InChIKey mismatch for ${lmid}: TSV has ${inchikey} but the SMILES converts to ${key}"

          // check stereochemistry
          potentialCount = potentialStereoCount(mol.atomContainer)
          undefinedCenters = cdk.getAtomsWithUndefinedStereo(mol).size()
          if (potentialCount > 0) {
            fullChiralityIsDefined = (undefinedCenters == 0)
            if (!fullChiralityIsDefined) {
              println "${smiles}\t${lmid} (potential: ${potentialCount}, undefined: ${undefinedCenters})"
            }
          }
        }
      }
    }
  }
}
