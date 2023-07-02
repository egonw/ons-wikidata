// Download the LMSD.sdf.zip from the LIPID MAPS website and unzip
//
// groovy extractSMILESes.groovy | tee lm.smi

@Grab(group='org.openscience.cdk', module='cdk-bundle', version='2.9-SNAPSHOT')

import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.io.*;
import org.openscience.cdk.io.iterator.*;
import org.openscience.cdk.silent.*;
import org.openscience.cdk.tools.manipulator.*;

iterator = new IteratingSDFReader(
  new File("structures.sdf").newReader(),
  SilentChemObjectBuilder.getInstance()
)
while (iterator.hasNext()) {
  IAtomContainer mol = iterator.next()
  println mol.getProperty("SMILES") + "\t" + mol.getProperty("LM_ID") + "\t" + mol.getProperty("SYSTEMATIC_NAME")
}
