# Open Notebook for Bioclipse scripts for Wikidata

To use these scripts you need to install either
[Bioclipse 2.6.2](https://sourceforge.net/projects/bioclipse/files/bioclipse2/bioclipse2.6.2)
or [Bacting](https://github.com/egonw/bacting).

After that, you can run the scripts with Groovy. If you use Bacting, make sure
to add the following at the top of the script (extend similarly when addition modules are needed):

```groovy
@Grab(group='net.bioclipse.managers', module='bioclipse-cdk', version='0.0.2-SNAPSHOT')

workspaceRoot = System.properties['user.dir']
cdk = new net.bioclipse.managers.CDKManager(workspaceRoot);
```
