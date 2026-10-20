from pathlib import Path
import os, subprocess, shutil, sys

import argparse
parser=argparse.ArgumentParser(description="Compile et teste gestion uniquement sur fixtures isolées.")
parser.add_argument('--jdk',type=Path,required=True,help="Dossier du JDK 25")
parser.add_argument('--junit-dir',type=Path,required=True,help="Dossier des JAR JUnit 6.0.1")
parser.add_argument('--output',type=Path,required=True,help="Dossier de sortie extérieur au projet")
args=parser.parse_args()
project=Path(__file__).resolve().parents[1]
build=args.output.resolve()
if build == project or project in build.parents:
    parser.error("Le dossier de test doit être extérieur au projet et à ses données.")
build.mkdir(parents=True,exist_ok=True)
jdk=args.jdk.resolve()/'bin'
plugins=args.junit_dir.resolve()
testjars=[*plugins.glob('junit-*_6.0.1.jar'),*plugins.glob('org.opentest4j_*.jar'),*plugins.glob('org.apiguardian.api_*.jar')]
production=build/'classes';production.mkdir(exist_ok=True)
tests=build/'tests';tests.mkdir(exist_ok=True)
temporary=build/'tmp';temporary.mkdir(exist_ok=True)
libs=list((project/'lib').glob('*.jar'))

def run(args,name,cwd=build):
    result=subprocess.run([str(a) for a in args],cwd=cwd,capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=90)
    (build/(name+'.txt')).write_text(result.stdout+result.stderr,encoding='utf-8')
    print(name,':',result.returncode)
    print((result.stdout+result.stderr)[-10000:])
    if result.returncode: sys.exit(result.returncode)

run([jdk/'javac.exe','--release','25','-encoding','UTF-8','--module-path',project/'lib','-d',production,*sorted((project/'src').rglob('*.java'))],'compilation-production')
shutil.copytree(project/'src'/'icons',production/'icons',dirs_exist_ok=True)
cp=os.pathsep.join(map(str,[production,*libs,*testjars]))
runner=build/'JunitRunner.java'
runner.write_text('''package gestionTest.runner;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import org.junit.platform.engine.discovery.DiscoverySelectors;
public class JunitRunner {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String name : args) request.selectors(DiscoverySelectors.selectClass(name));
        var listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request.build(), listener);
        var summary = listener.getSummary();
        var writer = new java.io.PrintWriter(System.out, true);
        summary.printTo(writer); summary.printFailuresTo(writer);
        if (summary.getTestsFailedCount() > 0 || summary.getTestsFoundCount() == 0) System.exit(1);
    }
}
''',encoding='utf-8')
run([jdk/'javac.exe','--release','25','-encoding','UTF-8','-cp',cp,'-d',tests,*sorted((project/'test').rglob('*.java')),runner],'compilation-tests')
classes=[]
for path in sorted((project/'test').rglob('*Test.java')):
    package=next(line.split()[1].rstrip(';') for line in path.read_text(encoding='utf-8-sig').splitlines() if line.startswith('package '))
    classes.append(package+'.'+path.stem)
props=['-Djava.awt.headless=true','-Duser.timezone=Europe/Paris','-Djava.io.tmpdir='+str(temporary),'-Djava.util.prefs.PreferencesFactory=gestionTest.support.InMemoryPreferencesFactory',
       '-Dtest.classes='+str(tests),'-Dtest.production='+str(production),'-Dtest.libs='+os.pathsep.join(map(str,libs))]
run([jdk/'java.exe',*props,'-cp',os.pathsep.join([str(tests),cp]),'gestionTest.runner.JunitRunner',*classes],'junit-classpath')
opens=[]
packages=set()
for path in (project/'test').rglob('*.java'):
    packages.add(next(line.split()[1].rstrip(';') for line in path.read_text(encoding='utf-8-sig').splitlines() if line.startswith('package ')))
for package in packages: opens += ['--add-opens','gestionRemplacement/'+package+'=ALL-UNNAMED']
run([jdk/'java.exe',*props,'--module-path',os.pathsep.join(map(str,[production,*libs])),
     '--patch-module','gestionRemplacement='+str(tests),'--add-reads','gestionRemplacement=ALL-UNNAMED',
     '--add-exports','gestionRemplacement/gestionTest.support=java.prefs',
     *opens,'-cp',os.pathsep.join(map(str,testjars)),'-m','gestionRemplacement/gestionTest.runner.JunitRunner',*classes],'junit-module')
