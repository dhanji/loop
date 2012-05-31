
if [ "$1" = "" ]; then
  echo Please provide a commit message. e.g.:
  echo ./release.sh \'Version 1.0\'
  echo
  exit
fi

TMP=build_tmp/loop

mvn -o clean package assembly:single
mkdir -p $TMP
cp ./target/loop-*-jar-with-dependencies.jar ./$TMP/loop.jar
cp loop $TMP/
cp loop.bat $TMP/
cp README $TMP/
cp LICENSE $TMP/
pushd build_tmp
tar zcf ../dist/loop.tar.gz loop/*
popd
rm -rf build_tmp

git add dist
git commit -a -m "$1"
git push
